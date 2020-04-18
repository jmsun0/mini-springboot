package com.sjm.core.util;

public enum Size {
    B(1), KB(1024), MB(1024 * 1024), GB(1024 * 1024 * 1024), TB(1024 * 1024 * 1024 * 1024);

    private int bytes;

    private Size(int bytes) {
        this.bytes = bytes;
    }

    // int
    public int toB(int count) {
        return bytes * count;
    }

    public int toKB(int count) {
        return bytes * count / 1024;
    }

    public int toMB(int count) {
        return bytes * count / (1024 * 1024);
    }

    public int toGB(int count) {
        return bytes * count / (1024 * 1024 * 1024);
    }

    public int toTB(int count) {
        return bytes * count / (1024 * 1024 * 1024 * 1024);
    }

    // long
    public long toB(long count) {
        return bytes * count;
    }

    public long toKB(long count) {
        return bytes * count / 1024;
    }

    public long toMB(long count) {
        return bytes * count / (1024 * 1024);
    }

    public long toGB(long count) {
        return bytes * count / (1024 * 1024 * 1024);
    }

    public long toTB(long count) {
        return bytes * count / (1024 * 1024 * 1024 * 1024);
    }

    // float
    public float toB(float count) {
        return bytes * count;
    }

    public float toKB(float count) {
        return bytes * count / 1024;
    }

    public float toMB(float count) {
        return bytes * count / (1024 * 1024);
    }

    public float toGB(float count) {
        return bytes * count / (1024 * 1024 * 1024);
    }

    public float toTB(float count) {
        return bytes * count / (1024 * 1024 * 1024 * 1024);
    }

    // double
    public double toB(double count) {
        return bytes * count;
    }

    public double toKB(double count) {
        return bytes * count / 1024;
    }

    public double toMB(double count) {
        return bytes * count / (1024 * 1024);
    }

    public double toGB(double count) {
        return bytes * count / (1024 * 1024 * 1024);
    }

    public double toTB(double count) {
        return bytes * count / (1024 * 1024 * 1024 * 1024);
    }

    public static long parseSize(String sizeStr) {
        return SizeParser.parse(sizeStr);
    }

    static class SizeParser {
        static final Integer EOF = 1;
        static final Integer BLANK = 2;
        static final Integer NUMBER = 3;

        static Analyzer<Object> sizeAnalyzer;

        private static void addAnalyzerUnit(Size size, String... lowCaseKeys) {
            for (String key : lowCaseKeys) {
                sizeAnalyzer.setSymbol(key, size);
                sizeAnalyzer.setSymbol(key.toUpperCase(), size);
            }
        }

        static {
            sizeAnalyzer = new Analyzer<>();
            sizeAnalyzer.setEOF(EOF);
            sizeAnalyzer.setBlank(" ", BLANK);
            sizeAnalyzer.setNumber(NUMBER);
            addAnalyzerUnit(Size.B, "bytes", "byte", "b");
            addAnalyzerUnit(Size.KB, "kb", "k");
            addAnalyzerUnit(Size.MB, "mb", "m");
            addAnalyzerUnit(Size.GB, "gb", "g");
            addAnalyzerUnit(Size.TB, "tb", "t");
        }

        public static long parse(String sizeStr) {
            Source<Object> src = sizeAnalyzer.analyze(sizeStr);
            src = Source.filter(src, v -> v != BLANK);
            Object key = src.next();
            Number size;
            if (key == NUMBER) {
                size = Numbers.parseDeclareNumber(src.getValue(), null, -1, -1);
                key = src.next();
            } else
                throw new IllegalArgumentException();
            Size unit;
            if (key instanceof Size) {
                unit = (Size) key;
                key = src.next();
            } else
                throw new IllegalArgumentException();
            if (key != EOF)
                throw new IllegalArgumentException();
            if (size instanceof Integer || size instanceof Long)
                return unit.toB(size.longValue());
            else
                return (long) unit.toB(size.doubleValue());
        }
    }

    public static void main(String[] args) {
        System.out.println(parseSize("1.13e5 mb"));
    }
}

