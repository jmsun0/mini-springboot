package com.sjm.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Strings {
    public static final String LowCaseChars = "abcdefghijklmnopqrstuvwxyz";
    public static final String UpCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NumberChars = "0123456789";
    public static final String SpecialChars = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
    public static final String NormalChars = LowCaseChars + UpCaseChars;
    public static final String CharAndNumber = NormalChars + NumberChars;
    public static final String NamedChars = CharAndNumber + "_$";
    public static final String VisibleChars = CharAndNumber + SpecialChars + " ";
    public static final String BlankChars = " \t\r\n\b\f";

    public static final char[] LowCaseNumberChars = "0123456789abcdef".toCharArray();
    public static final char[] UpCaseNumberChars = "0123456789ABCDEF".toCharArray();

    public static final char[] TrueChars = "true".toCharArray();
    public static final char[] FalseChars = "false".toCharArray();
    public static final char[] NullChars = "null".toCharArray();

    public static final char[] Base64Chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static boolean isCharLine(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    public static boolean isNumberCharLine(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    public static boolean isVisibleChar(char c) {
        return Filters.IsVisible.accept(c);
    }

    public static boolean isSpecialChar(char c) {
        return Filters.IsSpecial.accept(c);
    }

    public static boolean isChinese(char c) {
        return c >= 0x4e00 && c <= 0x9fbb || c >= 0xff01 && c <= 0xff20
                || c >= 0x2018 && c <= 0x201d || c >= 0x3001 && c <= 0x3011;
    }

    public static boolean isBlank(char c) {
        return BlankChars.indexOf(c) != -1;
    }

    public static boolean isNotBlank(char c) {
        return !isBlank(c);
    }

    public static boolean isUpCase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowCase(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static char toUpCase(char c) {
        return c >= 'a' && c <= 'z' ? (char) (c & (~32)) : c;
    }

    public static char toLowCase(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c | 32) : c;
    }

    public static boolean equalsIgnoreCase(char c, char ch) {
        return toLowCase(c) == toLowCase(ch);
    }

    public static boolean equals(char c, char... cs) {
        int len = cs.length;
        for (int i = 0; i < len; i++)
            if (c == cs[i])
                return true;
        return false;
    }

    public static boolean equalsIgnoreCase(char c, char... cs) {
        int len = cs.length;
        c = toLowCase(c);
        for (int i = 0; i < len; i++)
            if (c == toLowCase(cs[i]))
                return true;
        return false;
    }

    public static String replace(CharSequence str, Filter<Character> f, String r) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (f.accept(c)) {
                sb.append(r);
            } else
                sb.append(c);
        }
        return sb.toString();
    }

    public static List<String> split(String str, Filter<Character> f) {
        List<String> list = new ArrayList<String>();
        int begin = 0;
        while (true) {
            int i = indexOf(str, null, f, begin, -1);
            if (i == -1) {
                list.add(str.substring(begin));
                break;
            }
            list.add(str.substring(begin, i));
            begin = i + 1;
        }
        return list;
    }

    public static List<String> toLines(String str) {
        List<String> list = new ArrayList<>();
        for (int index = 0, len = str.length(); index < len;) {
            int n = str.indexOf('\n', index);
            if (n == -1)
                n = len;
            String line = str.substring(index, n).trim();
            if (!line.isEmpty())
                list.add(line);
            index = n + 1;
        }
        return list;
    }

    public static String MD5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] input = message.getBytes();
            byte[] buff = md.digest(input);
            return bytesToHex(buff);
        } catch (Exception e) {
            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        final char[] H = LowCaseNumberChars;
        for (byte b : bytes) {
            int n = b & 0xff;
            sb.append(H[n / 16]).append(H[n % 16]);
        }
        return sb.toString();
    }

    public static String decodeURL(String str, String charset) {
        try {
            return URLDecoder.decode(str, charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String encodeURL(String str, String charset) {
        try {
            return URLEncoder.encode(str, charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    static final byte[] LocateBase64 = new byte[128];

    static {
        for (int i = 0; i < Base64Chars.length; i++)
            LocateBase64[Base64Chars[i]] = (byte) i;
    }

    public static int encodeBase64(byte[] src, int srcOff, int srcLen, char[] dst, int dstOff,
            boolean onlyCalc) {
        int least = srcLen % 3, groups = srcLen / 3;
        int dstLen = (least == 0 ? groups : groups + 1) * 4;
        if (onlyCalc)
            return dstLen;
        for (int i = 0; i < groups; i++) {
            int v = (src[srcOff++] << 16) & 0xff0000 | (src[srcOff++] << 8) & 0xff00
                    | src[srcOff++] & 0xff;
            dst[dstOff++] = Base64Chars[(v >> 18) & 0x3f];
            dst[dstOff++] = Base64Chars[(v >> 12) & 0x3f];
            dst[dstOff++] = Base64Chars[(v >> 6) & 0x3f];
            dst[dstOff++] = Base64Chars[v & 0x3f];
        }
        if (least == 1) {
            int v = (src[srcOff++] << 16) & 0xff0000;
            dst[dstOff++] = Base64Chars[(v >> 18) & 0x3f];
            dst[dstOff++] = Base64Chars[(v >> 12) & 0x3f];
            dst[dstOff++] = '=';
            dst[dstOff++] = '=';
        } else if (least == 2) {
            int v = (src[srcOff++] << 16) & 0xff0000 | (src[srcOff++] << 8) & 0xff00;
            dst[dstOff++] = Base64Chars[(v >> 18) & 0x3f];
            dst[dstOff++] = Base64Chars[(v >> 12) & 0x3f];
            dst[dstOff++] = Base64Chars[(v >> 6) & 0x3f];
            dst[dstOff++] = '=';
        }
        return dstLen;
    }

    public static char[] encodeBase64(byte[] src, int srcOff, int srcLen) {
        char[] dst = new char[encodeBase64(src, srcOff, srcLen, null, 0, true)];
        encodeBase64(src, srcOff, srcLen, dst, 0, false);
        return dst;
    }

    public static char[] encodeBase64(byte[] src) {
        return encodeBase64(src, 0, src.length);
    }

    public static int decodeBase64(char[] src, int srcOff, int srcLen, byte[] dst, int dstOff,
            boolean onlyCalc) {
        int least = src[srcOff + srcLen - 1] == '=' ? (src[srcOff + srcLen - 2] == '=' ? 2 : 1) : 0;
        int groups = least == 0 ? srcLen / 4 : srcLen / 4 - 1;
        int dstLen = groups * 3 + (3 - least) % 3;
        if (onlyCalc)
            return dstLen;
        for (int i = 0; i < groups; i++) {
            int v = (LocateBase64[src[srcOff++]] << 18) | (LocateBase64[src[srcOff++]] << 12)
                    | (LocateBase64[src[srcOff++]] << 6) | LocateBase64[src[srcOff++]];
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
            dst[dstOff++] = (byte) v;
        }
        if (least == 1) {
            int v = (LocateBase64[src[srcOff++]] << 18) | (LocateBase64[src[srcOff++]] << 12)
                    | (LocateBase64[src[srcOff++]] << 6);
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
        } else if (least == 2) {
            int v = (LocateBase64[src[srcOff++]] << 18) | (LocateBase64[src[srcOff++]] << 12);
            dst[dstOff++] = (byte) (v >> 16);
        }
        return dstLen;
    }

    public static byte[] decodeBase64(char[] src, int srcOff, int srcLen) {
        byte[] dst = new byte[decodeBase64(src, srcOff, srcLen, null, 0, true)];
        decodeBase64(src, srcOff, srcLen, dst, 0, false);
        return dst;
    }

    public static byte[] decodeBase64(char[] src) {
        return decodeBase64(src, 0, src.length);
    }

    public static int decodeBase64(CharSequence src, int srcOff, int srcLen, byte[] dst, int dstOff,
            boolean onlyCalc) {
        int least = src.charAt(srcOff + srcLen - 1) == '='
                ? (src.charAt(srcOff + srcLen - 2) == '=' ? 2 : 1)
                : 0;
        int groups = least == 0 ? srcLen / 4 : srcLen / 4 - 1;
        int dstLen = groups * 3 + (3 - least) % 3;
        if (onlyCalc)
            return dstLen;
        for (int i = 0; i < groups; i++) {
            int v = (LocateBase64[src.charAt(srcOff++)] << 18)
                    | (LocateBase64[src.charAt(srcOff++)] << 12)
                    | (LocateBase64[src.charAt(srcOff++)] << 6)
                    | LocateBase64[src.charAt(srcOff++)];
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
            dst[dstOff++] = (byte) v;
        }
        if (least == 1) {
            int v = (LocateBase64[src.charAt(srcOff++)] << 18)
                    | (LocateBase64[src.charAt(srcOff++)] << 12)
                    | (LocateBase64[src.charAt(srcOff++)] << 6);
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
        } else if (least == 2) {
            int v = (LocateBase64[src.charAt(srcOff++)] << 18)
                    | (LocateBase64[src.charAt(srcOff++)] << 12);
            dst[dstOff++] = (byte) (v >> 16);
        }
        return dstLen;
    }

    public static byte[] decodeBase64(CharSequence src, int srcOff, int srcLen) {
        byte[] dst = new byte[decodeBase64(src, srcOff, srcLen, null, 0, true)];
        decodeBase64(src, srcOff, srcLen, dst, 0, false);
        return dst;

    }

    public static byte[] decodeBase64(CharSequence src) {
        return decodeBase64(src, 0, src.length());
    }

    public static <T> String combine(Iterable<T> data, String separator) {
        return new MyStringBuilder().appends(data, separator).toString();
    }

    public static <T> String combine(Collection<T> data, String separator) {
        return new MyStringBuilder().appends(data, separator).toString();
    }

    public static <T> String combine(List<T> data, String separator) {
        return new MyStringBuilder().appends(data, separator).toString();
    }

    public static <T> String combine(T[] data, String separator) {
        return new MyStringBuilder().appends(data, separator).toString();
    }

    public static <T> String combine(Object data, String separator) {
        return new MyStringBuilder().appends(data, separator).toString();
    }

    public static abstract class StringController<S> {
        public abstract char get(S str, int index);

        public abstract int getLength(S str);

        public String toString(S str, int begin, int end) {
            if (end == -1)
                end = getLength(str);
            char[] cs = new char[end - begin];
            for (int i = 0; begin < end; i++, begin++)
                cs[i] = get(str, begin);
            return new String(cs);
        }

        public void getChars(S str, int begin, int end, char[] dst, int dstBegin) {
            if (end == -1)
                end = getLength(str);
            for (; begin < end; begin++, dstBegin++)
                dst[dstBegin] = get(str, begin);
        }
    }

    public static final StringController<String> StringCtr = new StringController<String>() {
        @Override
        public int getLength(String str) {
            return str.length();
        }

        @Override
        public char get(String str, int index) {
            return str.charAt(index);
        }

        @Override
        public String toString(String str, int begin, int end) {
            return str.substring(begin, end);
        }

        @Override
        public void getChars(String str, int begin, int end, char[] dst, int dstBegin) {
            str.getChars(begin, end, dst, dstBegin);
        }
    };
    public static final StringController<CharSequence> CharSequenceCtr =
            new StringController<CharSequence>() {
                @Override
                public int getLength(CharSequence str) {
                    return str.length();
                }

                @Override
                public char get(CharSequence str, int index) {
                    return str.charAt(index);
                }
            };
    public static final StringController<char[]> CharArrayCtr = new StringController<char[]>() {
        @Override
        public int getLength(char[] str) {
            return str.length;
        }

        @Override
        public char get(char[] str, int index) {
            return str[index];
        }

        @Override
        public String toString(char[] str, int begin, int end) {
            return new String(str, begin, end - begin);
        }

        @Override
        public void getChars(char[] str, int begin, int end, char[] dst, int dstBegin) {
            System.arraycopy(str, begin, dst, dstBegin, end - begin);
        }
    };
    public static final StringController<byte[]> ByteArrayCtr = new StringController<byte[]>() {
        @Override
        public int getLength(byte[] str) {
            return str.length;
        }

        @Override
        public char get(byte[] str, int index) {
            return (char) str[index];
        }
    };
    public static final StringController<int[]> IntArrayCtr = new StringController<int[]>() {
        @Override
        public int getLength(int[] str) {
            return str.length;
        }

        @Override
        public char get(int[] str, int index) {
            return (char) str[index];
        }
    };

    @SuppressWarnings("unchecked")
    public static <S> StringController<S> getStringController(S str) {
        if (str instanceof String)
            return (StringController<S>) StringCtr;
        else if (str instanceof char[])
            return (StringController<S>) CharArrayCtr;
        else if (str instanceof CharSequence)
            return (StringController<S>) CharSequenceCtr;
        else if (str instanceof byte[])
            return (StringController<S>) ByteArrayCtr;
        else if (str instanceof int[])
            return (StringController<S>) IntArrayCtr;
        else
            throw new IllegalArgumentException();
    }

    public static <S> StringController<S> checkController(StringController<S> ctr, S str) {
        if (ctr == null)
            ctr = getStringController(str);
        return ctr;
    }

    public static int checkLeft(int left) {
        if (left == -1)
            left = 0;
        return left;
    }

    public static <S> int checkRight(int right, S str, StringController<S> ctr) {
        if (right == -1)
            right = ctr.getLength(str) - 1;
        return right;
    }

    public static <S> int checkEnd(int right, S str, StringController<S> ctr) {
        if (right == -1)
            right = ctr.getLength(str);
        return right;
    }

    public static <S> int indexOf(S str, StringController<S> ctr, char c, int from, int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        for (; from <= to; from++)
            if (ctr.get(str, from) == c)
                return from;
        return -1;
    }

    public static <S> int lastIndexOf(S str, StringController<S> ctr, char c, int from, int to) {
        ctr = checkController(ctr, str);
        from = checkRight(from, str, ctr);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (ctr.get(str, from) == c)
                return from;
        return -1;
    }

    public static <S> int indexOf(S str, StringController<S> ctr, Filter<Character> f, int from,
            int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        for (; from <= to; from++)
            if (f.accept(ctr.get(str, from)))
                return from;
        return -1;
    }

    public static <S> int lastIndexOf(S str, StringController<S> ctr, Filter<Character> f, int from,
            int to) {
        ctr = checkController(ctr, str);
        from = checkRight(from, str, ctr);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (f.accept(ctr.get(str, from)))
                return from;
        return -1;
    }

    public static <S> int indexOfSkip(S str, StringController<S> ctr, Filter<Character> f, int skip,
            int from, int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        for (int i = 0;;) {
            from = indexOf(str, ctr, f, from, to);
            if (from == -1)
                return -1;
            i++;
            if (i == skip)
                return from;
            from++;
        }
    }

    public static <S> int lastIndexOfSkip(S str, StringController<S> ctr, Filter<Character> f,
            int skip, int from, int to) {
        ctr = checkController(ctr, str);
        from = checkRight(from, str, ctr);
        to = checkLeft(to);
        for (int i = 0;;) {
            from = lastIndexOf(str, ctr, f, from, to);
            if (from == -1)
                return -1;
            i++;
            if (i == skip)
                return from;
            from--;
        }
    }

    public static <S> int indexOfIgnoreEscape(S str, StringController<S> ctr, char c, int from,
            int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        for (; from <= to; from++) {
            char ch = ctr.get(str, from);
            if (ch == '\\') {
                from++;
                continue;
            }
            if (ch == c)
                return from;
        }
        return -1;
    }


    public static <S> int indexOfRightQuotation(S str, StringController<S> ctr, int from, int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        char yh = ctr.get(str, from);
        if (yh != '\"' && yh != '\'')
            throw new IllegalArgumentException();
        int end = indexOfIgnoreEscape(str, ctr, yh, from + 1, to);
        if (end == -1)
            return -1;
        return end;
    }

    public static <S> int indexOfRightBrackets(S str, StringController<S> ctr, int from, int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        char left = ctr.get(str, from++);
        char right = getRightBrackets(left);
        for (int n = 1; from <= to; from++) {
            char c = ctr.get(str, from);
            if (c == left)
                n++;
            else if (c == right)
                n--;
            else if (c == '\"' || c == '\'') {
                from = indexOfRightQuotation(str, ctr, from, to);
                if (from == -1)
                    break;
                continue;
            }
            if (n == 0)
                return from;
        }
        return -1;
    }

    public static char getRightBrackets(char left) {
        switch (left) {
            case '(':
                return ')';
            case '{':
                return '}';
            case '[':
                return ']';
            case '<':
                return '>';
            default:
                throw new IllegalArgumentException();
        }
    }

    public static <S> int indexOf(S str, StringController<S> ctr, int from, int to, S substr,
            StringController<S> subctr, int subfrom, int subto) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        subctr = checkController(subctr, substr);
        subfrom = checkLeft(subfrom);
        subto = checkRight(subto, substr, subctr);
        char first = subctr.get(substr, subfrom);
        for (; from <= to; from++) {
            char ch = ctr.get(str, from);
            if (ch == first && compare(str, ctr, from, from + subto + 1 - subfrom, substr, subctr,
                    subfrom, subto + 1) == 0)
                return from;
        }
        return -1;
    }

    public static <S1, S2> int compare(S1 str1, StringController<S1> ctr1, int begin1, int end1,
            S2 str2, StringController<S2> ctr2, int begin2, int end2) {
        ctr1 = checkController(ctr1, str1);
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1, ctr1);
        ctr2 = checkController(ctr2, str2);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2, ctr2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = ctr1.get(str1, begin1);
            char c2 = ctr2.get(str2, begin2);
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static <S1, S2> int compareIgnoreCase(S1 str1, StringController<S1> ctr1, int begin1,
            int end1, S2 str2, StringController<S2> ctr2, int begin2, int end2) {
        ctr1 = checkController(ctr1, str1);
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1, ctr1);
        ctr2 = checkController(ctr2, str2);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2, ctr2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = toLowCase(ctr1.get(str1, begin1));
            char c2 = toLowCase(ctr2.get(str2, begin2));
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static <S1, S2> boolean startsWith(S1 str, StringController<S1> strCtr, S2 prefix,
            StringController<S2> prefixCtr) {
        prefixCtr = checkController(prefixCtr, prefix);
        int prefixLen = prefixCtr.getLength(prefix);
        return compare(str, strCtr, 0, prefixLen, prefix, prefixCtr, 0, prefixLen) == 0;
    }

    public static <S1, S2> boolean endWith(S1 str, StringController<S1> strCtr, S2 suffix,
            StringController<S2> suffixCtr) {
        strCtr = checkController(strCtr, str);
        suffixCtr = checkController(suffixCtr, suffix);
        int strLen = strCtr.getLength(str);
        int suffixLen = suffixCtr.getLength(suffix);
        return compare(str, strCtr, strLen - suffixLen, strLen, suffix, suffixCtr, 0,
                suffixLen) == 0;
    }

    public static <S1, S2> boolean startsWithIgnoreCase(S1 str, StringController<S1> strCtr,
            S2 prefix, StringController<S2> prefixCtr) {
        prefixCtr = checkController(prefixCtr, prefix);
        int prefixLen = prefixCtr.getLength(prefix);
        return compareIgnoreCase(str, strCtr, 0, prefixLen, prefix, prefixCtr, 0, prefixLen) == 0;
    }

    public static <S1, S2> boolean endWithIgnoreCase(S1 str, StringController<S1> strCtr, S2 suffix,
            StringController<S2> suffixCtr) {
        strCtr = checkController(strCtr, str);
        suffixCtr = checkController(suffixCtr, suffix);
        int strLen = strCtr.getLength(str);
        int suffixLen = suffixCtr.getLength(suffix);
        return compareIgnoreCase(str, strCtr, strLen - suffixLen, strLen, suffix, suffixCtr, 0,
                suffixLen) == 0;
    }

    public static void print(InputStream is) throws IOException {
        byte[] b = new byte[16];
        int offset = 0, n;
        MyStringBuilder sb = new MyStringBuilder();
        while ((n = is.read(b)) != -1) {
            sb.append("0x").append(offset, Strings.LowCaseNumberChars, 16, 8).append(":  ");
            for (int i = 0; i < n; i++)
                sb.append(b[i] & 0xff, Strings.LowCaseNumberChars, 16, 2).append(" ");
            for (int i = n; i < 16; i++)
                sb.append("   ");
            sb.append("  ");
            for (int i = 0; i < n; i++) {
                char c = (char) b[i];
                if (!Strings.isVisibleChar(c))
                    c = '.';
                sb.append(c);
            }
            System.out.println(sb);
            sb.clear();
            offset += n;
        }
    }

    public static void print(byte[] bytes) {
        try {
            print(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new Error();
        }
    }

    public static int nextChar(CharSequence str, Misc.IntBox ib) {
        return ib.value < str.length() ? str.charAt(ib.value++) : -1;
    }

    public static int nextUnEscapeChar(CharSequence str, Misc.IntBox ib) {
        int len = str.length();
        if (ib.value >= len)
            return -1;
        char c = str.charAt(ib.value++);
        if (c == '\\') {
            c = str.charAt(ib.value++);
            switch (c) {
                case 'r':
                    return '\r';
                case 'n':
                    return '\n';
                case 'f':
                    return '\f';
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'u':
                    return (char) Numbers.parseInt(str, null, 16, ib.value, ib.value += 4);
                default:
                    if (c >= '0' && c < '8') {
                        int begin = ib.value - 1;
                        if (ib.value < len && (c = str.charAt(ib.value)) >= '0' && c < '8'
                                && ++ib.value < len && (c = str.charAt(ib.value)) >= '0' && c < '8')
                            ib.value++;
                        return (char) Numbers.parseInt(str, null, 8, begin, ib.value);
                    }
            }
        }
        return c;
    }

    public static CharSequence unEscape(CharSequence str) {
        int len = str.length();
        char c1 = str.charAt(0), c2 = str.charAt(len - 1);
        if (c1 != c2 || c1 != '\'' && c1 != '\"')
            throw new IllegalArgumentException();
        return new MyStringBuilder().appendUnEscape(str, null, 1, len - 1);
    }

    public static <S> int count(S str, StringController<S> ctr, int from, int to,
            Filter<Character> f) {
        if (ctr == null)
            ctr = getStringController(str);
        if (from == -1)
            from = 0;
        if (to == -1)
            to = ctr.getLength(str) - 1;
        int n = 0;
        while (true) {
            int index = indexOf(str, ctr, f, from, to);
            if (index == -1)
                break;
            from = index + 1;
            n++;
        }
        return n;
    }

    public static int count(CharSequence str, int from, int to, Filter<Character> f) {
        return count(str, CharSequenceCtr, from, to, f);
    }

    public static String format(String fmt, Object map, Maps.MapGetter getter) {
        MyStringBuilder sb = new MyStringBuilder();
        int off = 0;
        while (true) {
            int index = fmt.indexOf('$', off);
            if (index == -1) {
                sb.append(fmt, off, fmt.length() - off);
                break;
            }
            sb.append(fmt, off, index - off);
            off = index;
            Object value = null;
            index++;
            if (index < fmt.length() && fmt.charAt(index) == '{') {
                int left = ++index;
                index = fmt.indexOf('}', left);
                if (index == -1) {
                    sb.append(fmt, off, fmt.length() - off);
                    break;
                } else {
                    value = getter.get(map, fmt.substring(left, index));
                    index++;
                }
            }
            if (value == null) {
                sb.append(fmt, off, index - off);
            } else {
                sb.append(value);
            }
            off = index;
        }
        return sb.toString();
    }

    public static String format(String fmt, Object map) {
        return format(fmt, map, Maps.GENERAL_MAP_GETTER);
    }

    public static <S> String substring(S str, StringController<S> ctr, int from, int to, char start,
            char end) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        int s = indexOf(str, ctr, start, from, to);
        if (s == -1)
            return null;
        int e = lastIndexOf(str, ctr, end, to, from);
        if (e == -1)
            return null;
        return ctr.toString(str, s + 1, e);
    }

    public static <S> int indexOfIgnoreQuotation(S str, StringController<S> ctr, char c, int from,
            int to) {
        ctr = checkController(ctr, str);
        from = checkLeft(from);
        to = checkRight(to, str, ctr);
        for (; from <= to; from++) {
            char ch = ctr.get(str, from);
            if (ch == '\'' || ch == '\"') {
                from = indexOfRightQuotation(str, ctr, from, to);
                if (from == -1)
                    break;
                ch = ctr.get(str, from);
            }
            if (ch == c)
                return from;
        }
        return -1;
    }

    /**
     * 方法描述:数字转字母 1-26 ： a-z A-Z
     * 
     * @param num 转换的数据
     * @param caseLetter 大小写区分,需要大写传任意大写字母;小写相反
     * @return
     */
    public static String numberToLetter(int num, char caseLetter) {
        if (num <= 0) {
            return null;
        }
        String letter = "";
        num--;
        if (letter.length() > 0) {
            num--;
        }
        if (Character.isUpperCase(caseLetter)) {
            letter = ((char) (num % 26 + (int) 'A')) + letter;
        } else {
            letter = ((char) (num % 26 + (int) 'a')) + letter;
        }
        return letter;
    }

    /**
     * 方法描述:字母转数字 A-Z a-z ：1-26
     * 
     * @param letter
     * @return
     */
    public static int letterToNumber(char letter) {
        int number = 0;
        if (Character.isUpperCase(letter)) {
            number = (int) (letter - 'A' + 1);
        } else {
            number = (int) (letter - 'a' + 1);
        }
        return number;
    }

    public static <T> List<T> split(String str, String sep, Converter<T, ? super String> conv) {
        List<T> list = Lists.emptyList();
        if (str != null && !str.isEmpty()) {
            String[] arr = str.split(sep);
            for (String s : arr) {
                s = s.trim();
                if (!s.isEmpty()) {
                    if (list.isEmpty())
                        list = new ArrayList<>();
                    list.add(conv.convert(s));
                }
            }
        }
        return list;
    }

    public static <T> List<T> split(String str, String sep, Class<T> clazz) {
        return split(str, sep, Converters.valueOf(clazz));
    }
}
