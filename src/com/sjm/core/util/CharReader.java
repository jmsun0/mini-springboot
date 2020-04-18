package com.sjm.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;


public abstract class CharReader implements Closeable {
    public abstract int read();

    @Override
    public void close() throws IOException {}

    public static CharReader valueOf(final InputStream is) {
        return new CharReader() {
            @Override
            public int read() {
                try {
                    return is.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static CharReader valueOf(final Reader reader) {
        return new CharReader() {
            @Override
            public int read() {
                try {
                    return reader.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static CharReader valueOf(final CharSequence cs) {
        return new CharReader() {
            int index;
            int len = cs.length();

            @Override
            public int read() {
                return index == len ? -1 : cs.charAt(index++);
            }
        };
    }

    public static CharReader valueOf(final byte[] bytes) {
        return new CharReader() {
            int index;
            int len = bytes.length;

            @Override
            public int read() {
                return index == len ? -1 : bytes[index++];
            }
        };
    }

    public static CharReader valueOf(final char[] chars) {
        return new CharReader() {
            int index;
            int len = chars.length;

            @Override
            public int read() {
                return index == len ? -1 : chars[index++];
            }
        };
    }
}
