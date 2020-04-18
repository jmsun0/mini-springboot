package com.sjm.core.util;

import java.math.BigInteger;
import java.util.Arrays;

public class Numbers {
    static final int[] LocateNumber = new int[128];

    static {
        Arrays.fill(LocateNumber, Integer.MAX_VALUE);
        for (int i = 0; i < 10; i++)
            LocateNumber[i + '0'] = i;
        for (int i = 0; i < 26; i++) {
            LocateNumber[i + 'a'] = i + 0xa;
            LocateNumber[i + 'A'] = i + 0xa;
        }
    }

    public static final String[] MaxIntString = new String[17];
    public static final String[] MinIntString = new String[17];
    public static final String[] MaxLongString = new String[17];
    public static final String[] MinLongString = new String[17];

    static {
        for (int i = 2; i < 17; i++) {
            MinIntString[i] = Integer.toString(Integer.MIN_VALUE, i);
            MaxIntString[i] = Integer.toString(Integer.MAX_VALUE, i);
            MinLongString[i] = Long.toString(Long.MIN_VALUE, i);
            MaxLongString[i] = Long.toString(Long.MAX_VALUE, i);
        }
    }

    public static int log(int a, int b) {
        int n = -1;
        while (b != 0) {
            b /= a;
            n++;
        }
        return n;
    }

    public static int log(long a, long b) {
        int n = -1;
        while (b != 0) {
            b /= a;
            n++;
        }
        return n;
    }

    public static int getBit(int n, int radix) {
        return n > 0 ? log(radix, n) + 1
                : (n == 0 ? 1
                        : (n == Integer.MIN_VALUE ? MinIntString[radix].length()
                                : log(radix, -n) + 2));
    }

    public static int getBit(long n, int radix) {
        return n > 0 ? log(radix, n) + 1
                : (n == 0 ? 1
                        : (n == Long.MIN_VALUE ? MinLongString[radix].length()
                                : log(radix, -n) + 2));
    }

    public static <S> int parseIntWithoutSign(S str, Strings.StringController<S> ctr, int radix,
            int begin, int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        int value = 0;
        for (; begin < end; begin++) {
            int h = LocateNumber[ctr.get(str, begin)];
            if (h >= radix)
                throw new NumberFormatException("非数字字符:" + ctr.get(str, begin));
            value *= radix;
            value += h;
            if (value < 0 && value != Integer.MIN_VALUE)
                throw new NumberFormatException("数字过长");
        }
        return value;
    }

    public static <S> long parseLongWithoutSign(S str, Strings.StringController<S> ctr, int radix,
            int begin, int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        long value = 0;
        for (; begin < end; begin++) {
            int h = LocateNumber[ctr.get(str, begin)];
            if (h >= radix)
                throw new NumberFormatException("非数字字符:" + ctr.get(str, begin));
            value *= radix;
            value += h;
            if (value < 0 && value != Long.MIN_VALUE)
                throw new NumberFormatException("数字过长");
        }
        return value;
    }

    public static <S> int parseInt(S str, Strings.StringController<S> ctr, int radix, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        char c = ctr.get(str, begin);
        if (c == '-')
            return -parseIntWithoutSign(str, ctr, radix, begin + 1, end);
        else if (c == '+')
            return parseIntWithoutSign(str, ctr, radix, begin + 1, end);
        else
            return parseIntWithoutSign(str, ctr, radix, begin, end);
    }

    public static <S> long parseLong(S str, Strings.StringController<S> ctr, int radix, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        char c = ctr.get(str, begin);
        if (c == '-')
            return -parseLongWithoutSign(str, ctr, radix, begin + 1, end);
        else if (c == '+')
            return parseLongWithoutSign(str, ctr, radix, begin + 1, end);
        else
            return parseLongWithoutSign(str, ctr, radix, begin, end);
    }

    public static <S> Number parseInteger(S str, Strings.StringController<S> ctr, int radix,
            int begin, int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        char ch = ctr.get(str, begin);
        if (ch == '-') {
            int c = Strings.compare(str, ctr, begin + 1, end, MinIntString[radix],
                    Strings.StringCtr, 0, -1);
            if (c < 0)
                return parseInt(str, ctr, radix, begin, end);
            else if (c == 0)
                return Integer.MIN_VALUE;
            else {
                c = Strings.compare(str, ctr, begin + 1, end, MinLongString[radix],
                        Strings.StringCtr, 0, -1);
                if (c < 0)
                    return parseLong(str, ctr, radix, begin, end);
                else if (c == 0)
                    return Long.MIN_VALUE;
            }
        } else if (ch == '+') {
            int c = Strings.compare(str, ctr, begin + 1, end, MaxIntString[radix],
                    Strings.StringCtr, 0, -1);
            if (c < 0)
                return parseInt(str, ctr, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, ctr, begin + 1, end, MaxLongString[radix],
                        Strings.StringCtr, 0, -1);
                if (c < 0)
                    return parseLong(str, ctr, radix, begin, end);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        } else {
            int c = Strings.compare(str, ctr, begin, end, MaxIntString[radix], Strings.StringCtr, 0,
                    -1);
            if (c < 0)
                return parseInt(str, ctr, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, ctr, begin, end, MaxLongString[radix], Strings.StringCtr,
                        0, -1);
                if (c < 0)
                    return parseLong(str, ctr, begin, end, radix);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        }
        return new BigInteger(ctr.toString(str, begin, end), radix);
    }

    public static void getCharsWithoutSign(int n, char[] numbers, int radix, char[] dst, int toLeft,
            int fromRight) {
        for (; fromRight >= toLeft; fromRight--) {
            dst[fromRight] = numbers[n % radix];
            n = n / radix;
        }
    }

    public static void getCharsWithoutSign(long n, char[] numbers, int radix, char[] dst,
            int toLeft, int fromRight) {
        for (; fromRight >= toLeft; fromRight--) {
            dst[fromRight] = numbers[(int) (n % radix)];
            n = n / radix;
        }
    }

    private static void copyMinNumber(String num, char zero, char[] dst, int toLeft,
            int fromRight) {
        int numLen = num.length();
        int i = fromRight - numLen + 2;
        num.getChars(1, numLen, dst, i);
        dst[toLeft++] = '-';
        while (toLeft < i)
            dst[toLeft++] = zero;
    }

    public static void getChars(int n, char[] numbers, int radix, char[] dst, int toLeft,
            int fromRight) {
        if (n < 0) {
            if (n == Integer.MIN_VALUE) {
                copyMinNumber(MinIntString[radix], numbers[0], dst, toLeft, fromRight);
                return;
            }
            n = -n;
            dst[toLeft++] = '-';
        }
        getCharsWithoutSign(n, numbers, radix, dst, toLeft, fromRight);
    }

    public static void getChars(long n, char[] numbers, int radix, char[] dst, int toLeft,
            int fromRight) {
        if (n < 0) {
            if (n == Long.MIN_VALUE) {
                copyMinNumber(MinLongString[radix], numbers[0], dst, toLeft, fromRight);
                return;
            }
            n = -n;
            dst[toLeft++] = '-';
        }
        getCharsWithoutSign(n, numbers, radix, dst, toLeft, fromRight);
    }

    public static String toString(int n, char[] numbers, int radix, int len) {
        len = Math.max(len, getBit(n, radix));
        char[] c = new char[len];
        getChars(n, numbers, radix, c, 0, len - 1);
        return new String(c);
    }

    public static String toString(long n, char[] numbers, int radix, int len) {
        len = Math.max(len, getBit(n, radix));
        char[] c = new char[len];
        getChars(n, numbers, radix, c, 0, len - 1);
        return new String(c);
    }

    public static <S> long parseLongWithRadix(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        char c = ctr.get(str, begin);
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (ctr.get(str, begin) == '0') {
            if (begin + 1 < end) {
                char f = Strings.toLowCase(ctr.get(str, begin));
                if (f == 'x') {
                    radix = 16;
                    begin += 2;
                } else if (f == 'b') {
                    radix = 2;
                    begin += 2;
                }
            }
        }
        long num = parseLong(str, ctr, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static <S> int parseIntWithRadix(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        char c = ctr.get(str, begin);
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (ctr.get(str, begin) == '0') {
            begin++;
            char f = Strings.toLowCase(ctr.get(str, begin));
            if (f == 'x') {
                radix = 16;
                begin++;
            } else if (f == 'b') {
                radix = 2;
                begin++;
            }
        }
        int num = parseInt(str, ctr, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static <S> Number parseDeclareNumber(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        int e = 0;
        char suffix = 0;
        int point = 0;
        char fh1 = 0, fh2 = 0;
        int x1 = 0, x2 = 0;
        for (int i = begin; i < end; i++) {
            char c = ctr.get(str, i);
            if (!Strings.isNumber(c)) {
                c = Strings.toLowCase(c);
                if (c == 'x') {
                    if (ctr.get(str, i - 1) == '0') {
                        if (i == 1 || i == 2) {
                            if (x1 == 0) {
                                x1 = i;
                                continue;
                            }
                        } else if (i == e + 2) {
                            if (x2 == 0) {
                                x2 = i;
                                continue;
                            }
                        }
                    }
                } else if (c == 'e') {
                    if (e == 0 && i != 0) {
                        e = i;
                        continue;
                    }
                } else if (c == 'f' || c == 'l') {
                    if (i == end - 1 && suffix == 0) {
                        suffix = c;
                        continue;
                    }
                } else if (c == '.') {
                    if (e == 0 && point == 0) {
                        point = i;
                        continue;
                    }
                } else if (c == '+' || c == '-') {
                    if (i == 0) {
                        if (fh1 == 0) {
                            fh1 = c;
                            continue;
                        }
                    } else if (Strings.toLowCase(ctr.get(str, i - 1)) == 'e') {
                        if (fh2 == 0) {
                            fh2 = c;
                            continue;
                        }
                    }
                }
                throw new NumberFormatException("非法字符:" + c);
            }
        }
        if (suffix != 0)
            end--;
        Number n;
        if (e == 0) {
            if (point == 0) {
                n = parseLongWithRadix(str, ctr, begin, end);
            } else
                n = Double.parseDouble(ctr.toString(str, begin, end));
        } else {
            String s1;
            String s2;
            if (x1 != 0)
                s1 = parseIntWithRadix(str, ctr, 0, e) + "";
            else
                s1 = ctr.toString(str, 0, e);
            if (x2 != 0)
                s2 = parseIntWithRadix(str, ctr, e + 1, end) + "";
            else
                s2 = ctr.toString(str, e + 1, end);
            n = Double.parseDouble(s1 + "e" + s2);
        }
        if (suffix == 'f') {
            return n.floatValue();
        } else if (suffix == 'l') {
            return n.longValue();
        } else if (point != 0 || e != 0) {
            return n.doubleValue();
        } else
            return n.intValue();
    }

    private static char Ling = '零';
    private static char Wan = '万';
    private static char Yi = '亿';
    private static char Fu = '负';
    private static String[] CN = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static String[] BN = {"", "十", "百", "千"};

    public static String toChineseNumber(long n) {
        return appendChineseNumber(new StringBuilder(), n).toString();
    }

    public static StringBuilder appendChineseNumber(StringBuilder sb, long n) {
        if (n < 0) {
            sb.append(Fu);
            n = -n;
        }
        if (n == 0)
            sb.append(Ling);
        else if (n < 10)
            sb.append(CN[(int) n]);
        else if (n < 20)
            sb.append(BN[1]).append(CN[(int) (n % 10)]);
        else if (n < 10000) {
            appendLessThanWan(sb, (int) n, 0, -1);
            if (sb.charAt(sb.length() - 1) == Ling)
                sb.delete(sb.length() - 1, sb.length());
        } else if (n < 100000000) {
            appendChineseNumber(sb, n / 10000);
            sb.append(Wan);
            long m = n % 10000;
            if (m != 0) {
                if (m < 1000)
                    sb.append(Ling);
                appendChineseNumber(sb, m);
            }
        } else {
            appendChineseNumber(sb, n / 100000000);
            sb.append(Yi);
            long m = n % 100000000;
            if (m != 0) {
                if (m < 10000000)
                    sb.append(Ling);
                appendChineseNumber(sb, m);
            }
        }
        return sb;
    }

    private static void appendLessThanWan(StringBuilder sb, int n, int x, int lx) {
        int a = n % 10;
        int b = n / 10;
        if (b != 0)
            appendLessThanWan(sb, b, x + 1, a != 0 ? x : lx);
        if (a != 0)
            sb.append(CN[a]).append(BN[x]);
        if (x != 1 && lx + 1 != x && sb.charAt(sb.length() - 1) != Ling)
            sb.append(Ling);
    }

    public static int getChars(float v, char[] cs, int off) {
        return new FloatNumber(v).getChars(cs, off);
    }

    public static int getChars(double v, char[] cs, int off) {
        return new FloatNumber(v).getChars(cs, off);
    }

    public static class BigInt extends Number implements Comparable<BigInt>, Cloneable {
        private static final long serialVersionUID = 1L;

        private int nWords;
        private int data[];

        public BigInt(int v) {
            nWords = 1;
            data = new int[1];
            data[0] = v;
        }

        public BigInt(long v) {
            data = new int[2];
            data[0] = (int) v;
            data[1] = (int) (v >>> 32);
            nWords = (data[1] == 0) ? 1 : 2;
        }

        private BigInt(int[] d, int n) {
            data = d;
            nWords = n;
        }

        public BigInt(long seed, char digit[], int nd0, int nd) {
            int n = (nd + 8) / 9;
            if (n < 2)
                n = 2;
            data = new int[n];
            data[0] = (int) seed;
            data[1] = (int) (seed >>> 32);
            nWords = (data[1] == 0) ? 1 : 2;
            int i = nd0;
            int limit = nd - 5;
            int v;
            while (i < limit) {
                int ilim = i + 5;
                v = (int) digit[i++] - (int) '0';
                while (i < ilim) {
                    v = 10 * v + (int) digit[i++] - (int) '0';
                }
                multaddMe(100000, v);
            }
            int factor = 1;
            v = 0;
            while (i < nd) {
                v = 10 * v + (int) digit[i++] - (int) '0';
                factor *= 10;
            }
            if (factor != 1) {
                multaddMe(factor, v);
            }
        }

        public void lshiftMe(int c) {
            if (c <= 0) {
                if (c == 0)
                    return;
                else
                    throw new IllegalArgumentException("negative shift count");
            }
            int wordcount = c >> 5;
            int bitcount = c & 0x1f;
            int anticount = 32 - bitcount;
            int t[] = data;
            int s[] = data;
            if (nWords + wordcount + 1 > t.length) {
                t = new int[nWords + wordcount + 1];
            }
            int target = nWords + wordcount;
            int src = nWords - 1;
            if (bitcount == 0) {
                System.arraycopy(s, 0, t, wordcount, nWords);
                target = wordcount - 1;
            } else {
                t[target--] = s[src] >>> anticount;
                while (src >= 1) {
                    t[target--] = (s[src] << bitcount) | (s[--src] >>> anticount);
                }
                t[target--] = s[src] << bitcount;
            }
            while (target >= 0) {
                t[target--] = 0;
            }
            data = t;
            nWords += wordcount + 1;
            while (nWords > 1 && data[nWords - 1] == 0)
                nWords--;
        }

        public int normalizeMe() {
            int src;
            int wordcount = 0;
            int bitcount = 0;
            int v = 0;
            for (src = nWords - 1; src >= 0 && (v = data[src]) == 0; src--) {
                wordcount += 1;
            }
            if (src < 0) {
                throw new IllegalArgumentException("zero value");
            }
            nWords -= wordcount;
            if ((v & 0xf0000000) != 0) {
                for (bitcount = 32; (v & 0xf0000000) != 0; bitcount--)
                    v >>>= 1;
            } else {
                while (v <= 0x000fffff) {
                    v <<= 8;
                    bitcount += 8;
                }
                while (v <= 0x07ffffff) {
                    v <<= 1;
                    bitcount += 1;
                }
            }
            if (bitcount != 0)
                lshiftMe(bitcount);
            return bitcount;
        }

        public BigInt mult(int iv) {
            long v = iv;
            int r[];
            long p;
            r = new int[(v * ((long) data[nWords - 1] & 0xffffffffL) > 0xfffffffL) ? nWords + 1
                    : nWords];
            p = 0L;
            for (int i = 0; i < nWords; i++) {
                p += v * ((long) data[i] & 0xffffffffL);
                r[i] = (int) p;
                p >>>= 32;
            }
            if (p == 0L) {
                return new BigInt(r, nWords);
            } else {
                r[nWords] = (int) p;
                return new BigInt(r, nWords + 1);
            }
        }

        public void multaddMe(int iv, int addend) {
            long v = iv;
            long p;
            p = v * ((long) data[0] & 0xffffffffL) + ((long) addend & 0xffffffffL);
            data[0] = (int) p;
            p >>>= 32;
            for (int i = 1; i < nWords; i++) {
                p += v * ((long) data[i] & 0xffffffffL);
                data[i] = (int) p;
                p >>>= 32;
            }
            if (p != 0L) {
                data[nWords] = (int) p;
                nWords++;
            }
        }

        public BigInt mult(BigInt other) {
            int r[] = new int[nWords + other.nWords];
            int i;
            for (i = 0; i < this.nWords; i++) {
                long v = (long) this.data[i] & 0xffffffffL;
                long p = 0L;
                int j;
                for (j = 0; j < other.nWords; j++) {
                    p += ((long) r[i + j] & 0xffffffffL) + v * ((long) other.data[j] & 0xffffffffL);
                    r[i + j] = (int) p;
                    p >>>= 32;
                }
                r[i + j] = (int) p;
            }
            for (i = r.length - 1; i > 0; i--)
                if (r[i] != 0)
                    break;
            return new BigInt(r, i + 1);
        }

        public BigInt add(BigInt other) {
            int i;
            int a[], b[];
            int n, m;
            long c = 0L;
            if (this.nWords >= other.nWords) {
                a = this.data;
                n = this.nWords;
                b = other.data;
                m = other.nWords;
            } else {
                a = other.data;
                n = other.nWords;
                b = this.data;
                m = this.nWords;
            }
            int r[] = new int[n];
            for (i = 0; i < n; i++) {
                c += (long) a[i] & 0xffffffffL;
                if (i < m) {
                    c += (long) b[i] & 0xffffffffL;
                }
                r[i] = (int) c;
                c >>= 32;
            }
            if (c != 0L) {
                int s[] = new int[r.length + 1];
                System.arraycopy(r, 0, s, 0, r.length);
                s[i++] = (int) c;
                return new BigInt(s, i);
            }
            return new BigInt(r, i);
        }

        public BigInt sub(BigInt other) {
            int r[] = new int[this.nWords];
            int i;
            int n = this.nWords;
            int m = other.nWords;
            int nzeros = 0;
            long c = 0L;
            for (i = 0; i < n; i++) {
                c += (long) this.data[i] & 0xffffffffL;
                if (i < m) {
                    c -= (long) other.data[i] & 0xffffffffL;
                }
                if ((r[i] = (int) c) == 0)
                    nzeros++;
                else
                    nzeros = 0;
                c >>= 32;
            }
            Misc.asserts(c == 0L, c);
            Misc.asserts(dataInRangeIsZero(i, m, other));
            return new BigInt(r, n - nzeros);
        }

        private static boolean dataInRangeIsZero(int i, int m, BigInt other) {
            while (i < m)
                if (other.data[i++] != 0)
                    return false;
            return true;
        }

        public int quoRemIteration(BigInt S) {
            if (nWords != S.nWords) {
                throw new IllegalArgumentException("disparate values");
            }
            int n = nWords - 1;
            long q = ((long) data[n] & 0xffffffffL) / (long) S.data[n];
            long diff = 0L;
            for (int i = 0; i <= n; i++) {
                diff += ((long) data[i] & 0xffffffffL) - q * ((long) S.data[i] & 0xffffffffL);
                data[i] = (int) diff;
                diff >>= 32;
            }
            if (diff != 0L) {
                long sum = 0L;
                while (sum == 0L) {
                    sum = 0L;
                    for (int i = 0; i <= n; i++) {
                        sum += ((long) data[i] & 0xffffffffL) + ((long) S.data[i] & 0xffffffffL);
                        data[i] = (int) sum;
                        sum >>= 32;
                    }
                    Misc.asserts(sum == 0 || sum == 1, sum);
                    q -= 1;
                }
            }
            long p = 0L;
            for (int i = 0; i <= n; i++) {
                p += 10 * ((long) data[i] & 0xffffffffL);
                data[i] = (int) p;
                p >>= 32;
            }
            Misc.asserts(p == 0L, p);
            return (int) q;
        }

        @Override
        public long longValue() {
            Misc.asserts(this.nWords > 0, this.nWords);
            if (this.nWords == 1)
                return ((long) data[0] & 0xffffffffL);
            Misc.asserts(dataInRangeIsZero(2, this.nWords, this));
            Misc.asserts(data[1] >= 0);
            return ((long) (data[1]) << 32) | ((long) data[0] & 0xffffffffL);
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }

        @Override
        public String toString() {
            StringBuffer r = new StringBuffer(30);
            r.append('[');
            int i = Math.min(nWords - 1, data.length - 1);
            if (nWords > data.length) {
                r.append("(" + data.length + "<" + nWords + "!)");
            }
            for (; i > 0; i--) {
                r.append(Integer.toHexString(data[i]));
                r.append(' ');
            }
            r.append(Integer.toHexString(data[0]));
            r.append(']');
            return new String(r);
        }

        @Override
        public BigInt clone() {
            try {
                BigInt other = (BigInt) super.clone();
                other.data = data.clone();
                return other;
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }

        @Override
        public int compareTo(BigInt other) {
            int i;
            if (this.nWords > other.nWords) {
                int j = other.nWords - 1;
                for (i = this.nWords - 1; i > j; i--)
                    if (this.data[i] != 0)
                        return 1;
            } else if (this.nWords < other.nWords) {
                int j = this.nWords - 1;
                for (i = other.nWords - 1; i > j; i--)
                    if (other.data[i] != 0)
                        return -1;
            } else {
                i = this.nWords - 1;
            }
            for (; i > 0; i--)
                if (this.data[i] != other.data[i])
                    break;
            int a = this.data[i];
            int b = other.data[i];
            if (a < 0) {
                if (b < 0) {
                    return a - b;
                } else {
                    return 1;
                }
            } else {
                if (b < 0) {
                    return -1;
                } else {
                    return a - b;
                }
            }
        }
    }
    public static class FloatNumber {
        private boolean isExceptional;
        private boolean isNegative;
        private int decExponent;
        private char digits[];
        private int nDigits;

        private static final long signMask = 0x8000000000000000L;
        private static final long expMask = 0x7ff0000000000000L;
        private static final long fractMask = ~(signMask | expMask);
        private static final int expShift = 52;
        private static final int expBias = 1023;
        private static final long fractHOB = (1L << expShift);
        private static final long expOne = ((long) expBias) << expShift;
        private static final int maxSmallBinExp = 62;
        private static final int minSmallBinExp = -(63 / 3);

        private static final long highbyte = 0xff00000000000000L;
        private static final long lowbytes = ~highbyte;

        private static final int singleSignMask = 0x80000000;
        private static final int singleExpMask = 0x7f800000;
        private static final int singleFractMask = ~(singleSignMask | singleExpMask);
        private static final int singleExpShift = 23;
        private static final int singleFractHOB = 1 << singleExpShift;
        private static final int singleExpBias = 127;

        private static final int small5pow[] = {1, 5, 5 * 5, 5 * 5 * 5, 5 * 5 * 5 * 5,
                5 * 5 * 5 * 5 * 5, 5 * 5 * 5 * 5 * 5 * 5, 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5 * 5 * 5 * 5 * 5 * 5 * 5 * 5, 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5, 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5};


        private static final long long5pow[] = {1L, 5L, 5L * 5, 5L * 5 * 5, 5L * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5, 5L * 5 * 5 * 5 * 5 * 5, 5L * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5, 5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5, 5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
                        * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
                        * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
                        * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
                        * 5 * 5 * 5 * 5,
                5L * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5 * 5
                        * 5 * 5 * 5 * 5 * 5,};

        private static final int n5bits[] = {0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33,
                35, 38, 40, 42, 45, 47, 49, 52, 54, 56, 59, 61,};

        private static final char infinity[] = {'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
        private static final char notANumber[] = {'N', 'a', 'N'};
        private static final char zero[] = {'0', '0', '0', '0', '0', '0', '0', '0'};


        public FloatNumber(double d) {
            long dBits = Double.doubleToLongBits(d);
            long fractBits;
            int binExp;
            int nSignificantBits;

            if ((dBits & signMask) != 0) {
                isNegative = true;
                dBits ^= signMask;
            } else {
                isNegative = false;
            }
            binExp = (int) ((dBits & expMask) >> expShift);
            fractBits = dBits & fractMask;
            if (binExp == (int) (expMask >> expShift)) {
                isExceptional = true;
                if (fractBits == 0L) {
                    digits = infinity;
                } else {
                    digits = notANumber;
                    isNegative = false;
                }
                nDigits = digits.length;
                return;
            }
            isExceptional = false;
            if (binExp == 0) {
                if (fractBits == 0L) {
                    decExponent = 0;
                    digits = zero;
                    nDigits = 1;
                    return;
                }
                while ((fractBits & fractHOB) == 0L) {
                    fractBits <<= 1;
                    binExp -= 1;
                }
                nSignificantBits = expShift + binExp + 1;
                binExp += 1;
            } else {
                fractBits |= fractHOB;
                nSignificantBits = expShift + 1;
            }
            binExp -= expBias;
            dtoa(binExp, fractBits, nSignificantBits);
        }

        public FloatNumber(float f) {
            int fBits = Float.floatToIntBits(f);
            int fractBits;
            int binExp;
            int nSignificantBits;
            if ((fBits & singleSignMask) != 0) {
                isNegative = true;
                fBits ^= singleSignMask;
            } else {
                isNegative = false;
            }
            binExp = (int) ((fBits & singleExpMask) >> singleExpShift);
            fractBits = fBits & singleFractMask;
            if (binExp == (int) (singleExpMask >> singleExpShift)) {
                isExceptional = true;
                if (fractBits == 0L) {
                    digits = infinity;
                } else {
                    digits = notANumber;
                    isNegative = false;
                }
                nDigits = digits.length;
                return;
            }
            isExceptional = false;
            if (binExp == 0) {
                if (fractBits == 0) {
                    decExponent = 0;
                    digits = zero;
                    nDigits = 1;
                    return;
                }
                while ((fractBits & singleFractHOB) == 0) {
                    fractBits <<= 1;
                    binExp -= 1;
                }
                nSignificantBits = singleExpShift + binExp + 1;
                binExp += 1;
            } else {
                fractBits |= singleFractHOB;
                nSignificantBits = singleExpShift + 1;
            }
            binExp -= singleExpBias;
            dtoa(binExp, ((long) fractBits) << (expShift - singleExpShift), nSignificantBits);
        }

        private static ThreadLocal<Object> perThreadBuffer = new ThreadLocal<Object>() {
            protected synchronized Object initialValue() {
                return new char[26];
            }
        };

        public int getChars(char[] result, int off) {
            Misc.asserts(nDigits <= 19, nDigits);
            int i = off;
            if (isNegative) {
                result[off] = '-';
                i++;
            }
            if (isExceptional) {
                System.arraycopy(digits, 0, result, i, nDigits);
                i += nDigits;
            } else {
                if (decExponent > 0 && decExponent < 8) {
                    int charLength = Math.min(nDigits, decExponent);
                    System.arraycopy(digits, 0, result, i, charLength);
                    i += charLength;
                    if (charLength < decExponent) {
                        charLength = decExponent - charLength;
                        System.arraycopy(zero, 0, result, i, charLength);
                        i += charLength;
                        result[i++] = '.';
                        result[i++] = '0';
                    } else {
                        result[i++] = '.';
                        if (charLength < nDigits) {
                            int t = nDigits - charLength;
                            System.arraycopy(digits, charLength, result, i, t);
                            i += t;
                        } else {
                            result[i++] = '0';
                        }
                    }
                } else if (decExponent <= 0 && decExponent > -3) {
                    result[i++] = '0';
                    result[i++] = '.';
                    if (decExponent != 0) {
                        System.arraycopy(zero, 0, result, i, -decExponent);
                        i -= decExponent;
                    }
                    System.arraycopy(digits, 0, result, i, nDigits);
                    i += nDigits;
                } else {
                    result[i++] = digits[0];
                    result[i++] = '.';
                    if (nDigits > 1) {
                        System.arraycopy(digits, 1, result, i, nDigits - 1);
                        i += nDigits - 1;
                    } else {
                        result[i++] = '0';
                    }
                    result[i++] = 'E';
                    int e;
                    if (decExponent <= 0) {
                        result[i++] = '-';
                        e = -decExponent + 1;
                    } else {
                        e = decExponent - 1;
                    }
                    if (e <= 9) {
                        result[i++] = (char) (e + '0');
                    } else if (e <= 99) {
                        result[i++] = (char) (e / 10 + '0');
                        result[i++] = (char) (e % 10 + '0');
                    } else {
                        result[i++] = (char) (e / 100 + '0');
                        e %= 100;
                        result[i++] = (char) (e / 10 + '0');
                        result[i++] = (char) (e % 10 + '0');
                    }
                }
            }
            return i - off;
        }

        private void dtoa(int binExp, long fractBits, int nSignificantBits) {
            int nFractBits;
            int nTinyBits;
            int decExp;
            nFractBits = countBits(fractBits);
            nTinyBits = Math.max(0, nFractBits - binExp - 1);
            if (binExp <= maxSmallBinExp && binExp >= minSmallBinExp) {
                if ((nTinyBits < long5pow.length) && ((nFractBits + n5bits[nTinyBits]) < 64)) {
                    long halfULP;
                    if (nTinyBits == 0) {
                        if (binExp > nSignificantBits) {
                            halfULP = 1L << (binExp - nSignificantBits - 1);
                        } else {
                            halfULP = 0L;
                        }
                        if (binExp >= expShift) {
                            fractBits <<= (binExp - expShift);
                        } else {
                            fractBits >>>= (expShift - binExp);
                        }
                        developLongDigits(0, fractBits, halfULP);
                        return;
                    }
                }
            }
            double d2 = Double.longBitsToDouble(expOne | (fractBits & ~fractHOB));
            decExp = (int) Math.floor(
                    (d2 - 1.5D) * 0.289529654D + 0.176091259 + (double) binExp * 0.301029995663981);
            int B2, B5;
            int S2, S5;
            int M2, M5;
            int Bbits;
            int tenSbits;
            BigInt Sval, Bval, Mval;

            B5 = Math.max(0, -decExp);
            B2 = B5 + nTinyBits + binExp;

            S5 = Math.max(0, decExp);
            S2 = S5 + nTinyBits;

            M5 = B5;
            M2 = B2 - nSignificantBits;
            fractBits >>>= (expShift + 1 - nFractBits);
            B2 -= nFractBits - 1;
            int common2factor = Math.min(B2, S2);
            B2 -= common2factor;
            S2 -= common2factor;
            M2 -= common2factor;
            if (nFractBits == 1)
                M2 -= 1;

            if (M2 < 0) {
                B2 -= M2;
                S2 -= M2;
                M2 = 0;
            }
            char digits[] = this.digits = new char[18];
            int ndigit = 0;
            boolean low, high;
            long lowDigitDifference;
            int q;
            Bbits = nFractBits + B2 + ((B5 < n5bits.length) ? n5bits[B5] : (B5 * 3));
            tenSbits = S2 + 1 + (((S5 + 1) < n5bits.length) ? n5bits[(S5 + 1)] : ((S5 + 1) * 3));
            if (Bbits < 64 && tenSbits < 64) {
                if (Bbits < 32 && tenSbits < 32) {
                    // wa-hoo! They're all ints!
                    int b = ((int) fractBits * small5pow[B5]) << B2;
                    int s = small5pow[S5] << S2;
                    int m = small5pow[M5] << M2;
                    int tens = s * 10;
                    ndigit = 0;
                    q = b / s;
                    b = 10 * (b % s);
                    m *= 10;
                    low = (b < m);
                    high = (b + m > tens);
                    Misc.asserts(q < 10, q);
                    if ((q == 0) && !high) {
                        decExp--;
                    } else {
                        digits[ndigit++] = (char) ('0' + q);
                    }
                    if (decExp < -3 || decExp >= 8) {
                        high = low = false;
                    }
                    while (!low && !high) {
                        q = b / s;
                        b = 10 * (b % s);
                        m *= 10;
                        Misc.asserts(q < 10, q);
                        if (m > 0L) {
                            low = (b < m);
                            high = (b + m > tens);
                        } else {
                            low = true;
                            high = true;
                        }
                        digits[ndigit++] = (char) ('0' + q);
                    }
                    lowDigitDifference = (b << 1) - tens;
                } else {
                    long b = (fractBits * long5pow[B5]) << B2;
                    long s = long5pow[S5] << S2;
                    long m = long5pow[M5] << M2;
                    long tens = s * 10L;
                    ndigit = 0;
                    q = (int) (b / s);
                    b = 10L * (b % s);
                    m *= 10L;
                    low = (b < m);
                    high = (b + m > tens);
                    Misc.asserts(q < 10, q);
                    if ((q == 0) && !high) {
                        decExp--;
                    } else {
                        digits[ndigit++] = (char) ('0' + q);
                    }
                    if (decExp < -3 || decExp >= 8) {
                        high = low = false;
                    }
                    while (!low && !high) {
                        q = (int) (b / s);
                        b = 10 * (b % s);
                        m *= 10;
                        Misc.asserts(q < 10, q);
                        if (m > 0L) {
                            low = (b < m);
                            high = (b + m > tens);
                        } else {
                            low = true;
                            high = true;
                        }
                        digits[ndigit++] = (char) ('0' + q);
                    }
                    lowDigitDifference = (b << 1) - tens;
                }
            } else {
                BigInt tenSval;
                int shiftBias;
                Bval = multPow52(new BigInt(fractBits), B5, B2);
                Sval = constructPow52(S5, S2);
                Mval = constructPow52(M5, M2);
                Bval.lshiftMe(shiftBias = Sval.normalizeMe());
                Mval.lshiftMe(shiftBias);
                tenSval = Sval.mult(10);
                ndigit = 0;
                q = Bval.quoRemIteration(Sval);
                Mval = Mval.mult(10);
                low = (Bval.compareTo(Mval) < 0);
                high = (Bval.add(Mval).compareTo(tenSval) > 0);
                Misc.asserts(q < 10, q);
                if ((q == 0) && !high) {
                    decExp--;
                } else {
                    digits[ndigit++] = (char) ('0' + q);
                }
                if (decExp < -3 || decExp >= 8) {
                    high = low = false;
                }
                while (!low && !high) {
                    q = Bval.quoRemIteration(Sval);
                    Mval = Mval.mult(10);
                    Misc.asserts(q < 10, q);
                    low = (Bval.compareTo(Mval) < 0);
                    high = (Bval.add(Mval).compareTo(tenSval) > 0);
                    digits[ndigit++] = (char) ('0' + q);
                }
                if (high && low) {
                    Bval.lshiftMe(1);
                    lowDigitDifference = Bval.compareTo(tenSval);
                } else
                    lowDigitDifference = 0L;
            }
            this.decExponent = decExp + 1;
            this.digits = digits;
            this.nDigits = ndigit;
            if (high) {
                if (low) {
                    if (lowDigitDifference == 0L) {
                        if ((digits[nDigits - 1] & 1) != 0)
                            roundup();
                    } else if (lowDigitDifference > 0) {
                        roundup();
                    }
                } else {
                    roundup();
                }
            }
        }

        private void roundup() {
            int i;
            int q = digits[i = (nDigits - 1)];
            if (q == '9') {
                while (q == '9' && i > 0) {
                    digits[i] = '0';
                    q = digits[--i];
                }
                if (q == '9') {
                    decExponent += 1;
                    digits[0] = '1';
                    return;
                }
            }
            digits[i] = (char) (q + 1);
        }

        private static int countBits(long v) {
            if (v == 0L)
                return 0;
            while ((v & highbyte) == 0L) {
                v <<= 8;
            }
            while (v > 0L) {
                v <<= 1;
            }
            int n = 0;
            while ((v & lowbytes) != 0L) {
                v <<= 8;
                n += 8;
            }
            while (v != 0L) {
                v <<= 1;
                n += 1;
            }
            return n;
        }

        private void developLongDigits(int decExponent, long lvalue, long insignificant) {
            char digits[];
            int ndigits;
            int digitno;
            int c;
            int i;
            for (i = 0; insignificant >= 10L; i++)
                insignificant /= 10L;
            if (i != 0) {
                long pow10 = long5pow[i] << i;
                long residue = lvalue % pow10;
                lvalue /= pow10;
                decExponent += i;
                if (residue >= (pow10 >> 1)) {
                    lvalue++;
                }
            }
            if (lvalue <= Integer.MAX_VALUE) {
                Misc.asserts(lvalue > 0L, lvalue);
                int ivalue = (int) lvalue;
                ndigits = 10;
                digits = (char[]) (perThreadBuffer.get());
                digitno = ndigits - 1;
                c = ivalue % 10;
                ivalue /= 10;
                while (c == 0) {
                    decExponent++;
                    c = ivalue % 10;
                    ivalue /= 10;
                }
                while (ivalue != 0) {
                    digits[digitno--] = (char) (c + '0');
                    decExponent++;
                    c = ivalue % 10;
                    ivalue /= 10;
                }
                digits[digitno] = (char) (c + '0');
            } else {
                ndigits = 20;
                digits = (char[]) (perThreadBuffer.get());
                digitno = ndigits - 1;
                c = (int) (lvalue % 10L);
                lvalue /= 10L;
                while (c == 0) {
                    decExponent++;
                    c = (int) (lvalue % 10L);
                    lvalue /= 10L;
                }
                while (lvalue != 0L) {
                    digits[digitno--] = (char) (c + '0');
                    decExponent++;
                    c = (int) (lvalue % 10L);
                    lvalue /= 10;
                }
                digits[digitno] = (char) (c + '0');
            }
            char result[];
            ndigits -= digitno;
            result = new char[ndigits];
            System.arraycopy(digits, digitno, result, 0, ndigits);
            this.digits = result;
            this.decExponent = decExponent + 1;
            this.nDigits = ndigits;
        }

        private static BigInt multPow52(BigInt v, int p5, int p2) {
            if (p5 != 0) {
                if (p5 < small5pow.length) {
                    v = v.mult(small5pow[p5]);
                } else {
                    v = v.mult(big5pow(p5));
                }
            }
            if (p2 != 0) {
                v.lshiftMe(p2);
            }
            return v;
        }

        private static BigInt constructPow52(int p5, int p2) {
            BigInt v = big5pow(p5).clone();
            if (p2 != 0) {
                v.lshiftMe(p2);
            }
            return v;
        }

        private static BigInt b5p[];

        private static synchronized BigInt big5pow(int p) {
            Misc.asserts(p >= 0, p);
            if (b5p == null) {
                b5p = new BigInt[p + 1];
            } else if (b5p.length <= p) {
                BigInt t[] = new BigInt[p + 1];
                System.arraycopy(b5p, 0, t, 0, b5p.length);
                b5p = t;
            }
            if (b5p[p] != null)
                return b5p[p];
            else if (p < small5pow.length)
                return b5p[p] = new BigInt(small5pow[p]);
            else if (p < long5pow.length)
                return b5p[p] = new BigInt(long5pow[p]);
            else {
                int q, r;
                q = p >> 1;
                r = p - q;
                BigInt bigq = b5p[q];
                if (bigq == null)
                    bigq = big5pow(q);
                if (r < small5pow.length) {
                    return (b5p[p] = bigq.mult(small5pow[r]));
                } else {
                    BigInt bigr = b5p[r];
                    if (bigr == null)
                        bigr = big5pow(r);
                    return (b5p[p] = bigq.mult(bigr));
                }
            }
        }
    }
}
