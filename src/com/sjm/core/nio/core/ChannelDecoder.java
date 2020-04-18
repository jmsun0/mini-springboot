package com.sjm.core.nio.core;

import java.io.IOException;

public abstract class ChannelDecoder implements ChannelHandler {

    protected abstract void decode(ChannelContext ctx) throws IOException;

    @Override
    public int handle(ChannelContext ctx) throws IOException {
        while (true) {
            int n = beforeRead(ctx);
            if (n <= 0) {
                return n;
            }
            n = ctx.channel.read(ctx.readBuffer);
            if (n <= 0) {
                return n;
            }
            ctx.readBuffer.flip();
            while (true) {
                decode(ctx);
                if (!ctx.readBuffer.hasRemaining()) {
                    break;
                }
            }
            ctx.readBuffer.clear();
        }
    }

    protected int beforeRead(ChannelContext ctx) throws IOException {
        return 1;
    }
}
