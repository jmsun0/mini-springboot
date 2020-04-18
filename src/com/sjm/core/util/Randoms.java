package com.sjm.core.util;

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
        return nextString(len, Strings.CharAndNumber);
    }

    public interface Getter<T> {
        public T get();
    }

    public static Getter<Integer> intGetter(final int range) {
        return new Getter<Integer>() {
            @Override
            public Integer get() {
                return nextInt(range);
            }
        };
    }

    public static Getter<Integer> intGetter(final int from, final int to) {
        return new Getter<Integer>() {
            @Override
            public Integer get() {
                return nextInt(from, to);
            }
        };
    }

    public static Getter<String> stringGetter(final String chars, final int len) {
        return new Getter<String>() {
            @Override
            public String get() {
                return nextString(len, chars);
            }
        };
    }

    public static Getter<String> stringGetter(final int len) {
        return new Getter<String>() {
            @Override
            public String get() {
                return nextString(len);
            }
        };
    }
}
