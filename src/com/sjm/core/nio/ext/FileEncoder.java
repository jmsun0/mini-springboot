package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelEncoder;
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
public class FileEncoder extends ChannelEncoder {

    static final int STATE_HEADER = 0;
    static final int STATE_BODY = 1;

    static class EncodeContext implements Closeable {
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

    public void encode(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        switch (ec.state) {
            case STATE_HEADER: {
                if (ec.file == null) {
                    ec.file = (File) ctx.writeQueue.peek();
                    ec.total = ec.file.length();
                    NIOTools.putLong(ec.header, ec.total);
                }
                int n = Math.min(ctx.writeBuffer.remaining(), ec.header.length - ec.headerIndex);
                ctx.writeBuffer.put(ec.header, ec.headerIndex, n);
                ec.headerIndex += n;
                if (ec.headerIndex == ec.header.length) {
                    ec.fc = FileChannel.open(ec.file.toPath(), StandardOpenOption.READ);
                    ec.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                int len = ec.fc.read(ctx.writeBuffer);
                if (len == -1)
                    throw new IOException();
                ec.position += len;
                if (ec.position == ec.total) {
                    finish(ctx, ec);
                } else if (ec.position > ec.total)
                    throw new IOException();
                break;
            }
        }
    }

    protected int beforeWrite(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        if (ec.state == STATE_BODY) {
            while (true) {
                long n = ec.fc.transferTo(ec.position, ec.total - ec.position, ctx.channel);
                if (n <= 0) {
                    return (int) n;
                }
                ec.position += n;
                if (ec.position == ec.total) {
                    finish(ctx, ec);
                    break;
                } else if (ec.position > ec.total)
                    throw new IOException();
            }
        }
        return 1;
    }

    private void finish(ChannelContext ctx, EncodeContext ec) throws IOException {
        ctx.writeQueue.poll();
        ec.fc.close();
        ec.headerIndex = 0;
        ec.position = 0;
        ec.fc = null;
        ec.file = null;
        ec.state = STATE_HEADER;
    }

    private EncodeContext getEncodeContext(ChannelContext ctx) {
        EncodeContext ec = (EncodeContext) ctx.encodeCotext;
        if (ec == null)
            ctx.encodeCotext = ec = new EncodeContext();
        return ec;
    }
}
