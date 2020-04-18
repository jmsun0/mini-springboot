package com.sjm.core.nio.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class NIOClient extends NIOBase {
    private String host;
    private int port;

    public NIOClient(String host, int port, ByteBufferPool bufferPool, ChannelHandler encoder,
            ChannelHandler decoder, EventHandler handler) {
        super(bufferPool, encoder, decoder, handler);
        this.host = host;
        this.port = port;
    }

    @Override
    protected Closeable openChannel() throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress(host, port));
        sc.register(selector, SelectionKey.OP_CONNECT);
        return sc;
    }

    @Override
    protected SocketChannel processConnect(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
            SocketChannel sc = (SocketChannel) key.channel();
            if (sc.isConnectionPending()) {
                sc.finishConnect();
            }
            sc.configureBlocking(false);
            return sc;
        }
        return null;
    }

    @Override
    protected boolean shutdownOnClose() {
        return true;
    }
}
