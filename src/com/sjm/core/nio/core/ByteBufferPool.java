package com.sjm.core.nio.core;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ByteBufferPool {
    private int bufferSize;
    private Queue<ByteBuffer> buffers;

    public ByteBufferPool(int bufferSize) {
        this.bufferSize = bufferSize;
        this.buffers = new ConcurrentLinkedQueue<>();
    }

    public ByteBuffer apply() {
        ByteBuffer buffer = buffers.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(bufferSize);
        return buffer;
    }

    public void recycle(ByteBuffer buffer) {
        buffer.clear();
        buffers.offer(buffer);
    }
}
