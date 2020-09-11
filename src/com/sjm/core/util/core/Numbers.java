package com.sjm.core.util.core;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 数字处理工具
 */
public class Numbers {
    private static final int[] NUMBER_LOCATION = new int[128];

    static {
        Arrays.fill(NUMBER_LOCATION, Integer.MAX_VALUE);
        for (int i = 0; i < 10; i++)
            NUMBER_LOCATION[i + '0'] = i;
        for (int i = 0; i < 26; i++) {
            NUMBER_LOCATION[i + 'a'] = i + 0xa;
            NUMBER_LOCATION[i + 'A'] = i + 0xa;
        }
    }

    private static final String[] MAX_INT_STRINGS = new String[17];
    private static final String[] MIN_INT_STRINGS = new String[17];
    private static final String[] MAX_LONG_STRINGS = new String[17];
    private static final String[] MIN_LONG_STRINGS = new String[17];

    static {
        for (int i = 2; i < 17; i++) {
            MIN_INT_STRINGS[i] = Integer.toString(Integer.MIN_VALUE, i);
            MAX_INT_STRINGS[i] = Integer.toString(Integer.MAX_VALUE, i);
            MIN_LONG_STRINGS[i] = Long.toString(Long.MIN_VALUE, i);
            MAX_LONG_STRINGS[i] = Long.toString(Long.MAX_VALUE, i);
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
                        : (n == Integer.MIN_VALUE ? MIN_INT_STRINGS[radix].length()
                                : log(radix, -n) + 2));
    }

    public static int getBit(long n, int radix) {
        return n > 0 ? log(radix, n) + 1
                : (n == 0 ? 1
                        : (n == Long.MIN_VALUE ? MIN_LONG_STRINGS[radix].length()
                                : log(radix, -n) + 2));
    }

    public static int parseUnsignInt(CharSequence str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        int value = 0;
        for (; begin < end; begin++) {
            int h = NUMBER_LOCATION[str.charAt(begin)];
            if (h >= radix)
                throw new NumberFormatException(Strings.substring(str, begin, end));
            value *= radix;
            value += h;
            if (value < 0 && value != Integer.MIN_VALUE)
                throw new NumberFormatException(Strings.substring(str, begin, end));
        }
        return value;
    }

    public static int parseUnsignInt(char[] str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        int value = 0;
        for (; begin < end; begin++) {
            int h = NUMBER_LOCATION[str[begin]];
            if (h >= radix)
                throw new NumberFormatException(Strings.substring(str, begin, end));
            value *= radix;
            value += h;
            if (value < 0 && value != Integer.MIN_VALUE)
                throw new NumberFormatException(Strings.substring(str, begin, end));
        }
        return value;
    }

    public static long parseUnsignLong(CharSequence str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        long value = 0;
        for (; begin < end; begin++) {
            int h = NUMBER_LOCATION[str.charAt(begin)];
            if (h >= radix)
                throw new NumberFormatException(Strings.substring(str, begin, end));
            value *= radix;
            value += h;
            if (value < 0 && value != Long.MIN_VALUE)
                throw new NumberFormatException(Strings.substring(str, begin, end));
        }
        return value;
    }

    public static long parseUnsignLong(char[] str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        long value = 0;
        for (; begin < end; begin++) {
            int h = NUMBER_LOCATION[str[begin]];
            if (h >= radix)
                throw new NumberFormatException(Strings.substring(str, begin, end));
            value *= radix;
            value += h;
            if (value < 0 && value != Long.MIN_VALUE)
                throw new NumberFormatException(Strings.substring(str, begin, end));
        }
        return value;
    }

    public static int parseInt(CharSequence str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str.charAt(begin);
        if (c == '-')
            return -parseUnsignInt(str, radix, begin + 1, end);
        else if (c == '+')
            return parseUnsignInt(str, radix, begin + 1, end);
        else
            return parseUnsignInt(str, radix, begin, end);
    }

    public static int parseInt(char[] str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str[begin];
        if (c == '-')
            return -parseUnsignInt(str, radix, begin + 1, end);
        else if (c == '+')
            return parseUnsignInt(str, radix, begin + 1, end);
        else
            return parseUnsignInt(str, radix, begin, end);
    }

    public static long parseLong(CharSequence str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str.charAt(begin);
        if (c == '-')
            return -parseUnsignLong(str, radix, begin + 1, end);
        else if (c == '+')
            return parseUnsignLong(str, radix, begin + 1, end);
        else
            return parseUnsignLong(str, radix, begin, end);
    }

    public static long parseLong(char[] str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str[begin];
        if (c == '-')
            return -parseUnsignLong(str, radix, begin + 1, end);
        else if (c == '+')
            return parseUnsignLong(str, radix, begin + 1, end);
        else
            return parseUnsignLong(str, radix, begin, end);
    }

    public static Number parseInteger(CharSequence str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char ch = str.charAt(begin);
        if (ch == '-') {
            int c = Strings.compare(str, begin + 1, end, MIN_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MIN_VALUE;
            else {
                c = Strings.compare(str, begin + 1, end, MIN_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, radix, begin, end);
                else if (c == 0)
                    return Long.MIN_VALUE;
            }
        } else if (ch == '+') {
            int c = Strings.compare(str, begin + 1, end, MAX_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, begin + 1, end, MAX_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, radix, begin, end);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        } else {
            int c = Strings.compare(str, begin, end, MAX_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, begin, end, MAX_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, begin, end, radix);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        }
        return new BigInteger(Strings.substring(str, begin, end), radix);
    }

    public static Number parseInteger(char[] str, int radix, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char ch = str[begin];
        if (ch == '-') {
            int c = Strings.compare(str, begin + 1, end, MIN_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MIN_VALUE;
            else {
                c = Strings.compare(str, begin + 1, end, MIN_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, radix, begin, end);
                else if (c == 0)
                    return Long.MIN_VALUE;
            }
        } else if (ch == '+') {
            int c = Strings.compare(str, begin + 1, end, MAX_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, begin + 1, end, MAX_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, radix, begin, end);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        } else {
            int c = Strings.compare(str, begin, end, MAX_INT_STRINGS[radix], 0, -1);
            if (c < 0)
                return parseInt(str, radix, begin, end);
            else if (c == 0)
                return Integer.MAX_VALUE;
            else {
                c = Strings.compare(str, begin, end, MAX_LONG_STRINGS[radix], 0, -1);
                if (c < 0)
                    return parseLong(str, begin, end, radix);
                else if (c == 0)
                    return Long.MAX_VALUE;
            }
        }
        return new BigInteger(Strings.substring(str, begin, end), radix);
    }

    public static int parseIntWithRadix(CharSequence str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str.charAt(begin);
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (str.charAt(begin) == '0') {
            begin++;
            char f = Strings.toLowcase(str.charAt(begin));
            if (f == 'x') {
                radix = 16;
                begin++;
            } else if (f == 'b') {
                radix = 2;
                begin++;
            }
        }
        int num = parseInt(str, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static int parseIntWithRadix(char[] str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str[begin];
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (str[begin] == '0') {
            begin++;
            char f = Strings.toLowcase(str[begin]);
            if (f == 'x') {
                radix = 16;
                begin++;
            } else if (f == 'b') {
                radix = 2;
                begin++;
            }
        }
        int num = parseInt(str, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static long parseLongWithRadix(CharSequence str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str.charAt(begin);
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (str.charAt(begin) == '0') {
            if (begin + 1 < end) {
                char f = Strings.toLowcase(str.charAt(begin));
                if (f == 'x') {
                    radix = 16;
                    begin += 2;
                } else if (f == 'b') {
                    radix = 2;
                    begin += 2;
                }
            }
        }
        long num = parseLong(str, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static long parseLongWithRadix(char[] str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        char c = str[begin];
        if (c == '+' || c == '-')
            begin++;
        int radix = 10;
        if (str[begin] == '0') {
            if (begin + 1 < end) {
                char f = Strings.toLowcase(str[begin]);
                if (f == 'x') {
                    radix = 16;
                    begin += 2;
                } else if (f == 'b') {
                    radix = 2;
                    begin += 2;
                }
            }
        }
        long num = parseLong(str, radix, begin, end);
        return c == '-' ? -num : num;
    }

    public static Number parseDeclareNumber(CharSequence str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        int e = 0;
        char suffix = 0;
        int point = 0;
        char fh1 = 0, fh2 = 0;
        int x1 = 0, x2 = 0;
        for (int i = begin; i < end; i++) {
            char c = str.charAt(i);
            if (!Strings.isNumber(c)) {
                c = Strings.toLowcase(c);
                if (c == 'x') {
                    if (str.charAt(i - 1) == '0') {
                        if (i == begin + 1 || i == begin + 2) {
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
                    if (e == 0 && i != begin) {
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
                    if (i == begin) {
                        if (fh1 == 0) {
                            fh1 = c;
                            continue;
                        }
                    } else if (Strings.toLowcase(str.charAt(i - 1)) == 'e') {
                        if (fh2 == 0) {
                            fh2 = c;
                            continue;
                        }
                    }
                }
                throw new NumberFormatException(Strings.substring(str, begin, end));
            }
        }
        if (suffix != 0)
            end--;
        Number n;
        if (e == 0) {
            if (point == 0) {
                n = parseLongWithRadix(str, begin, end);
            } else
                n = Double.parseDouble(Strings.substring(str, begin, end));
        } else {
            String s1;
            String s2;
            if (x1 != 0)
                s1 = parseIntWithRadix(str, 0, e) + "";
            else
                s1 = Strings.substring(str, 0, e);
            if (x2 != 0)
                s2 = parseIntWithRadix(str, e + 1, end) + "";
            else
                s2 = Strings.substring(str, e + 1, end);
            n = Double.parseDouble(s1 + "e" + s2);
        }
        if (suffix == 'f') {
            return n.floatValue();
        } else if (suffix == 'l') {
            return n.longValue();
        } else if (point != 0 || e != 0) {
            return n.doubleValue();
        } else
            return n;
    }

    public static Number parseDeclareNumber(char[] str, int begin, int end) {
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str);
        int e = 0;
        char suffix = 0;
        int point = 0;
        char fh1 = 0, fh2 = 0;
        int x1 = 0, x2 = 0;
        for (int i = begin; i < end; i++) {
            char c = str[i];
            if (!Strings.isNumber(c)) {
                c = Strings.toLowcase(c);
                if (c == 'x') {
                    if (str[i - 1] == '0') {
                        if (i == begin + 1 || i == begin + 2) {
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
                    if (e == 0 && i != begin) {
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
                    if (i == begin) {
                        if (fh1 == 0) {
                            fh1 = c;
                            continue;
                        }
                    } else if (Strings.toLowcase(str[i - 1]) == 'e') {
                        if (fh2 == 0) {
                            fh2 = c;
                            continue;
                        }
                    }
                }
                throw new NumberFormatException(Strings.substring(str, begin, end));
            }
        }
        if (suffix != 0)
            end--;
        Number n;
        if (e == 0) {
            if (point == 0) {
                n = parseLongWithRadix(str, begin, end);
            } else
                n = Double.parseDouble(Strings.substring(str, begin, end));
        } else {
            String s1;
            String s2;
            if (x1 != 0)
                s1 = parseIntWithRadix(str, 0, e) + "";
            else
                s1 = Strings.substring(str, 0, e);
            if (x2 != 0)
                s2 = parseIntWithRadix(str, e + 1, end) + "";
            else
                s2 = Strings.substring(str, e + 1, end);
            n = Double.parseDouble(s1 + "e" + s2);
        }
        if (suffix == 'f') {
            return n.floatValue();
        } else if (suffix == 'l') {
            return n.longValue();
        } else if (point != 0 || e != 0) {
            return n.doubleValue();
        } else
            return n;
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
                copyMinNumber(MIN_INT_STRINGS[radix], numbers[0], dst, toLeft, fromRight);
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
                copyMinNumber(MIN_LONG_STRINGS[radix], numbers[0], dst, toLeft, fromRight);
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
}
