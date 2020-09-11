package com.sjm.core.util.misc;

import com.sjm.core.util.core.Lex;
import com.sjm.core.util.core.Numbers;
import com.sjm.core.util.core.Strings;

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
        SizeLex lex = new SizeLex();
        lex.resetAndNext(sizeStr);
        return lex.parse();
    }

    static class SizeLex extends Lex.StringLex<Object> {
        static final Integer EOF = 1;
        static final Integer NUM = 2;

        private static final DFAState START = new SizeBuilder().build();

        public SizeLex() {
            super(START);
        }

        static class SizeBuilder extends Lex.Builder<SizeLex> {
            public DFAState build() {
                initNFA();

                defineActionTemplate("finish", (lex, a) -> lex.finish(a[0]));

                defineVariable("EOF", EOF);
                defineVariable("NUM", NUM);
                for (Size key : Size.values())
                    defineVariable(key.name(), key);

                addPattern("START", "[\r\n\t\b\f ]+#{finish(null)}");
                addPattern("START", "[$]#{finish(EOF)}");
                addPattern("START", "[0-9]+(\\.[0-9]+)?#{finish(NUM)}");
                addCaseInsensitivePatterns("B", "bytes", "byte", "b");
                addCaseInsensitivePatterns("KB", "kb", "k");
                addCaseInsensitivePatterns("MB", "mb", "m");
                addCaseInsensitivePatterns("GB", "gb", "g");
                addCaseInsensitivePatterns("TB", "tb", "t");
                return buildDFA("START");
            }

            private void addCaseInsensitivePatterns(String finishKey, String... matchKeys) {
                StringBuilder sb = new StringBuilder();
                for (String matchKey : matchKeys) {
                    sb.append("(");
                    for (int i = 0; i < matchKey.length(); i++) {
                        char ch = matchKey.charAt(i);
                        sb.append("(").append(Strings.toUpcase(ch)).append("|")
                                .append(Strings.toLowcase(ch)).append(")");
                    }
                    sb.append(")|");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("#{finish(").append(finishKey).append(")}");
                addPattern("START", sb.toString());
            }
        }

        @Override
        public RuntimeException newError(String message) {
            return new IllegalArgumentException(message);
        }

        public long parse() {
            if (key != NUM)
                throw newError();
            Number size = Numbers.parseDeclareNumber(str, begin, end);
            next();
            if (!(key instanceof Size))
                throw newError();
            Size unit = (Size) key;
            next();
            if (key != EOF)
                throw newError();
            if (size instanceof Integer || size instanceof Long)
                return unit.toB(size.longValue());
            else
                return (long) unit.toB(size.doubleValue());
        }
    }

    public static void main(String[] args) {
        System.out.println(parseSize("1.12 Mb"));
    }
}

