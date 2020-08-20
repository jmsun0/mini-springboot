package com.sjm.core.nio.ext;

import java.io.IOException;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelEncoder;


/**
 * 报文类型： byte[]
 * 
 * 报文结构： [总长度，包括自己(固定4字节)] [body]
 * 
 * @author root
 *
 */
public class ByteArrayEncoder extends ChannelEncoder {

    static final int STATE_HEADER = 0;
    static final int STATE_BODY = 1;

    static class EncodeContext {
        public int state;
        public byte[] header = new byte[4];
        public int headerIndex;
        public byte[] data;
        public int dataIndex;
    }

    @Override
    protected boolean encode(ChannelContext ctx) throws IOException {
        EncodeContext ec = (EncodeContext) ctx.encodeCotext;
        if (ec == null)
            ctx.encodeCotext = ec = new EncodeContext();
        int n = -1;
        switch (ec.state) {
            case STATE_HEADER: {
                if (ec.headerIndex == 0) {
                    if (ctx.writeQueue.isEmpty())
                        return false;
                    ec.data = (byte[]) ctx.writeQueue.peek();
                    NIOTools.putInt(ec.header, 0, ec.data.length);
                }
                n = Math.min(ctx.writeBuffer.remaining(), ec.header.length - ec.headerIndex);
                ctx.writeBuffer.put(ec.header, ec.headerIndex, n);
                ec.headerIndex += n;
                if (ec.headerIndex == ec.header.length) {
                    ec.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                n = Math.min(ctx.writeBuffer.remaining(), ec.data.length - ec.dataIndex);
                ctx.writeBuffer.put(ec.data, ec.dataIndex, n);
                ec.dataIndex += n;
                if (ec.dataIndex == ec.data.length) {
                    ctx.writeQueue.poll();
                    ec.headerIndex = 0;
                    ec.dataIndex = 0;
                    ec.state = STATE_HEADER;
                }
                break;
            }
        }
        return n != 0;
    }
}
