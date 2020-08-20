package com.sjm.core.util.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;

public class Strings {
    public static final String LOWCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NUMBER = "0123456789";
    public static final String SPECIAL = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
    public static final String LETTER = LOWCASE + UPCASE;
    public static final String LETTER_NUMBER = LETTER + NUMBER;
    public static final String LETTER_UNDERLINE = LETTER + "_";
    public static final String LETTER_NUMBER_UNDERLINE = LETTER_NUMBER + "_";
    public static final String JAVA_NAMED_PREFIX = LETTER + "_$";
    public static final String JAVA_NAMED_BODY = LETTER_NUMBER + "_$";
    public static final String PRINTABLE = LETTER_NUMBER + SPECIAL + " ";
    public static final String BLANK = " \r\n\t\b\f";

    public static final char[] LOWCASE_HEX_CHARS = "0123456789abcdef".toCharArray();
    public static final char[] UPCASE_HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static final char[] TRUE_CHARS = "true".toCharArray();
    public static final char[] FALSE_CHARS = "false".toCharArray();
    public static final char[] NULL_CHARS = "null".toCharArray();

    public static final char[] BASE64_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static final boolean[] LETTER_MAP = getAsciiMap(LETTER);
    public static final boolean[] LETTER_UNDERLINE_MAP = getAsciiMap(LETTER_UNDERLINE);
    public static final boolean[] LETTER_NUMBER_UNDERLINE_MAP =
            getAsciiMap(LETTER_NUMBER_UNDERLINE);
    public static final boolean[] PRINTABLE_MAP = getAsciiMap(PRINTABLE);
    public static final boolean[] SPECIAL_MAP = getAsciiMap(SPECIAL);

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isLetter(char c) {
        return c < 128 && LETTER_MAP[c];
    }

    public static boolean isLetterUnderline(char c) {
        return c < 128 && LETTER_UNDERLINE_MAP[c];
    }

    public static boolean isLetterNumberUnderline(char c) {
        return c < 128 && LETTER_NUMBER_UNDERLINE_MAP[c];
    }

    public static boolean isPrintable(char c) {
        return c < 128 && PRINTABLE_MAP[c];
    }

    public static boolean isSpecial(char c) {
        return c < 128 && SPECIAL_MAP[c];
    }

    public static boolean isChinese(char c) {
        return c >= 0x4e00 && c <= 0x9fbb || c >= 0xff01 && c <= 0xff20
                || c >= 0x2018 && c <= 0x201d || c >= 0x3001 && c <= 0x3011;
    }

    public static boolean isBlank(char c) {
        return BLANK.indexOf(c) != -1;
    }

    public static boolean isNotBlank(char c) {
        return !isBlank(c);
    }

    public static boolean isUpcase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowcase(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static char toUpcase(char c) {
        return c >= 'a' && c <= 'z' ? (char) (c & (~32)) : c;
    }

    public static char toLowcase(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c | 32) : c;
    }

    public static boolean[] getAsciiMap(String chars) {
        boolean[] cs = new boolean[128];
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (c < 128) {
                cs[c] = true;
            } else
                throw new IllegalArgumentException("'" + c + "'");
        }
        return cs;
    }

    public static boolean equalsIgnoreCase(char c, char ch) {
        return toLowcase(c) == toLowcase(ch);
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
        c = toLowcase(c);
        for (int i = 0; i < len; i++)
            if (c == toLowcase(cs[i]))
                return true;
        return false;
    }

    public static String replace(CharSequence str, IntPredicate target, String replacement) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (target.test(c)) {
                sb.append(replacement);
            } else
                sb.append(c);
        }
        return sb.toString();
    }

    public static List<String> split(String str, IntPredicate separator) {
        List<String> list = new ArrayList<String>();
        int begin = 0;
        while (true) {
            int i = indexOf(str, separator, begin, -1);
            if (i == -1) {
                list.add(str.substring(begin));
                break;
            }
            list.add(str.substring(begin, i));
            begin = i + 1;
        }
        return list;
    }

    public static <T> List<T> splitMapper(String str, String sep,
            Function<? super String, T> mapper) {
        List<T> list = Lists.emptyList();
        if (str != null && !str.isEmpty()) {
            String[] arr = str.split(sep);
            for (String s : arr) {
                s = s.trim();
                if (!s.isEmpty()) {
                    if (list.isEmpty())
                        list = new ArrayList<>();
                    list.add(mapper.apply(s));
                }
            }
        }
        return list;
    }

    public static List<String> toNonEmptyLines(String str) {
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
            return encodeHex(buff);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encodeHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        final char[] H = LOWCASE_HEX_CHARS;
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

    static final byte[] LOCATE_BASE64 = new byte[128];
    static {
        for (int i = 0; i < BASE64_CHARS.length; i++)
            LOCATE_BASE64[BASE64_CHARS[i]] = (byte) i;
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
            dst[dstOff++] = BASE64_CHARS[(v >> 18) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[(v >> 12) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[(v >> 6) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[v & 0x3f];
        }
        if (least == 1) {
            int v = (src[srcOff++] << 16) & 0xff0000;
            dst[dstOff++] = BASE64_CHARS[(v >> 18) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[(v >> 12) & 0x3f];
            dst[dstOff++] = '=';
            dst[dstOff++] = '=';
        } else if (least == 2) {
            int v = (src[srcOff++] << 16) & 0xff0000 | (src[srcOff++] << 8) & 0xff00;
            dst[dstOff++] = BASE64_CHARS[(v >> 18) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[(v >> 12) & 0x3f];
            dst[dstOff++] = BASE64_CHARS[(v >> 6) & 0x3f];
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
            int v = (LOCATE_BASE64[src.charAt(srcOff++)] << 18)
                    | (LOCATE_BASE64[src.charAt(srcOff++)] << 12)
                    | (LOCATE_BASE64[src.charAt(srcOff++)] << 6)
                    | LOCATE_BASE64[src.charAt(srcOff++)];
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
            dst[dstOff++] = (byte) v;
        }
        if (least == 1) {
            int v = (LOCATE_BASE64[src.charAt(srcOff++)] << 18)
                    | (LOCATE_BASE64[src.charAt(srcOff++)] << 12)
                    | (LOCATE_BASE64[src.charAt(srcOff++)] << 6);
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
        } else if (least == 2) {
            int v = (LOCATE_BASE64[src.charAt(srcOff++)] << 18)
                    | (LOCATE_BASE64[src.charAt(srcOff++)] << 12);
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

    public static int decodeBase64(char[] src, int srcOff, int srcLen, byte[] dst, int dstOff,
            boolean onlyCalc) {
        int least = src[srcOff + srcLen - 1] == '=' ? (src[srcOff + srcLen - 2] == '=' ? 2 : 1) : 0;
        int groups = least == 0 ? srcLen / 4 : srcLen / 4 - 1;
        int dstLen = groups * 3 + (3 - least) % 3;
        if (onlyCalc)
            return dstLen;
        for (int i = 0; i < groups; i++) {
            int v = (LOCATE_BASE64[src[srcOff++]] << 18) | (LOCATE_BASE64[src[srcOff++]] << 12)
                    | (LOCATE_BASE64[src[srcOff++]] << 6) | LOCATE_BASE64[src[srcOff++]];
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
            dst[dstOff++] = (byte) v;
        }
        if (least == 1) {
            int v = (LOCATE_BASE64[src[srcOff++]] << 18) | (LOCATE_BASE64[src[srcOff++]] << 12)
                    | (LOCATE_BASE64[src[srcOff++]] << 6);
            dst[dstOff++] = (byte) (v >> 16);
            dst[dstOff++] = (byte) (v >> 8);
        } else if (least == 2) {
            int v = (LOCATE_BASE64[src[srcOff++]] << 18) | (LOCATE_BASE64[src[srcOff++]] << 12);
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

    public static class ByteSequence implements CharSequence {
        private byte[] data;
        private int size;

        public ByteSequence(int cap) {
            data = new byte[cap];
        }

        @Override
        public char charAt(int index) {
            return (char) data[index];
        }

        @Override
        public int length() {
            return size;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new String(data, start, end - start);
        }

        @Override
        public String toString() {
            return toString(Charset.defaultCharset());
        }

        public String toString(Charset charset) {
            return new String(data, 0, size, charset);
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            size = 0;
        }

        public byte[] getLocalBytes() {
            return data;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
            System.arraycopy(data, srcBegin, dst, dstBegin, srcEnd - srcBegin);
        }

        public byte[] toByteArray() {
            return Arrays.copyOfRange(data, 0, size);
        }

        public byte get(int index) {
            return data[index];
        }

        public void put(byte b) {
            resize(1);
            data[size++] = b;
        }

        public void put(byte[] v, int off, int len) {
            resize(len);
            System.arraycopy(v, off, data, size, len);
            size += len;
        }

        public void put(byte[] v) {
            put(v, 0, v.length);
        }

        private void resize(int addSize) {
            int newSize = size + addSize;
            if (newSize > data.length) {
                if (newSize < data.length * 2)
                    newSize = data.length * 2;
                byte[] newBuf = new byte[newSize];
                System.arraycopy(data, 0, newBuf, 0, data.length);
                data = newBuf;
            }
        }
    }

    public static int checkLeft(int left) {
        if (left == -1)
            left = 0;
        return left;
    }

    public static int checkRight(int right, CharSequence str) {
        if (right == -1)
            right = str.length() - 1;
        return right;
    }

    public static int checkRight(int right, char[] str) {
        if (right == -1)
            right = str.length - 1;
        return right;
    }

    public static int checkEnd(int right, CharSequence str) {
        if (right == -1)
            right = str.length();
        return right;
    }

    public static int checkEnd(int right, char[] str) {
        if (right == -1)
            right = str.length;
        return right;
    }

    public static String substring(CharSequence str, int begin, int end) {
        begin = checkLeft(begin);
        end = checkEnd(end, str);
        if (str instanceof String)
            return ((String) str).substring(begin, end);
        else {
            char[] cs = new char[end - begin];
            for (int i = 0; begin < end; i++, begin++)
                cs[i] = str.charAt(begin);
            return new String(cs);
        }
    }

    public static String substring(char[] str, int begin, int end) {
        begin = checkLeft(begin);
        end = checkEnd(end, str);
        return new String(str, begin, end - begin);
    }

    public static String substring(CharSequence str, int from, int to, char start, char end) {
        from = checkLeft(from);
        to = checkRight(to, str);
        int s = start == 0 ? 0 : indexOf(str, start, from, to);
        if (s == -1)
            return null;
        int e = end == 0 ? to + 1 : lastIndexOf(str, end, to, from);
        if (e == -1)
            return null;
        return substring(str, s + 1, e);
    }

    public static String substring(char[] str, int from, int to, char start, char end) {
        from = checkLeft(from);
        to = checkRight(to, str);
        int s = start == 0 ? 0 : indexOf(str, start, from, to);
        if (s == -1)
            return null;
        int e = end == 0 ? to + 1 : lastIndexOf(str, end, to, from);
        if (e == -1)
            return null;
        return substring(str, s + 1, e);
    }

    public static void getChars(CharSequence str, int begin, int end, char[] dst, int dstBegin) {
        begin = checkLeft(begin);
        end = checkEnd(end, str);
        if (str instanceof String)
            ((String) str).getChars(begin, end, dst, dstBegin);
        else {
            for (; begin < end; begin++, dstBegin++)
                dst[dstBegin] = str.charAt(begin);
        }
    }

    public static void getChars(char[] str, int begin, int end, char[] dst, int dstBegin) {
        begin = checkLeft(begin);
        end = checkEnd(end, str);
        System.arraycopy(str, begin, dst, dstBegin, end - begin);
    }

    public static int indexOf(CharSequence str, char c, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++)
            if (str.charAt(from) == c)
                return from;
        return -1;
    }

    public static int indexOf(char[] str, char c, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++)
            if (str[from] == c)
                return from;
        return -1;
    }

    public static int lastIndexOf(CharSequence str, char c, int from, int to) {
        from = checkRight(from, str);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (str.charAt(from) == c)
                return from;
        return -1;
    }

    public static int lastIndexOf(char[] str, char c, int from, int to) {
        from = checkRight(from, str);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (str[from] == c)
                return from;
        return -1;
    }

    public static int indexOf(CharSequence str, IntPredicate p, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++)
            if (p.test(str.charAt(from)))
                return from;
        return -1;
    }

    public static int indexOf(char[] str, IntPredicate p, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++)
            if (p.test(str[from]))
                return from;
        return -1;
    }

    public static int lastIndexOf(CharSequence str, IntPredicate p, int from, int to) {
        from = checkRight(from, str);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (p.test(str.charAt(from)))
                return from;
        return -1;
    }

    public static int lastIndexOf(char[] str, IntPredicate p, int from, int to) {
        from = checkRight(from, str);
        to = checkLeft(to);
        for (; from >= to; from--)
            if (p.test(str[from]))
                return from;
        return -1;
    }

    public static int compare(CharSequence str1, int begin1, int end1, CharSequence str2,
            int begin2, int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = str1.charAt(begin1);
            char c2 = str2.charAt(begin2);
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static int compare(char[] str1, int begin1, int end1, CharSequence str2, int begin2,
            int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = str1[begin1];
            char c2 = str2.charAt(begin2);
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static int compare(char[] str1, int begin1, int end1, char[] str2, int begin2,
            int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = str1[begin1];
            char c2 = str2[begin2];
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static int compareIgnoreCase(CharSequence str1, int begin1, int end1, CharSequence str2,
            int begin2, int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = toLowcase(str1.charAt(begin1));
            char c2 = toLowcase(str2.charAt(begin2));
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static int compareIgnoreCase(char[] str1, int begin1, int end1, CharSequence str2,
            int begin2, int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = toLowcase(str1[begin1]);
            char c2 = toLowcase(str2.charAt(begin2));
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static int compareIgnoreCase(char[] str1, int begin1, int end1, char[] str2, int begin2,
            int end2) {
        begin1 = checkLeft(begin1);
        end1 = checkEnd(end1, str1);
        begin2 = checkLeft(begin2);
        end2 = checkEnd(end2, str2);
        int len1 = end1 - begin1, len2 = end2 - begin2;
        if (len1 != len2)
            return len1 - len2;
        for (; begin1 < end1; begin1++, begin2++) {
            char c1 = toLowcase(str1[begin1]);
            char c2 = toLowcase(str2[begin2]);
            if (c1 != c2)
                return c1 - c2;
        }
        return 0;
    }

    public static void hexdump(InputStream is) throws IOException {
        byte[] b = new byte[16];
        int offset = 0, n;
        MyStringBuilder sb = new MyStringBuilder();
        while ((n = is.read(b)) != -1) {
            sb.append("0x").append(offset, Strings.LOWCASE_HEX_CHARS, 16, 8).append(":  ");
            for (int i = 0; i < n; i++)
                sb.append(b[i] & 0xff, Strings.LOWCASE_HEX_CHARS, 16, 2).append(" ");
            for (int i = n; i < 16; i++)
                sb.append("   ");
            sb.append("  ");
            for (int i = 0; i < n; i++) {
                char c = (char) b[i];
                if (!Strings.isPrintable(c))
                    c = '.';
                sb.append(c);
            }
            System.out.println(sb);
            sb.clear();
            offset += n;
        }
    }

    public static void hexdump(byte[] bytes) {
        try {
            hexdump(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new Error();
        }
    }

    public static int indexOfIgnoreEscape(CharSequence str, char c, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++) {
            char ch = str.charAt(from);
            if (ch == '\\') {
                from++;
                continue;
            }
            if (ch == c)
                return from;
        }
        return -1;
    }

    public static int indexOfRightQuotation(CharSequence str, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        char yh = str.charAt(from);
        if (yh != '\"' && yh != '\'')
            throw new IllegalArgumentException();
        int end = indexOfIgnoreEscape(str, yh, from + 1, to);
        if (end == -1)
            return -1;
        return end;
    }

    public static int indexOfIgnoreQuotation(CharSequence str, char c, int from, int to) {
        from = checkLeft(from);
        to = checkRight(to, str);
        for (; from <= to; from++) {
            char ch = str.charAt(from);
            if (ch == '\'' || ch == '\"') {
                from = indexOfRightQuotation(str, from, to);
                if (from == -1)
                    break;
                ch = str.charAt(from);
            }
            if (ch == c)
                return from;
        }
        return -1;
    }
}
