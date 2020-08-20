package com.sjm.core.nio.ext;

import java.io.IOException;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelDecoder;

/**
 * 报文类型： byte[]
 * 
 * 报文结构： [总长度，包括自己(固定4字节)] [body]
 * 
 * @author root
 *
 */
public class ByteArrayDecoder extends ChannelDecoder {

    static final int STATE_HEADER = 0;
    static final int STATE_BODY = 1;

    static class DecodeContext {
        public int state;
        public byte[] header = new byte[4];
        public int headerIndex;
        public byte[] data;
        public int dataIndex;
    }

    @Override
    protected boolean decode(ChannelContext ctx) throws IOException {
        DecodeContext dc = (DecodeContext) ctx.decodeCotext;
        if (dc == null)
            ctx.decodeCotext = dc = new DecodeContext();
        int n = -1;
        switch (dc.state) {
            case STATE_HEADER: {
                n = Math.min(ctx.readBuffer.remaining(), dc.header.length - dc.headerIndex);
                ctx.readBuffer.get(dc.header, dc.headerIndex, n);
                dc.headerIndex += n;
                if (dc.headerIndex == dc.header.length) {
                    dc.data = new byte[NIOTools.getInt(dc.header, 0)];
                    dc.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                n = Math.min(ctx.readBuffer.remaining(), dc.data.length - dc.dataIndex);
                ctx.readBuffer.get(dc.data, dc.dataIndex, n);
                dc.dataIndex += n;
                if (dc.dataIndex == dc.data.length) {
                    ctx.processer.process(ctx, dc.data);
                    dc.headerIndex = 0;
                    dc.dataIndex = 0;
                    dc.data = null;
                    dc.state = STATE_HEADER;
                }
                break;
            }
        }
        return n != 0;
    }
}
