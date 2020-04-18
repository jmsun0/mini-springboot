package com.sjm.core.util;

import java.util.Arrays;

public abstract class CharFilter implements Filter<Character> {
    public abstract boolean accept(int ch);

    public abstract int hashCode();

    public abstract boolean equals(Object obj);

    @Override
    public boolean accept(Character data) {
        return accept((char) data);
    }

    public static CharFilter equal(int c) {
        return new EqualFilter(c);
    }

    public static CharFilter range(int min, int max) {
        return new RangeFilter(min, max);
    }

    public static CharFilter or(CharFilter c1, CharFilter c2) {
        return new OrFilter(c1, c2);
    }

    public static CharFilter or(CharFilter... cs) {
        return new OrsFilter(cs);
    }

    public static CharFilter not(CharFilter cf) {
        return new NotFilter(cf);
    }

    public static CharFilter ascii(String asc) {
        return new AsciiFilter(asc);
    }

    /**
     * 
     */
    public static final CharFilter JavaNameHead = ascii(Strings.NormalChars + "_$");

    public static final CharFilter JavaNameBody = ascii(Strings.CharAndNumber + "_$");

    public static final CharFilter HexNumber = ascii(Strings.NumberChars + "abcdef" + "ABCDEF");

    public static final CharFilter DecimalNumber = range('0', '9');

    public static final CharFilter OctalNumber = range('0', '7');

    public static final CharFilter BinaryNumber = range('0', '1');

    public static final CharFilter Chinese = or(range('\u4e00', '\u9fbb'),
            range('\uff01', '\uff20'), range('\u2018', '\u201d'), range('\u3001', '\u3011'));

    public static final CharFilter Any = new CharFilter() {
        @Override
        public boolean accept(int ch) {
            return ch != -1;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public String toString() {
            return "any";
        }
    };

    static class EqualFilter extends CharFilter {
        int c;

        EqualFilter(int c) {
            this.c = c;
        }

        @Override
        public boolean accept(int ch) {
            return ch == c;
        }

        @Override
        public int hashCode() {
            return EqualFilter.class.hashCode() ^ c;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof EqualFilter && c == ((EqualFilter) obj).c;
        }

        @Override
        public String toString() {
            return String.valueOf(c);
        }
    }
    static class RangeFilter extends CharFilter {
        int min, max;

        RangeFilter(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean accept(int ch) {
            return ch >= min && ch <= max;
        }

        @Override
        public int hashCode() {
            return RangeFilter.class.hashCode() ^ min ^ max;
        }

        @Override
        public boolean equals(Object obj) {
            RangeFilter ft;
            return this == obj || obj instanceof RangeFilter && min == (ft = (RangeFilter) obj).min
                    && max == ft.max;
        }

        @Override
        public String toString() {
            return min + "," + max;
        }
    }
    static class OrFilter extends CharFilter {
        CharFilter c1, c2;

        OrFilter(CharFilter c1, CharFilter c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean accept(int ch) {
            return c1.accept(ch) || c2.accept(ch);
        }

        @Override
        public int hashCode() {
            return OrFilter.class.hashCode() ^ c1.hashCode() ^ c2.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            OrFilter ft;
            return this == obj
                    || obj instanceof OrFilter && c1 == (ft = (OrFilter) obj).c1 && c2 == ft.c2;
        }

        @Override
        public String toString() {
            return c1 + "|" + c2;
        }
    }
    static class OrsFilter extends CharFilter {
        CharFilter[] cs;

        OrsFilter(CharFilter... cs) {
            this.cs = cs;
        }

        @Override
        public boolean accept(int ch) {
            for (CharFilter c : cs)
                if (c.accept(ch))
                    return true;
            return false;
        }

        @Override
        public int hashCode() {
            return OrsFilter.class.hashCode() ^ Arrays.hashCode(cs);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj
                    || obj instanceof OrsFilter && Arrays.equals(cs, ((OrsFilter) obj).cs);
        }

        @Override
        public String toString() {
            return Strings.combine(cs, "|");
        }
    }
    static class NotFilter extends CharFilter {
        CharFilter cf;

        NotFilter(CharFilter cf) {
            this.cf = cf;
        }

        @Override
        public boolean accept(int ch) {
            return !cf.accept(ch);
        }

        @Override
        public int hashCode() {
            return NotFilter.class.hashCode() ^ cf.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof NotFilter && cf.equals(((NotFilter) obj).cf);
        }

        @Override
        public String toString() {
            return "!" + cf;
        }
    }
    static class AsciiFilter extends CharFilter {
        String asc;
        boolean[] cs = new boolean[128];

        AsciiFilter(String asc) {
            this.asc = asc;
            for (int i = 0; i < asc.length(); i++)
                cs[asc.charAt(i)] = true;
        }

        @Override
        public boolean accept(int ch) {
            return ch >= 0 && ch < 128 && cs[ch];
        }

        Integer hashCode;

        @Override
        public int hashCode() {
            if (hashCode == null)
                hashCode = AsciiFilter.class.hashCode() ^ Arrays.hashCode(cs);
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj
                    || obj instanceof AsciiFilter && Arrays.equals(cs, ((AsciiFilter) obj).cs);
        }

        @Override
        public String toString() {
            return "ascii";
        }
    }
}
