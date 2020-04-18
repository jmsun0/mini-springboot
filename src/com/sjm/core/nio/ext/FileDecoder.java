package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelDecoder;
import com.sjm.core.nio.core.NIOTools;
import com.sjm.core.util.Misc;

/**
 * 报文类型： File（实现零拷贝）
 * 
 * 报文结构： [总长度，包括自己(固定8字节)] [body]
 * 
 * @author root
 *
 */
public class FileDecoder extends ChannelDecoder {

    static final int STATE_HEADER = 0;
    static final int STATE_BODY = 1;

    static class DecodeContext implements Closeable {
        public int state;
        public byte[] header = new byte[8];
        public int headerIndex;
        public long total;
        public long position;
        public File file;
        public FileChannel fc;

        @Override
        public void close() throws IOException {
            Misc.close(fc);
            file.delete();
        }
    }

    @Override
    protected void decode(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        switch (dc.state) {
            case STATE_HEADER: {
                int len = Math.min(ctx.readBuffer.remaining(), dc.header.length - dc.headerIndex);
                ctx.readBuffer.get(dc.header, dc.headerIndex, len);
                dc.headerIndex += len;
                if (dc.headerIndex == dc.header.length) {
                    dc.total = NIOTools.getLong(dc.header);
                    dc.file = Files.createTempFile(null, null).toFile();
                    dc.fc = FileChannel.open(dc.file.toPath(), StandardOpenOption.WRITE);
                    dc.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                int oldLimit = ctx.readBuffer.limit();
                int len = (int) Math.min(ctx.readBuffer.remaining(), dc.total);
                ctx.readBuffer.limit(ctx.readBuffer.position() + len);
                int n = dc.fc.write(ctx.readBuffer);
                if (n != len)
                    throw new IOException();
                dc.position += len;
                ctx.readBuffer.limit(oldLimit);
                if (dc.position == dc.total) {
                    finish(ctx, dc);
                }
                break;
            }
        }
    }

    @Override
    protected int beforeRead(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        if (dc.state == STATE_BODY) {
            while (true) {
                long n = dc.fc.transferFrom(ctx.channel, dc.position, dc.total - dc.position);
                if (n <= 0) {
                    return (int) n;
                }
                dc.position += n;
                if (dc.position == dc.total) {
                    finish(ctx, dc);
                    break;
                }
            }
        }
        return 1;
    }

    private void finish(ChannelContext ctx, DecodeContext dc) throws IOException {
        dc.fc.close();
        ctx.processer.process(ctx, dc.file);
        dc.headerIndex = 0;
        dc.position = 0;
        dc.fc = null;
        dc.file = null;
        dc.state = STATE_HEADER;
    }

    private DecodeContext getDecodeContext(ChannelContext ctx) {
        DecodeContext dc = (DecodeContext) ctx.decodeCotext;
        if (dc == null)
            ctx.decodeCotext = dc = new DecodeContext();
        return dc;
    }
}
