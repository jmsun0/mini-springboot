package com.sjm.core.nio.core;

public interface PacketProcesser {
    public void process(ChannelContext ctx, Object packet);
}
