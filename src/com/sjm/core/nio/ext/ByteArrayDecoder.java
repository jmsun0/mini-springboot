package com.sjm.core.nio.ext;

import java.io.IOException;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelDecoder;
import com.sjm.core.nio.core.NIOTools;

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
    protected void decode(ChannelContext ctx) throws IOException {
        DecodeContext dc = (DecodeContext) ctx.decodeCotext;
        if (dc == null)
            ctx.decodeCotext = dc = new DecodeContext();
        switch (dc.state) {
            case STATE_HEADER: {
                int len = Math.min(ctx.readBuffer.remaining(), dc.header.length - dc.headerIndex);
                ctx.readBuffer.get(dc.header, dc.headerIndex, len);
                dc.headerIndex += len;
                if (dc.headerIndex == dc.header.length) {
                    dc.data = new byte[NIOTools.getInt(dc.header)];
                    dc.state = STATE_BODY;
                }
                break;
            }
            case STATE_BODY: {
                int len = Math.min(ctx.readBuffer.remaining(), dc.data.length - dc.dataIndex);
                ctx.readBuffer.get(dc.data, dc.dataIndex, len);
                dc.dataIndex += len;
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
    }
}
