package com.sjm.core.util;

import java.io.IOException;
import java.io.InterruptedIOException;

public class ObjectPipe<T> {
    public static void main(String[] args) throws Exception {
        final ObjectPipe<Integer> pipe = new ObjectPipe<Integer>(20);

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    for (int i = 0; i < 30; i++)
                        pipe.write(i, -1);
                    Thread.sleep(4000);
                    for (int i = 0; i < 30; i++)
                        pipe.write(i, -1);
                    pipe.closeWrite();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        while (true) {
            Integer r = pipe.read(-1);
            if (r == null)
                break;
            System.out.println(r);
        }
        pipe.closeRead();
    }

    private T[] buffer;
    private int head, tail;
    private boolean write;
    private boolean closeRead;
    private boolean closeWrite;

    @SuppressWarnings("unchecked")
    public ObjectPipe(int cap) {
        buffer = (T[]) new Object[cap];
    }

    public int size() {
        int size = tail - head;
        return size > 0 ? size : size == 0 ? write ? buffer.length : 0 : size + buffer.length;
    }

    public int capacity() {
        return buffer.length;
    }

    public synchronized void write(T b, long timeout) throws IOException {
        waitForWrite(timeout);
        boolean empty = is(EMPTY);
        buffer[tail] = b;
        tail = (tail + 1) % buffer.length;
        write = true;
        if (empty)
            notifyAll();
    }

    public synchronized void write(T[] b, int off, int len, long timeout) throws IOException {
        while (len > 0) {
            waitForWrite(timeout);
            boolean empty = is(EMPTY);
            int start = tail, size = Math.min(len,
                    head <= tail ? head == 0 ? buffer.length - tail : tail - head + buffer.length
                            : head - tail);
            if (start + size <= buffer.length) {
                System.arraycopy(b, off, buffer, start, size);
            } else {
                int n = buffer.length - start;
                System.arraycopy(b, off, buffer, start, n);
                System.arraycopy(b, off + n, buffer, 0, size - n);
            }
            tail = (start + size) % buffer.length;
            write = true;
            off += size;
            len -= size;
            if (empty)
                notifyAll();
        }
    }

    public synchronized T read(long timeout) throws IOException {
        if (waitForRead(timeout))
            return null;
        boolean full = is(FULL);
        T b = buffer[head];
        head = (head + 1) % buffer.length;
        write = false;
        if (full)
            notifyAll();
        return b;
    }

    public synchronized int read(T[] b, int off, int len, long timeout) throws IOException {
        if (waitForRead(timeout))
            return -1;
        boolean full = is(FULL);
        int start = head,
                size = Math.min(len, head >= tail ? tail - head + buffer.length : tail - head);
        if (start + size <= buffer.length) {
            System.arraycopy(buffer, start, b, off, size);
        } else {
            int n = buffer.length - start;
            System.arraycopy(buffer, start, b, off, n);
            System.arraycopy(buffer, 0, b, off + n, size - n);
        }
        head = (start + size) % buffer.length;
        write = false;
        if (full)
            notifyAll();
        return size;
    }

    public synchronized long skip(long n, long timeout) throws IOException {
        long remain = n;
        while (!waitForRead(timeout) && remain > 0) {
            boolean full = is(FULL);
            int size = Math.min(remain <= Integer.MAX_VALUE ? (int) remain : Integer.MAX_VALUE,
                    head >= tail ? tail - head + buffer.length : tail - head);
            head = (head + size) % buffer.length;
            write = false;
            remain -= size;
            if (full)
                notifyAll();
        }
        return n - remain;
    }

    public synchronized void flush() {
        notifyAll();
    }

    public synchronized void closeRead() {
        closeRead = true;
        notifyAll();
    }

    public synchronized void closeWrite() {
        closeWrite = true;
        notifyAll();
    }

    private static final boolean EMPTY = true;
    private static final boolean FULL = false;

    private boolean is(boolean state) {
        return tail == head && (state ^ write);
    }

    private void checkClose(boolean con) throws IOException {
        if (con)
            throw new IOException("close");
    }

    private void waitForWrite(long timeout) throws IOException {
        checkClose(closeRead || closeWrite);
        while (is(FULL)) {
            timeout = waitThrowIO(timeout);
            checkClose(closeRead || closeWrite);
        }
    }

    private boolean waitForRead(long timeout) throws IOException {
        checkClose(closeRead);
        while (is(EMPTY)) {
            if (closeWrite)
                return true;
            timeout = waitThrowIO(timeout);
            checkClose(closeRead);
        }
        return false;
    }

    private long waitThrowIO(long timeout) throws IOException {
        try {
            if (timeout < 0) {
                wait(1000);
                return timeout;
            } else if (timeout > 0) {
                long wait = Math.min(1000, timeout);
                wait(wait);
                return timeout - wait;
            } else {
                throw new IOException("timeout");
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }
}
