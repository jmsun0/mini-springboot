package com.sjm.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.concurrent.Executor;

public class IOUtil {
    public static int DEFAULT_BUFFER_SIZE = 4096;

    public static final char DIR_SEPARATOR = File.separatorChar;

    public static final String LINE_SEPARATOR;
    static {
        StringWriter sw = new StringWriter(4);
        PrintWriter pw = new PrintWriter(sw);
        pw.println();
        LINE_SEPARATOR = sw.toString();
    }
    public static final byte[] LINE_SEPARATOR_BYTES = LINE_SEPARATOR.getBytes();

    public static final InputStream EMPTY_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public int available() throws IOException {
            return 0;
        }

        @Override
        public long skip(long n) throws IOException {
            return 0;
        }
    };
    public static final Reader EMPTY_READER = new Reader() {
        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public int read(CharBuffer target) throws IOException {
            return -1;
        }

        @Override
        public boolean ready() throws IOException {
            return true;
        }

        @Override
        public void close() throws IOException {}

        @Override
        public long skip(long n) throws IOException {
            return 0;
        }
    };

    public static void copy(InputStream in, OutputStream out, byte[] buffer, boolean flush)
            throws IOException {
        if (buffer == null)
            buffer = new byte[DEFAULT_BUFFER_SIZE];
        if (flush)
            for (int len; (len = in.read(buffer)) != -1;) {
                out.write(buffer, 0, len);
                out.flush();
            }
        else
            for (int len; (len = in.read(buffer)) != -1;)
                out.write(buffer, 0, len);
    }

    public static void copy(Reader in, Writer out, char[] buffer, boolean flush)
            throws IOException {
        if (buffer == null)
            buffer = new char[DEFAULT_BUFFER_SIZE];
        if (flush)
            for (int len; (len = in.read(buffer)) != -1;) {
                out.write(buffer, 0, len);
                out.flush();
            }
        else
            for (int len; (len = in.read(buffer)) != -1;)
                out.write(buffer, 0, len);
    }

    public static int readLine(InputStream is, byte[] buffer) throws IOException {
        int i = 0;
        for (int c; (c = is.read()) != -1;) {
            if (c == '\n') {
                if (i > 0 && buffer[i - 1] == '\r')
                    i--;
                return i;
            }
            buffer[i++] = (byte) c;
        }
        if (i == 0)
            return -1;
        return i;
    }

    public static String readLine(InputStream is, byte[] buffer, Charset charset)
            throws IOException {
        int len = readLine(is, buffer);
        return len == -1 ? null : len == 0 ? "" : new String(buffer, 0, len, charset);
    }

    public static int readLine(InputStream is, ByteVector vec) throws IOException {
        int i = 0;
        for (int c; (c = is.read()) != -1; i++) {
            if (c == '\n') {
                if (i > 0 && vec.get(i - 1) == '\r') {
                    i--;
                    vec.setSize(i);
                }
                return i;
            }
            vec.put((byte) c);
        }
        if (i == 0)
            return -1;
        return i;
    }

    public static Charset getCharset(String charset) {
        return charset == null ? Charset.defaultCharset() : Charset.forName(charset);
    }

    public static Charset getCharsetThrow(String charset) throws UnsupportedEncodingException {
        try {
            return getCharset(charset);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(charset);
        }
    }

    public static boolean compareCharset(String charset1, String charset2) {
        return charset1 == charset2 || charset1 != null && charset1.equals(charset2)
                || getCharset(charset1).equals(getCharset(charset2));
    }

    public static byte[] encodeCharset(char[] chars, Charset charset) {
        ByteBuffer bb = charset.encode(CharBuffer.wrap(chars));
        return Arrays.copyOfRange(bb.array(), 0, bb.limit());
    }

    public static char[] decodeCharset(byte[] bytes, Charset charset) {
        CharBuffer cb = charset.decode(ByteBuffer.wrap(bytes));
        return Arrays.copyOfRange(cb.array(), 0, cb.limit());
    }

    public static Reader toReader(InputStream is, String charset) {
        return new InputStreamReader(is, IOUtil.getCharset(charset));
    }

    public static Writer toWriter(OutputStream os, String charset) {
        return new OutputStreamWriter(os, IOUtil.getCharset(charset));
    }

    public static InputStream toInputStream(Reader reader, String charset) {
        return new ReaderInputStream(reader, IOUtil.getCharset(charset).newEncoder());
    }

    public static OutputStream toOutputStream(Writer writer, String charset) {
        return new WriterOutputStream(writer, IOUtil.getCharset(charset).newDecoder());
    }

    public static InputStream convert(InputStream is, String fromCharset, String toCharset) {
        if (compareCharset(fromCharset, toCharset))
            return is;
        return toInputStream(toReader(is, fromCharset), toCharset);
    }

    public static OutputStream convert(OutputStream os, String fromCharset, String toCharset) {
        if (compareCharset(fromCharset, toCharset))
            return os;
        return toOutputStream(toWriter(os, fromCharset), toCharset);
    }

    public static InputStream buffer(InputStream is) {
        return is instanceof BufferedInputStream ? is : new BufferedInputStream(is);
    }

    public static OutputStream buffer(OutputStream os) {
        return os instanceof BufferedOutputStream ? os : new BufferedOutputStream(os);
    }

    public static BufferedReader buffer(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader
                : new BufferedReader(reader);
    }

    public static BufferedWriter buffer(Writer writer) {
        return writer instanceof BufferedWriter ? (BufferedWriter) writer
                : new BufferedWriter(writer);
    }

    public interface WriteTo {
        public void write(OutputStream out) throws Exception;
    }

    public static InputStream convert(final WriteTo wt, int buffer, Executor ex,
            final Closeable c) {
        if (buffer == -1)
            buffer = DEFAULT_BUFFER_SIZE;
        if (ex == null)
            ex = Misc.DEAULT_EXECUTOR;
        final Pipe pipe = new Pipe(buffer) {
            @Override
            public void close() {
                super.close();
                Misc.close(c);
            }
        };
        ex.execute(new Runnable() {
            @Override
            public void run() {
                try (OutputStream out = pipe.getOutputStream()) {
                    wt.write(out);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return pipe;
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    static class ReaderInputStream extends InputStream {
        protected Reader reader;
        protected CharsetEncoder ce;
        protected ByteBuffer bbuf;
        protected CharBuffer cbuf;
        protected boolean eof;

        public ReaderInputStream(Reader reader, CharsetEncoder ce) {
            this.reader = reader;
            this.ce = ce;
            cbuf = CharBuffer.allocate(5);
            bbuf = ByteBuffer.allocate((int) (cbuf.capacity() * ce.maxBytesPerChar()));
            cbuf.limit(0);
            bbuf.limit(0);
        }

        @Override
        public int read() throws IOException {
            return eof || !(bbuf.hasRemaining() || fill()) ? -1 : bbuf.get();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (eof)
                return -1;
            int size = 0;
            while (size < len) {
                int remain = bbuf.remaining();
                if (remain == 0) {
                    if (!fill())
                        break;
                    remain = bbuf.remaining();
                }
                int readLen = Math.min(remain, len - size);
                bbuf.get(b, off, readLen);
                size += readLen;
                off += readLen;
            }
            return size;
        }

        @Override
        public int available() throws IOException {
            return bbuf.remaining();
        }

        @Override
        public void reset() throws IOException {
            reader.reset();
            eof = false;
            cbuf.position(0);
            bbuf.position(0);
            cbuf.limit(0);
            bbuf.limit(0);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        protected boolean fill() throws IOException {
            cbuf.clear();
            bbuf.clear();
            int len = reader.read(cbuf);
            if (len == -1) {
                eof = true;
                return false;
            }
            cbuf.flip();
            ce.reset();
            CoderResult cr = ce.encode(cbuf, bbuf, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bbuf);
            if (!cr.isUnderflow())
                cr.throwException();
            bbuf.flip();
            return true;
        }
    }
    static class WriterOutputStream extends OutputStream {
        protected Writer writer;
        protected CharsetDecoder cd;
        protected ByteBuffer bbuf;
        protected CharBuffer cbuf;
        protected boolean eof;

        public WriterOutputStream(Writer writer, CharsetDecoder cd) {
            this.writer = writer;
            this.cd = cd;
            bbuf = ByteBuffer.allocate(4096);
            cbuf = CharBuffer.allocate((int) (bbuf.capacity() * cd.maxCharsPerByte()));
            bbuf.clear();
            cbuf.clear();
        }

        @Override
        public void write(int b) throws IOException {
            if (!bbuf.hasRemaining())
                flush0();
            bbuf.put((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            while (len != 0) {
                int remain = bbuf.remaining();
                if (remain == 0) {
                    flush0();
                    remain = bbuf.remaining();
                }
                int n = Math.min(len, remain);
                bbuf.put(b, off, n);
                off += n;
                len -= n;
            }
        }

        @Override
        public void flush() throws IOException {
            flush0();
            if (bbuf.position() != 0)
                throw new IOException("刷新失败，不是完整编码");
            writer.flush();
        }

        public void flush0() throws IOException {
            bbuf.flip();
            cd.reset();
            CoderResult cr = cd.decode(bbuf, cbuf, false);
            if (!cr.isUnderflow())
                cr.throwException();
            cbuf.flip();

            writer.write(cbuf.array(), cbuf.position(), cbuf.limit());

            int position = 0;
            for (int i = bbuf.position(), len = bbuf.limit(); i < len; i++, position++)
                bbuf.put(position, bbuf.get(i));

            bbuf.clear();
            cbuf.clear();

            bbuf.position(position);
        }

        @Override
        public void close() throws IOException {
            try {
                flush();
            } finally {
                writer.close();
            }
        }
    }

    public static class ByteVector {
        private byte[] data;
        private int size;

        public ByteVector(int cap) {
            data = new byte[cap];
        }

        public ByteVector() {
            this(DEFAULT_BUFFER_SIZE);
        }

        @Override
        public String toString() {
            return toString(Charset.defaultCharset());
        }

        public String toString(Charset charset) {
            return new String(data, 0, size, charset);
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            size = 0;
        }

        public byte[] getLocalBytes() {
            return data;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
            System.arraycopy(data, srcBegin, dst, dstBegin, srcEnd - srcBegin);
        }

        public byte[] toByteArray() {
            return Arrays.copyOfRange(data, 0, size);
        }

        public byte get(int index) {
            return data[index];
        }

        public void put(byte b) {
            resize(1);
            data[size++] = b;
        }

        public void put(byte[] v, int off, int len) {
            resize(len);
            System.arraycopy(v, off, data, size, len);
            size += len;
        }

        public void put(byte[] v) {
            put(v, 0, v.length);
        }

        private void resize(int addSize) {
            int newSize = size + addSize;
            if (newSize > data.length) {
                if (newSize < data.length * 2)
                    newSize = data.length * 2;
                byte[] newBuf = new byte[newSize];
                System.arraycopy(data, 0, newBuf, 0, data.length);
                data = newBuf;
            }
        }
    }

    // 字节阻塞队列
    public static class Pipe extends InputStream {
        private byte[] arr;
        private int head, tail;
        private boolean write;
        private boolean closeRead;
        private boolean closeWrite;

        public Pipe(int cap) {
            arr = new byte[cap];
        }

        public Pipe() {
            this(IOUtil.DEFAULT_BUFFER_SIZE);
        }

        public synchronized void closeRead() {
            closeRead = true;
            notifyAll();
        }

        public synchronized void closeWrite() {
            closeWrite = true;
            notifyAll();
        }

        public int size() {
            int size = tail - head;
            return size > 0 ? size : size == 0 ? write ? arr.length : 0 : size + arr.length;
        }

        public int capacity() {
            return arr.length;
        }

        static final boolean EMPTY = true;
        static final boolean FULL = false;

        boolean is(boolean state) {
            return tail == head && (state ^ write);
        }

        void checkClose(boolean con) throws IOException {
            if (con)
                throw new IOException("close");
        }

        void waitForWrite() throws IOException {
            try {
                checkClose(closeRead || closeWrite);
                while (is(FULL)) {
                    wait(1000);
                    checkClose(closeRead || closeWrite);
                }
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }

        boolean waitForRead() throws IOException {
            try {
                checkClose(closeRead);
                while (is(EMPTY)) {
                    if (closeWrite)
                        return true;
                    wait(1000);
                    checkClose(closeRead);
                }
                return false;
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }

        public synchronized void write(int b) throws IOException {
            waitForWrite();
            boolean empty = is(EMPTY);
            arr[tail] = (byte) (b & 0xFF);
            tail = (tail + 1) % arr.length;
            write = true;
            if (empty)
                notifyAll();
        }

        public synchronized void write(byte[] b, int off, int len) throws IOException {
            while (len > 0) {
                waitForWrite();
                boolean empty = is(EMPTY);
                int start = tail, size = Math.min(len,
                        head <= tail ? head == 0 ? arr.length - tail : tail - head + arr.length
                                : head - tail);
                if (start + size <= arr.length) {
                    System.arraycopy(b, off, arr, start, size);
                } else {
                    int n = arr.length - start;
                    System.arraycopy(b, off, arr, start, n);
                    System.arraycopy(b, off + n, arr, 0, size - n);
                }
                tail = (start + size) % arr.length;
                write = true;
                off += size;
                len -= size;
                if (empty)
                    notifyAll();
            }
        }

        public synchronized int read() throws IOException {
            if (waitForRead())
                return -1;
            boolean full = is(FULL);
            int b = arr[head];
            head = (head + 1) % arr.length;
            write = false;
            if (full)
                notifyAll();
            return b;
        }

        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (waitForRead())
                return -1;
            boolean full = is(FULL);
            int start = head,
                    size = Math.min(len, head >= tail ? tail - head + arr.length : tail - head);
            if (start + size <= arr.length) {
                System.arraycopy(arr, start, b, off, size);
            } else {
                int n = arr.length - start;
                System.arraycopy(arr, start, b, off, n);
                System.arraycopy(arr, 0, b, off + n, size - n);
            }
            head = (start + size) % arr.length;
            write = false;
            if (full)
                notifyAll();
            return size;
        }

        public void close() {
            closeRead();
        }

        public int available() {
            return size();
        }

        public synchronized long skip(long n) throws IOException {
            long remain = n;
            while (!waitForRead() && remain > 0) {
                boolean full = is(FULL);
                int size = Math.min(remain <= Integer.MAX_VALUE ? (int) remain : Integer.MAX_VALUE,
                        head >= tail ? tail - head + arr.length : tail - head);
                head = (head + size) % arr.length;
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

        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    Pipe.this.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    Pipe.this.write(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    Pipe.this.closeWrite();
                }

                @Override
                public void flush() throws IOException {
                    Pipe.this.flush();
                }
            };
        }
    }
}
