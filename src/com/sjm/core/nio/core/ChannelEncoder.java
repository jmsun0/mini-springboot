package com.sjm.core.nio.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class ChannelEncoder implements ChannelHandler {

    protected abstract void encode(ChannelContext ctx) throws IOException;

    @Override
    public int handle(ChannelContext ctx) throws IOException {
        while (true) {
            if (ctx.writeBuffer.hasRemaining()) {
                int n = ctx.channel.write(ctx.writeBuffer);
                if (n <= 0) {
                    return n;
                }
            } else {
                int n = beforeWrite(ctx);
                if (n <= 0) {
                    return n;
                }
                if (ctx.writeQueue.isEmpty()) {
                    ctx.key.interestOps(ctx.key.interestOps() & ~SelectionKey.OP_WRITE);
                    return 0;
                }
                ctx.writeBuffer.clear();
                while (true) {
                    encode(ctx);
                    if (!ctx.writeBuffer.hasRemaining() || ctx.writeQueue.isEmpty()) {
                        break;
                    }
                }
                ctx.writeBuffer.flip();
            }
        }
    }

    protected int beforeWrite(ChannelContext ctx) throws IOException {
        return 1;
    }
}
