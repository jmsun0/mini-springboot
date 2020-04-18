package com.sjm.core.nio.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sjm.core.util.Misc;

public class ChannelContext implements Closeable {
    public SocketChannel channel;
    public Selector selector;
    public SelectionKey key;
    public ByteBufferPool bufferPool;
    public Map<String, Object> attributes = new HashMap<>();

    public ChannelHandler decoder;// 读处理器
    public PacketProcesser processer;// 消息输出器
    public ByteBuffer readBuffer;// 读取缓存
    public Object decodeCotext;// 解码上下文

    public ChannelHandler encoder;// 写处理器
    public Queue<Object> writeQueue;// 消息输入队列
    public ByteBuffer writeBuffer;// 写入缓存
    public Object encodeCotext;// 编码上下文

    public ChannelContext(SocketChannel channel, Selector selector, SelectionKey key,
            ChannelHandler encoder, ChannelHandler decoder, ByteBufferPool bufferPool,
            PacketProcesser processer) {
        this.channel = channel;
        this.selector = selector;
        this.key = key;
        this.encoder = encoder;
        this.decoder = decoder;
        this.bufferPool = bufferPool;
        this.processer = processer;
        this.readBuffer = bufferPool.apply();
        this.writeBuffer = bufferPool.apply();
        this.writeQueue = new ConcurrentLinkedQueue<>();
        writeBuffer.position(writeBuffer.limit());
    }

    public void write(Object obj) throws IOException {
        writeQueue.offer(obj);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    @Override
    public void close() throws IOException {
        Misc.close(channel);
        bufferPool.recycle(readBuffer);
        bufferPool.recycle(writeBuffer);
        if (decodeCotext instanceof Closeable) {
            Misc.close((Closeable) decodeCotext);
        }
        if (decodeCotext instanceof Closeable) {
            Misc.close((Closeable) decodeCotext);
        }
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "["
                    + (channel.isOpen()
                            ? ("local=" + channel.getLocalAddress() + " remote="
                                    + channel.getRemoteAddress())
                            : "closed")
                    + "]";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
