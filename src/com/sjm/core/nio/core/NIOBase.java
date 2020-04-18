package com.sjm.core.nio.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;
import com.sjm.core.util.Misc;

public abstract class NIOBase implements PacketProcesser, Runnable {
    static final Logger logger = LoggerFactory.getLogger(NIOBase.class);

    protected Closeable channel;
    protected ByteBufferPool bufferPool;
    protected ChannelHandler encoder, decoder;
    protected EventHandler handler;
    protected boolean isOpen = true;
    protected Selector selector;

    public NIOBase(ByteBufferPool bufferPool, ChannelHandler encoder, ChannelHandler decoder,
            EventHandler handler) {
        this.bufferPool = bufferPool;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
    }

    protected abstract Closeable openChannel() throws IOException;

    protected abstract SocketChannel processConnect(SelectionKey key) throws IOException;

    protected abstract boolean shutdownOnClose();

    @Override
    public void run() {
        try {
            isOpen = true;
            selector = Selector.open();
            channel = openChannel();
            handler.onStartup(this);
            while (isOpen) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    ChannelContext ctx = null;
                    int n = 0;
                    try {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        SocketChannel sc = processConnect(key);
                        if (sc != null) {
                            SelectionKey k = sc.register(selector, SelectionKey.OP_READ);
                            ctx = new ChannelContext(sc, selector, k, encoder, decoder, bufferPool,
                                    this);
                            k.attach(ctx);
                            handler.onConnect(ctx);
                            if (!ctx.channel.isOpen()) {
                                n = -1;
                            }
                        }
                        if (n != -1 && (key.isReadable() || key.isWritable())) {
                            ctx = (ChannelContext) key.attachment();
                            if (key.isWritable())
                                n = ctx.encoder.handle(ctx);
                            if (key.isReadable())
                                n = ctx.decoder.handle(ctx);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        n = -1;
                    }
                    if (n == -1) {
                        try {
                            handler.onClose(ctx);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        Misc.close(ctx);
                        if (shutdownOnClose())
                            shutdown();
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        shutdown();
    }

    @Override
    public void process(ChannelContext ctx, Object packet) {
        try {
            handler.onRead(ctx, packet);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown() {
        isOpen = false;
        Misc.close(selector);
        Misc.close(channel);
    }
}
