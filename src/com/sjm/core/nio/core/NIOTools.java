package com.sjm.core.nio.core;

public class NIOTools {

    public static int getInt(byte[] bytes) {
        int val = 0;
        for (int i = 0; i < 4; i++) {
            val += (bytes[i] & 0xff) << (8 * (3 - i));
        }
        return val;
    }

    public static void putInt(byte[] bytes, int val) {
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) ((val >>> (8 * (3 - i))) & 0xff);
        }
    }

    public static long getLong(byte[] bytes) {
        long val = 0;
        for (int i = 0; i < 8; i++) {
            val += (bytes[i] & 0xffl) << (8 * (7 - i));
        }
        return val;
    }

    public static void putLong(byte[] bytes, long val) {
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) ((val >>> (8 * (7 - i))) & 0xff);
        }
    }
}
