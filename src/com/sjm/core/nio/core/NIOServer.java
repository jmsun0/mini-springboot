package com.sjm.core.nio.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NIOServer extends NIOBase {
    protected int port;

    public NIOServer(int port, ByteBufferPool bufferPool, ChannelHandler encoder,
            ChannelHandler decoder, EventHandler handler) {
        super(bufferPool, encoder, decoder, handler);
        this.port = port;
    }

    @Override
    protected Closeable openChannel() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(port));
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        return ssc;
    }

    @Override
    protected SocketChannel processConnect(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel sc = server.accept();
            sc.configureBlocking(false);
            return sc;
        }
        return null;
    }

    @Override
    protected boolean shutdownOnClose() {
        return false;
    }
}
