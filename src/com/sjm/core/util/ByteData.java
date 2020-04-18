package com.sjm.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public abstract class ByteData implements Closeable {
    public static ByteData valueOf(InputStream data) {
        return new StreamData(data);
    }

    public static ByteData valueOf(Reader data, String charset) {
        return new ReaderData(data, charset);
    }

    public static ByteData valueOf(Reader data) {
        return new ReaderData(data, null);
    }

    public static ByteData valueOf(byte[] data) {
        return new BytesData(data);
    }

    public static ByteData valueOf(String data, String charset) {
        return new StringData(data, charset);
    }

    public static ByteData valueOf(String data) {
        return new StringData(data, null);
    }

    public static ByteData valueOf(char[] data, String charset) {
        return new CharsData(data, charset);
    }

    public static ByteData valueOf(char[] data) {
        return new CharsData(data, null);
    }

    public static ByteData valueOf(File data) {
        return new FileData(data);
    }

    public abstract InputStream toStream() throws IOException;

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.copy(toStream(), baos, getByteBuffer(), false);
        return baos.toByteArray();
    }

    public Reader toReader(String charset) throws IOException {
        return new InputStreamReader(toStream(), IOUtil.getCharset(charset));
    }

    public Reader toReader() throws IOException {
        return toReader(null);
    }

    public char[] toCharArray(String charset) throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        IOUtil.copy(toReader(charset), caw, getCharBuffer(), false);
        return caw.toCharArray();
    }

    public char[] toCharArray() throws IOException {
        return toCharArray(null);
    }

    public String toString(String charSet) throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.copy(toReader(charSet), sw, getCharBuffer(), false);
        return sw.toString();
    }

    public String toString() {
        try {
            return toString(null);
        } catch (IOException e) {
            return null;
        }
    }

    public void write(OutputStream os) throws IOException {
        IOUtil.copy(toStream(), os, getByteBuffer(), false);
    }

    public void write(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        write(fos);
        fos.close();
    }

    public void write(Writer writer, String charset) throws IOException {
        IOUtil.copy(toReader(charset), writer, getCharBuffer(), false);
    }

    public void write(Writer writer) throws IOException {
        write(writer, null);
    }

    public void reset() throws IOException {}

    private Closeable closeable;

    public ByteData withCloseable(Closeable closeable) {
        this.closeable = closeable;
        return this;
    }

    @Override
    public void close() throws IOException {
        if (closeable != null)
            closeable.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    private WeakReference<byte[]> byteBuffer;
    private WeakReference<char[]> charBuffer;

    protected byte[] getByteBuffer() {
        byte[] buf;
        if (byteBuffer == null || (buf = byteBuffer.get()) == null)
            byteBuffer = new WeakReference<byte[]>(buf = new byte[IOUtil.DEFAULT_BUFFER_SIZE]);
        return buf;
    }

    protected char[] getCharBuffer() {
        char[] buf;
        if (charBuffer == null || (buf = charBuffer.get()) == null)
            charBuffer = new WeakReference<char[]>(buf = new char[IOUtil.DEFAULT_BUFFER_SIZE]);
        return buf;
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */
    public static final ByteData EMPTY_DATA = new ByteData() {
        @Override
        public InputStream toStream() throws IOException {
            return IOUtil.EMPTY_STREAM;
        }

        public byte[] toByteArray() throws IOException {
            return Lists.emptyByteArray;
        }

        public Reader toReader(String charset) throws IOException {
            return IOUtil.EMPTY_READER;
        }

        public char[] toCharArray(String charset) throws IOException {
            return Lists.emptyCharArray;
        }

        public String toString(String charSet) throws IOException {
            return "";
        }

        public void write(OutputStream os) throws IOException {}

        public void write(File file) throws IOException {
            new FileOutputStream(file).close();
        }

        public void write(Writer writer, String charset) throws IOException {}
    };

    static class StreamData extends ByteData {
        public InputStream data;

        public StreamData(InputStream data) {
            this.data = data;
        }

        @Override
        public InputStream toStream() throws IOException {
            return data;
        }

        @Override
        public void close() throws IOException {
            data.close();
            super.close();
        }

        @Override
        public void reset() throws IOException {
            data.reset();
            super.reset();
        }
    }

    static class ReaderData extends ByteData {
        public Reader data;
        public String charset;

        public ReaderData(Reader data, String charset) {
            this.data = data;
            this.charset = charset;
        }

        @Override
        public InputStream toStream() throws IOException {
            return IOUtil.toInputStream(data, charset);
        }

        @Override
        public Reader toReader(String charSet) throws IOException {
            Charset cs = IOUtil.getCharset(charSet);
            if (cs.equals(IOUtil.getCharset(charset)))
                return data;
            return new InputStreamReader(toStream(), cs);
        }

        @Override
        public void close() throws IOException {
            super.close();
            data.close();
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            data.reset();
        }
    }

    static class BytesData extends ByteData {
        public byte[] data;

        public BytesData(byte[] data) {
            this.data = data;
        }

        @Override
        public InputStream toStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public byte[] toByteArray() {
            return data;
        }

        @Override
        public char[] toCharArray(String charset) throws IOException {
            return IOUtil.decodeCharset(data, IOUtil.getCharset(charset));
        }

        @Override
        public String toString(String charset) throws IOException {
            return new String(data, IOUtil.getCharset(charset));
        }

        @Override
        public void write(OutputStream os) throws IOException {
            os.write(data);
        }

        @Override
        public void write(Writer writer, String charset) throws IOException {
            OutputStream os = IOUtil.toOutputStream(writer, charset);
            os.write(data);
            os.flush();
        }
    }

    static class FileData extends ByteData {
        public File file;
        public InputStream is;

        public FileData(File file) {
            this.file = file;
        }

        @Override
        public InputStream toStream() throws IOException {
            if (is == null)
                is = new FileInputStream(file);
            return is;
        }

        @Override
        public void close() throws IOException {
            if (is != null) {
                is.close();
                is = null;
            }
        }

        @Override
        public void reset() throws IOException {
            toStream().reset();
        }
    }

    static class StringData extends ByteData {
        public String data;
        public String charset;

        public StringData(String data, String charset) {
            this.data = data;
            this.charset = charset;
        }

        @Override
        public InputStream toStream() throws IOException {
            return IOUtil.toInputStream(new StringReader(data), charset);
        }

        @Override
        public byte[] toByteArray() throws IOException {
            return data.getBytes(IOUtil.getCharset(charset));
        }

        @Override
        public Reader toReader(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return new StringReader(data);
            return new InputStreamReader(toStream(), charset);
        }

        @Override
        public String toString(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return data;
            return new String(toByteArray(), IOUtil.getCharset(charset));
        }

        @Override
        public char[] toCharArray(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return data.toCharArray();
            return IOUtil.decodeCharset(toByteArray(), IOUtil.getCharset(charset));
        }
    }

    static class CharsData extends ByteData {
        public char[] data;
        public String charset;

        public CharsData(char[] data, String charset) {
            this.data = data;
            this.charset = charset;
        }

        @Override
        public InputStream toStream() throws IOException {
            return IOUtil.toInputStream(new CharArrayReader(data), charset);
        }

        @Override
        public byte[] toByteArray() throws IOException {
            return IOUtil.encodeCharset(data, IOUtil.getCharset(charset));
        }

        @Override
        public Reader toReader(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return new CharArrayReader(data);
            return new InputStreamReader(toStream(), charset);
        }

        @Override
        public String toString(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return new String(data);
            return new String(toByteArray(), IOUtil.getCharset(charset));
        }

        @Override
        public char[] toCharArray(String charset) throws IOException {
            if (IOUtil.compareCharset(charset, this.charset))
                return data;
            CharBuffer cb = IOUtil.getCharset(charset)
                    .decode(IOUtil.getCharset(this.charset).encode(CharBuffer.wrap(data)));
            return Arrays.copyOfRange(cb.array(), 0, cb.limit());
        }
    }
}
