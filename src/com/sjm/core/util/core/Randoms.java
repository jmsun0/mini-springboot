package com.sjm.core.util.core;

public class Randoms {
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;
    private static final long seedUniquifier = 8682522807148012L;
    private static long seed;
    static {
        long s = seedUniquifier + System.nanoTime();
        s = (s ^ multiplier) & mask;
        seed = s;
    }

    private static int next(int bits) {
        long oldSeed = seed, nextSeed;
        do {
            nextSeed = (oldSeed * multiplier + addend) & mask;
        } while (oldSeed == nextSeed);
        seed = nextSeed;
        return (int) (nextSeed >>> (48 - bits));
    }

    public static int nextInt() {
        return next(32);
    }

    public static int nextInt(int bound) {
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)
            r = (int) ((bound * (long) r) >> 31);
        else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31));
        }
        return r;
    }

    public static int nextInt(int from, int to) {
        return nextInt(to - from + 1) + from;
    }

    public static long nextLong() {
        return ((long) (next(32)) << 32) + next(32);
    }

    public static boolean nextBoolean() {
        return next(1) != 0;
    }

    public static float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public static double nextDouble() {
        return (((long) (next(26)) << 27) + next(27)) * 0x1.0p-53;
    }

    public static String nextString(int len, String chars) {
        int strLen = chars.length();
        char[] cs = new char[len];
        for (int i = 0; i < len; i++)
            cs[i] = chars.charAt(nextInt(strLen));
        return new String(cs);
    }

    public static String nextString(int len) {
        return nextString(len, Strings.LETTER_NUMBER);
    }
}
