package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelEncoder;
import com.sjm.core.util.core.IOUtil;

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
            IOUtil.close(fc);
            file.delete();
        }
    }

    public boolean encode(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        int n = -1;
        switch (ec.state) {
            case STATE_HEADER: {
                if (ec.file == null) {
                    if (ctx.writeQueue.isEmpty())
                        return false;
                    ec.file = (File) ctx.writeQueue.peek();
                    ec.total = ec.file.length();
                    NIOTools.putLong(ec.header, 0, ec.total);
                }
                n = Math.min(ctx.writeBuffer.remaining(), ec.header.length - ec.headerIndex);
                ctx.writeBuffer.put(ec.header, ec.headerIndex, n);
                ec.headerIndex += n;
                if (ec.headerIndex == ec.header.length) {
                    ec.fc = FileChannel.open(ec.file.toPath(), StandardOpenOption.READ);
                    ec.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                n = ec.fc.read(ctx.writeBuffer);
                if (n == -1)
                    throw new IOException();
                ec.position += n;
                if (ec.position == ec.total) {
                    finish(ctx, ec);
                } else if (ec.position > ec.total)
                    throw new IOException();
                break;
            }
        }
        return n != 0;
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
