package com.sjm.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public class MyStringBuilder implements CharSequence, Cloneable {
    protected char[] buf;
    protected int size;

    public MyStringBuilder(int cap) {
        buf = new char[cap];
    }

    public MyStringBuilder() {
        this(16);
    }

    public MyStringBuilder(Object str) {
        this();
        append(str);
    }

    @Override
    public char charAt(int index) {
        return buf[index];
    }

    @Override
    public int length() {
        return size;
    }

    @Override
    public CharSequence subSequence(int begin, int end) {
        return new String(buf, begin, end - begin);
    }

    @Override
    public String toString() {
        return new String(buf, 0, size);
    }

    @Override
    public MyStringBuilder clone() {
        try {
            MyStringBuilder sb = (MyStringBuilder) super.clone();
            sb.buf = buf.clone();
            return sb;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public char[] getLocalChars() {
        return buf;
    }

    public void setLength(int size) {
        this.size = size;
    }

    public char lastCharAt(int index) {
        return buf[size - 1 - index];
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        System.arraycopy(buf, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public char[] toCharArray() {
        return Arrays.copyOfRange(buf, 0, size);
    }

    public MyStringBuilder append(char[] v, int off, int len) {
        resize(len);
        System.arraycopy(v, off, buf, size, len);
        size += len;
        return this;
    }

    public MyStringBuilder append(char[] v) {
        return append(v, 0, v.length);
    }

    public MyStringBuilder append(String v, int off, int len) {
        resize(len);
        v.getChars(off, off + len, buf, size);
        size += len;
        return this;
    }

    public MyStringBuilder append(String v) {
        return append(v, 0, v.length());
    }

    public MyStringBuilder append(MyStringBuilder v) {
        return append(v.getLocalChars(), 0, v.length());
    }

    public <S> MyStringBuilder append(S str, Strings.StringController<S> ctr, int begin, int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        int len = end - begin;
        resize(len);
        ctr.getChars(str, begin, end, buf, size);
        size += len;
        return this;
    }

    public MyStringBuilder append(int v, char[] numbers, int radix, int len) {
        resize(len);
        Numbers.getChars(v, numbers, radix, buf, size, size + len - 1);
        size += len;
        return this;
    }

    public MyStringBuilder append(int v, int radix) {
        return append(v, Strings.LowCaseNumberChars, radix, Numbers.getBit(v, radix));
    }

    public MyStringBuilder append(int v) {
        return append(v, 10);
    }

    public MyStringBuilder append(long v, char[] numbers, int radix, int len) {
        resize(len);
        Numbers.getChars(v, numbers, radix, buf, size, size + len - 1);
        size += len;
        return this;
    }

    public MyStringBuilder append(long v, int radix) {
        return append(v, Strings.LowCaseNumberChars, radix, Numbers.getBit(v, radix));
    }

    public MyStringBuilder append(long v) {
        return append(v, 10);
    }

    public MyStringBuilder append(char v) {
        resize(1);
        buf[size++] = v;
        return this;
    }

    public MyStringBuilder append(char v, int repeat) {
        for (int i = 0; i < repeat; i++)
            append(v);
        return this;
    }

    public MyStringBuilder append(boolean v) {
        return appendLess(v ? Strings.TrueChars : Strings.FalseChars);
    }

    public MyStringBuilder append(float v) {
        resize(26);
        size += Numbers.getChars(v, buf, size);
        return this;
    }

    public MyStringBuilder append(double v) {
        resize(26);
        size += Numbers.getChars(v, buf, size);
        return this;
    }

    public MyStringBuilder appendNull() {
        return appendLess(Strings.NullChars);
    }

    public interface AppendTo {
        public MyStringBuilder appendTo(MyStringBuilder ap);
    }

    public <T> MyStringBuilder append(AppendTo ap) {
        ap.appendTo(this);
        return this;
    }

    public MyStringBuilder appendLess(char[] v, int off, int len) {
        resize(len);
        char[] buf = this.buf;
        int size = this.size;
        for (int i = 0; i < len; i++)
            buf[size + i] = v[off + i];
        this.size += len;
        return this;
    }

    public MyStringBuilder appendLess(char[] v) {
        return appendLess(v, 0, v.length);
    }

    public MyStringBuilder append(Object obj) {
        if (obj == null)
            return appendNull();
        Class<?> clz = obj.getClass();
        if (obj instanceof CharSequence) {
            if (clz == String.class)
                return append((String) obj);
            if (clz == MyStringBuilder.class)
                return append((MyStringBuilder) obj);
            return append(obj, null, -1, -1);
        } else if (obj instanceof Number) {
            if (clz == Integer.class)
                return append((int) obj);
            else if (clz == Long.class)
                return append((long) obj);
            else if (clz == Byte.class)
                return append((byte) obj);
            else if (clz == Short.class)
                return append((short) obj);
            else if (clz == Float.class)
                return append((float) obj);
            else if (clz == Double.class)
                return append((double) obj);
        } else if (clz == char[].class)
            return append((char[]) obj);
        else if (clz == Character.class)
            return append((char) obj);
        else if (clz == Boolean.class)
            return append((boolean) obj);
        else if (obj instanceof AppendTo)
            return ((AppendTo) obj).appendTo(this);
        else if (clz == byte[].class)
            return appendBase64((byte[]) obj);
        return append(obj.toString());
    }

    public interface AppendHandler<T> {
        public int getLength(T v);

        public void getChars(T v, char[] c, int off, int len);
    }

    public <T> MyStringBuilder append(T v, AppendHandler<T> h) {
        int len = h.getLength(v);
        resize(len);
        h.getChars(v, buf, size, len);
        size += len;
        return this;
    }

    public MyStringBuilder appendEscape(char c) {
        switch (c) {
            case '\\':
                append('\\').append('\\');
                break;
            case '\'':
                append('\\').append('\'');
                break;
            case '\"':
                append('\\').append('\"');
                break;
            case '\r':
                append('\\').append('r');
                break;
            case '\n':
                append('\\').append('n');
                break;
            case '\f':
                append('\\').append('f');
                break;
            case '\t':
                append('\\').append('t');
                break;
            case '\b':
                append('\\').append('b');
                break;
            default:
                if (c > 127 || Strings.isVisibleChar(c))
                    append(c);
                else {
                    append('\\').append('u').append(c, Strings.LowCaseNumberChars, 16, 4);
                }
        }
        return this;
    }

    public <S> MyStringBuilder appendEscape(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        for (; begin < end; begin++)
            appendEscape(ctr.get(str, begin));
        return this;
    }

    public <S> MyStringBuilder appendUnEscape(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        for (; begin < end; begin++) {
            char c = ctr.get(str, begin);
            if (c == '\\') {
                c = ctr.get(str, ++begin);
                switch (c) {
                    case '\\':
                        append('\\');
                        break;
                    case '\'':
                        append('\'');
                        break;
                    case '\"':
                        append('\"');
                        break;
                    case 'r':
                        append('\r');
                        break;
                    case 'n':
                        append('\n');
                        break;
                    case 'f':
                        append('\f');
                        break;
                    case 't':
                        append('\t');
                        break;
                    case 'b':
                        append('\b');
                        break;
                    case 'u':
                        append((char) Numbers.parseInt(str, ctr, 16, begin + 1, begin + 5));
                        begin += 4;
                        break;
                    default:
                        if (Strings.isNumber(c)) {
                            int size = 1, n = ctr.getLength(str);
                            if (begin + 1 < n && Strings.isNumber(ctr.get(str, begin + 1))) {
                                size++;
                                if (begin + 2 < n && Strings.isNumber(ctr.get(str, begin + 2)))
                                    size++;
                            }
                            append((char) Numbers.parseInt(str, ctr, 8, begin, size));
                            begin += size - 1;
                        } else
                            throw new IllegalArgumentException("未知转义字符:" + "\\" + c);
                }
            } else
                append(c);
        }
        return this;
    }

    public MyStringBuilder appendBase64(byte[] bytes) {
        int len = Strings.encodeBase64(bytes, 0, bytes.length, null, 0, true);
        resize(len);
        Strings.encodeBase64(bytes, 0, bytes.length, buf, size, false);
        size += len;
        return this;
    }

    public <S> MyStringBuilder appendUpCase(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        for (; begin < end; begin++)
            append(Strings.toUpCase(ctr.get(str, begin)));
        return this;
    }

    public <S> MyStringBuilder appendLowCase(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        for (; begin < end; begin++)
            append(Strings.toLowCase(ctr.get(str, begin)));
        return this;
    }

    /**
     * 转下划线大写风格（例：HashMap -> HASH_MAP ; getValue -> GET_VALUE）
     * 
     */
    public <S> MyStringBuilder appendUpCaseLine(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        if (begin < end) {
            append(Strings.toUpCase(ctr.get(str, begin++)));
            for (; begin < end; begin++) {
                char c = ctr.get(str, begin);
                if (Strings.isUpCase(c))
                    append('_').append(c);
                else
                    append(Strings.toUpCase(c));
            }
        }
        return this;
    }

    public <S> MyStringBuilder appendLowCaseLine(S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        if (begin < end) {
            append(Strings.toLowCase(ctr.get(str, begin++)));
            for (; begin < end; begin++) {
                char c = ctr.get(str, begin);
                if (Strings.isUpCase(c))
                    append('_').append(c);
                else
                    append(Strings.toLowCase(c));
            }
        }
        return this;
    }

    public MyStringBuilder delete(int index) {
        System.arraycopy(buf, index + 1, buf, index, size - index - 1);
        size--;
        return this;
    }

    public MyStringBuilder delete(int begin, int end) {
        System.arraycopy(buf, end, buf, begin, size - end);
        size -= (end - begin);
        return this;
    }

    public MyStringBuilder deleteEnd() {
        size--;
        return this;
    }

    public MyStringBuilder deleteEnd(int len) {
        size -= len;
        return this;
    }

    public MyStringBuilder insert(int index, char c) {
        resize(1);
        System.arraycopy(buf, index, buf, index + 1, size - index);
        size++;
        buf[index] = c;
        return this;
    }

    public <S> MyStringBuilder insert(int index, S str, Strings.StringController<S> ctr, int begin,
            int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        int len = end - begin;
        resize(len);
        System.arraycopy(buf, index, buf, index + len, size - index);
        size += len;
        ctr.getChars(str, begin, end, buf, index);
        return this;
    }

    public MyStringBuilder replace(int index, char c) {
        buf[index] = c;
        return this;
    }

    public <S> MyStringBuilder replace(int from, int to, S str, Strings.StringController<S> ctr,
            int begin, int end) {
        ctr = Strings.checkController(ctr, str);
        begin = Strings.checkLeft(begin);
        end = Strings.checkEnd(end, str, ctr);
        int len = end - begin;
        int addSize = len - (to - from + 1);
        if (addSize > 0)
            resize(addSize);
        System.arraycopy(buf, to, buf, to + addSize, size - to);
        size += addSize;
        ctr.getChars(str, begin, end, buf, from);
        return this;
    }

    public void resize(int addSize) {
        int newSize = size + addSize;
        if (newSize > buf.length) {
            if (newSize < buf.length * 2)
                newSize = buf.length * 2;
            char[] newBuf = new char[newSize];
            System.arraycopy(buf, 0, newBuf, 0, buf.length);
            buf = newBuf;
        }
    }

    public interface AppendController<T> {
        public void append(MyStringBuilder sb, T value);
    }

    public <T> MyStringBuilder appends(Iterable<T> data, AppendController<T> ctr,
            String separator) {
        boolean flag = false;
        for (T o : data) {
            ctr.append(this, o);
            append(separator);
            flag = true;
        }
        if (flag)
            deleteEnd(separator.length());
        return this;
    }

    public <T> MyStringBuilder appends(Collection<T> data, AppendController<T> ctr,
            String separator) {
        if (!data.isEmpty()) {
            for (T o : data) {
                ctr.append(this, o);
                append(separator);
            }
            deleteEnd(separator.length());
        }
        return this;
    }

    public <T> MyStringBuilder appends(List<T> data, AppendController<T> ctr, String separator) {
        int len = data.size();
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++) {
                ctr.append(this, data.get(i));
                append(separator);
            }
            ctr.append(this, data.get(len));
        }
        return this;
    }

    public <T> MyStringBuilder appends(T[] data, AppendController<T> ctr, String separator) {
        int len = data.length;
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++) {
                ctr.append(this, data[i]);
                append(separator);
            }
            ctr.append(this, data[len]);
        }
        return this;
    }

    public <T> MyStringBuilder appends(Object data, AppendController<T> actr, String separator) {
        ArrayController<T, Object> ctr = ArrayController.valueOf(data);
        int len = ctr.getLength(data);
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++) {
                actr.append(this, ctr.get(data, i));
                append(separator);
            }
            actr.append(this, ctr.get(data, len));
        }
        return this;
    }

    public <T> MyStringBuilder appends(Iterable<T> data, Converter<?, ? super T> conv,
            String separator) {
        boolean flag = false;
        for (T o : data) {
            append(conv.convert(o)).append(separator);
            flag = true;
        }
        if (flag)
            deleteEnd(separator.length());
        return this;
    }

    public <T> MyStringBuilder appends(Collection<T> data, Converter<?, ? super T> conv,
            String separator) {
        if (!data.isEmpty()) {
            for (T o : data)
                append(conv.convert(o)).append(separator);
            deleteEnd(separator.length());
        }
        return this;
    }

    public <T> MyStringBuilder appends(List<T> data, Converter<?, ? super T> conv,
            String separator) {
        int len = data.size();
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++)
                append(conv.convert(data.get(i))).append(separator);
            append(conv.convert(data.get(len)));
        }
        return this;
    }

    public <T> MyStringBuilder appends(T[] data, Converter<?, ? super T> conv, String separator) {
        int len = data.length;
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++)
                append(conv.convert(data[i])).append(separator);
            append(conv.convert(data[len]));
        }
        return this;
    }

    public <T> MyStringBuilder appends(Object data, Converter<?, ? super T> conv,
            String separator) {
        ArrayController<T, Object> ctr = ArrayController.valueOf(data);
        int len = ctr.getLength(data);
        if (len > 0) {
            len--;
            for (int i = 0; i < len; i++)
                append(conv.convert(ctr.get(data, i))).append(separator);
            append(conv.convert(ctr.get(data, len)));
        }
        return this;
    }

    public <T> MyStringBuilder appends(Iterable<T> data, String separator) {
        return appends(data, Converters.NotConvert, separator);
    }

    public <T> MyStringBuilder appends(Collection<T> data, String separator) {
        return appends(data, Converters.NotConvert, separator);
    }

    public <T> MyStringBuilder appends(List<T> data, String separator) {
        return appends(data, Converters.NotConvert, separator);
    }

    public <T> MyStringBuilder appends(T[] data, String separator) {
        return appends(data, Converters.NotConvert, separator);
    }

    public <T> MyStringBuilder appends(Object data, String separator) {
        return appends(data, Converters.NotConvert, separator);
    }
}

