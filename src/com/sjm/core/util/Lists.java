package com.sjm.core.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Lists {
    public static List<Integer> from(int[] arr) {
        return new MyArrayList<Integer, int[]>(arr, ArrayController.FOR_INT);
    }

    public static List<Long> from(long[] arr) {
        return new MyArrayList<Long, long[]>(arr, ArrayController.FOR_LONG);
    }

    public static List<Short> from(short[] arr) {
        return new MyArrayList<Short, short[]>(arr, ArrayController.FOR_SHORT);
    }

    public static List<Byte> from(byte[] arr) {
        return new MyArrayList<Byte, byte[]>(arr, ArrayController.FOR_BYTE);
    }

    public static List<Boolean> from(boolean[] arr) {
        return new MyArrayList<Boolean, boolean[]>(arr, ArrayController.FOR_BOOLEAN);
    }

    public static List<Character> from(char[] arr) {
        return new MyArrayList<Character, char[]>(arr, ArrayController.FOR_CHAR);
    }

    public static List<Double> from(double[] arr) {
        return new MyArrayList<Double, double[]>(arr, ArrayController.FOR_DOUBLE);
    }

    public static List<Float> from(float[] arr) {
        return new MyArrayList<Float, float[]>(arr, ArrayController.FOR_FLOAT);
    }

    public static <T> List<T> from(T[] arr) {
        return new MyArrayList<T, T[]>(arr, (ArrayController) ArrayController.valueOf(arr));
    }

    public static List<Character> from(CharSequence cs) {
        return new CharSequenceList(cs);
    }

    public static <T> List<T> from(Object arr) {
        if (arr == null)
            return emptyList;
        if (arr instanceof List)
            return (List) arr;
        if (arr instanceof CharSequence)
            return (List) new CharSequenceList((CharSequence) arr);
        return new MyArrayList(arr, ArrayController.valueOf(arr));
    }

    public static <T> List<T> from(Iterable<T> it) {
        List<T> list = new ArrayList<T>();
        for (T value : it)
            list.add(value);
        return list;
    }

    /**
     * 转换List为另一种类型
     * 
     * @param list 待转换的List
     * @param conv 转换器
     * @return
     */
    public static <D, S> List<D> convert(List<S> list, Converter<? extends D, ? super S> conv) {
        return new ConvertList<D, S>(list, conv);
    }

    public static <D, S> List<D> convert(S[] arr, Converter<? extends D, ? super S> conv) {
        return new ConvertArrayList<D, S>(arr, conv);
    }

    /**
     * 缓存List,原List不可有空元素，数组长度不可变化
     * 
     * @param list 待缓存的List
     * @return
     */
    public static <T> List<T> cache(List<T> list) {
        return new CacheList<T>(list);
    }

    /**
     * 截取子List
     * 
     * @param list 待转换的List
     * @param off 子List的偏移
     * @param len 子List的长度
     * @return
     */
    public static <T> List<T> sub(List<T> list, int off, int len) {
        return new SubList<T>(list, off, len);
    }

    /**
     * 合并两个List
     * 
     * @param list1
     * @param list2
     * @return
     */
    public static <T> List<T> combine(List<T> list1, List<T> list2) {
        return new CombineDoubleList<T>(list1, list2);
    }

    /**
     * 将List倒序
     * 
     * @param list
     * @return
     */
    public static <T> List<T> reverse(List<T> list) {
        return new ReverseList<T>(list);
    }

    /**
     * 生成一个随机List
     * 
     * @param getter 随机值产生器
     * @param len list的长度
     * @return
     */
    public static <T> List<T> random(Randoms.Getter<T> getter, int len) {
        return new RandomList<T>(getter, len);
    }

    public static List<Integer> randomInt(int range, int len) {
        return new RandomList<Integer>(Randoms.intGetter(range), len);
    }

    public static List<Integer> randomInt(int from, int to, int len) {
        return new RandomList<Integer>(Randoms.intGetter(from, to), len);
    }

    public static List<String> randomString(String chars, int strLen, int len) {
        return new RandomList<String>(Randoms.stringGetter(chars, strLen), len);
    }

    /**
     * 生成一个元素重复的List
     * 
     * @param value 元素的值
     * @param len list的长度
     * @return
     */
    public static <T> List<T> repeat(T value, int len) {
        return new RepeatList<T>(value, len);
    }

    /**
     * 获取一个线性递增List，步长为1
     * 
     * @param start 起始值
     * @param len list长度
     * @return
     */
    public static List<Integer> linearInt(int start, int len) {
        return new LinearList(start, len);
    }

    /**
     * 获取一个写时复制的list，写入时不覆盖原来的list
     * 
     * @param list
     * @return
     */
    public static <T> List<T> writeCopy(List<T> list) {
        return new WriteCopyList<T>(list);
    }

    /**
     * 获取一个只读的list，写入将报异常
     * 
     * @param list
     * @return
     */
    public static <T> List<T> readOnly(List<T> list) {
        return new ReadOnlyList<T>(list);
    }

    public static <T> void addAll(Collection<T> col, Object arr, ArrayController<T, Object> ctr) {
        for (int i = 0, len = ctr.getLength(arr); i < len; i++)
            col.add(ctr.get(arr, i));
    }

    /**
     * 将List转换为数组
     * 
     * @param list 待转换的List
     * @param componentType 数组成员类型，可以是基本数据类型
     * @return
     */
    public static Object toArray(List<?> list, Class<?> componentType) {
        int len = list.size();
        ArrayController ctr = ArrayController.valueOf(componentType);
        Object arr = ctr.newInstance(len);
        for (int i = 0; i < len; i++)
            ctr.set(arr, i, list.get(i));
        return arr;
    }

    public static <T> T[] toTArray(List<T> list, Class<?> componentType) {
        if (componentType.isPrimitive())
            throw new IllegalArgumentException();
        int len = list.size();
        ArrayController ctr = ArrayController.valueOf(componentType);
        Object arr = ctr.newInstance(len);
        for (int i = 0; i < len; i++)
            ctr.set(arr, i, list.get(i));
        return (T[]) arr;
    }

    public static Object toArray(Collection<?> col, Class<?> componentType) {
        int len = col.size();
        ArrayController ctr = ArrayController.valueOf(componentType);
        Object arr = ctr.newInstance(len);
        int i = 0;
        for (Object value : col)
            ctr.set(arr, i++, value);
        return arr;
    }

    public static <T> T[] toTArray(Collection<T> col, Class<?> componentType) {
        if (componentType.isPrimitive())
            throw new IllegalArgumentException();
        int len = col.size();
        ArrayController ctr = ArrayController.valueOf(componentType);
        Object arr = ctr.newInstance(len);
        int i = 0;
        for (Object value : col)
            ctr.set(arr, i++, value);
        return (T[]) arr;
    }

    /**
     * 将List转换为ArrayList
     * 
     * @param list 待转换的List
     * @return
     */
    public static <T> ArrayList<T> toArrayList(List<T> list, int begin, int end) {
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = list.size();
        ArrayList<T> arr = new ArrayList<T>(end - begin);
        for (int i = begin; i < end; i++)
            arr.add(list.get(i));
        return arr;
    }

    public static <T> ArrayList<T> toArrayList(Iterable<T> list) {
        if (list instanceof ArrayList)
            return (ArrayList<T>) list;
        ArrayList<T> arr = new ArrayList<T>();
        for (T value : list)
            arr.add(value);
        return arr;
    }

    public static void checkIndex(int index, int size) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index:" + index);
    }

    public static void checkIndex(int off, int len, int size) {
        if (off < 0 || len < 0 || off + len > size)
            throw new IndexOutOfBoundsException("off:" + off + ",len:" + len);
    }

    public static final int[] emptyIntArray = new int[0];
    public static final long[] emptyLongArray = new long[0];
    public static final char[] emptyCharArray = new char[0];
    public static final short[] emptyShortArray = new short[0];
    public static final byte[] emptyByteArray = new byte[0];
    public static final boolean[] emptyBooleanArray = new boolean[0];
    public static final float[] emptyFloatArray = new float[0];
    public static final double[] emptyDoubleArray = new double[0];
    public static final Object[] emptyObjectArray = new Object[0];
    public static final String[] emptyStringArray = new String[0];

    public static final List emptyList = new AbstractList() {
        @Override
        public Object get(int index) {
            throw new IndexOutOfBoundsException("index:" + index);
        }

        @Override
        public int size() {
            return 0;
        }
    };

    public static <T> List<T> emptyList() {
        return emptyList;
    }

    public static final Comparator<Object> DefaultComparator = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }
    };

    public static <T, A> int binarySearch(A array, ArrayController<T, A> ctr, T key,
            Comparator<? super T> comp, int begin, int end) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (comp == null)
            comp = DefaultComparator;
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = ctr.getLength(array);
        int low = begin;
        int high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = comp.compare(ctr.get(array, mid), key);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -low - 1;
    }

    public static <D, S, A> int binarySearch(A array, ArrayController<S, A> ctr,
            Converter<? extends D, S> conv, D key, Comparator<? super D> comp, int begin, int end) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (comp == null)
            comp = DefaultComparator;
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = ctr.getLength(array);
        int low = begin;
        int high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = comp.compare(conv.convert(ctr.get(array, mid)), key);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -low - 1;
    }

    public static <T, A> int indexOf(A array, ArrayController<T, A> ctr, T value, int from,
            int to) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = 0;
        if (to == -1)
            to = ctr.getLength(array) - 1;
        if (value == null) {
            for (; from <= to; from++)
                if (ctr.get(array, from) == null)
                    return from;
        } else {
            for (; from <= to; from++)
                if (value.equals(ctr.get(array, from)))
                    return from;
        }
        return -1;
    }

    public static <T, A> int lastIndexOf(A array, ArrayController<T, A> ctr, T value, int from,
            int to) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = ctr.getLength(array) - 1;
        if (to == -1)
            to = 0;
        if (value == null) {
            for (; from >= to; from--)
                if (ctr.get(array, from) == null)
                    return from;
        } else {
            for (; from >= to; from--)
                if (value.equals(ctr.get(array, from)))
                    return from;
        }
        return -1;
    }

    public static <T, A> int indexOfFilter(A array, ArrayController<T, A> ctr, Filter<? super T> f,
            int from, int to) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = 0;
        if (to == -1)
            to = ctr.getLength(array) - 1;
        for (; from <= to; from++) {
            if (f.accept(ctr.get(array, from)))
                return from;
        }
        return -1;
    }

    public static <T, A> int lastIndexOfFilter(A array, ArrayController<T, A> ctr,
            Filter<? super T> f, int from, int to) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = ctr.getLength(array) - 1;
        if (to == -1)
            to = 0;
        for (; from >= to; from--) {
            if (f.accept(ctr.get(array, from)))
                return from;
        }
        return -1;
    }

    public static <T, A> int indexOfIdentity(A array, ArrayController<T, A> ctr, T key, int from,
            int to) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = 0;
        if (to == -1)
            to = ctr.getLength(array) - 1;
        for (; from <= to; from++)
            if (ctr.get(array, from) == key)
                return from;
        return -1;
    }

    public static <T, A> int lastIndexOfIdentity(A array, ArrayController<T, A> ctr, int from,
            int to, T key) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (from == -1)
            from = ctr.getLength(array) - 1;
        if (to == -1)
            to = 0;
        for (; from >= to; from--)
            if (ctr.get(array, from) == key)
                return from;
        return -1;
    }

    public static <T, A> void swap(A arr, ArrayController<T, A> ctr, int i, int j) {
        T t = ctr.get(arr, i);
        ctr.set(arr, i, ctr.get(arr, j));
        ctr.set(arr, j, t);
    }

    public static <T, A> void fill(A array, ArrayController<T, A> ctr, int begin, int end,
            T value) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = ctr.getLength(array);
        for (int i = begin; i < end; i++)
            ctr.set(array, i, value);
    }

    public static void main(String[] args) {
        System.out.println(int.class.getComponentType());
    }

    public static <T, SA, DA> void copy(SA src, ArrayController<T, SA> srcCtr, int srcOff, DA dst,
            ArrayController<T, DA> dstCtr, int dstOff, int len) {
        if (len > 0) {
            Class<?> st = src.getClass().getComponentType();
            Class<?> dt = dst.getClass().getComponentType();
            if (st != null && st == dt)
                System.arraycopy(src, srcOff, dst, dstOff, len);
            else {
                if (src == dst && srcOff < dstOff)
                    for (int i = len - 1; i >= 0; i--)
                        dstCtr.set(dst, dstOff + i, srcCtr.get(src, srcOff + i));
                else
                    for (int i = 0; i < len; i++)
                        dstCtr.set(dst, dstOff + i, srcCtr.get(src, srcOff + i));
            }
        }
    }

    private static <T, A> void quickSort1(A array, ArrayController<T, A> ctr,
            Comparator<? super T> cmp, int from, int to) {
        if (from < to) {
            int p = from - 1;
            T x = ctr.get(array, to);
            for (int i = from; i <= to; i++) {
                if (cmp.compare(ctr.get(array, i), x) <= 0) {
                    p++;
                    swap(array, ctr, p, i);
                }
            }
            quickSort1(array, ctr, cmp, from, p - 1);
            quickSort1(array, ctr, cmp, p + 1, to);
        }
    }

    public static <T, A> void quickSort(A array, ArrayController<T, A> ctr,
            Comparator<? super T> cmp, int begin, int end) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = ctr.getLength(array) - 1;
        if (cmp == null)
            cmp = DefaultComparator;
        quickSort1(array, ctr, cmp, begin, end);
    }

    public static <T, A> void insertSort(A array, ArrayController<T, A> ctr,
            Comparator<? super T> cmp, int begin, int end) {
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        if (cmp == null)
            cmp = DefaultComparator;
        if (begin == -1)
            begin = 0;
        if (end == -1)
            end = ctr.getLength(array);
        for (; begin < end; begin++) {
            T value = ctr.get(array, begin);
            for (int j = 0; j < begin; j++) {
                if (cmp.compare(value, ctr.get(array, j)) < 0) {
                    copy(array, ctr, j, array, ctr, j + 1, begin - j);
                    ctr.set(array, j, value);
                    break;
                }
            }
        }
    }

    public static <T, A> T get(A array, ArrayController<T, A> ctr, int index, T defaultValue) {
        if (array == null)
            return null;
        if (ctr == null)
            ctr = ArrayController.valueOf(array);
        return array == null || index < 0 || index >= ctr.getLength(array) ? defaultValue
                : ctr.get(array, index);
    }

    public static <T> T getFirst(List<T> list) {
        return get(list, null, 0, null);
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    static class ConvertList<D, S> extends AbstractList<D> {
        protected List<S> list;
        protected Converter<? extends D, ? super S> conv;

        public ConvertList(List<S> list, Converter<? extends D, ? super S> conv) {
            this.list = list;
            this.conv = conv;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public D get(int index) {
            return conv.convert(list.get(index));
        }
    }
    static class ConvertArrayList<D, S> extends AbstractList<D> {
        protected S[] arr;
        protected Converter<? extends D, ? super S> conv;

        public ConvertArrayList(S[] arr, Converter<? extends D, ? super S> conv) {
            this.arr = arr;
            this.conv = conv;
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public D get(int index) {
            return conv.convert(arr[index]);
        }
    }
    static class CharSequenceList extends AbstractList<Character> {
        protected CharSequence cs;

        public CharSequenceList(CharSequence cs) {
            this.cs = cs;
        }

        @Override
        public int size() {
            return cs.length();
        }

        @Override
        public Character get(int index) {
            return cs.charAt(index);
        }
    }
    static class SubList<T> extends AbstractList<T> {
        protected List<T> list;
        protected int off;
        protected int len;

        public SubList(List<T> list, int off, int len) {
            checkIndex(off, len, list.size());
            if (list instanceof SubList) {
                SubList<T> subList = (SubList<T>) list;
                this.list = subList.list;
                this.off = subList.off + off;
                this.len = len;
            } else {
                this.list = list;
                this.off = off;
                this.len = len;
            }
        }

        @Override
        public int size() {
            return len;
        }

        @Override
        public T get(int index) {
            checkIndex(index, len);
            return list.get(off + index);
        }

        @Override
        public T set(int index, T value) {
            checkIndex(index, len);
            return list.set(off + index, value);
        }

        @Override
        public void add(int index, T value) {
            checkIndex(index, len);
            list.add(off + index, value);
        }

        @Override
        public T remove(int index) {
            checkIndex(index, len);
            return list.remove(off + index);
        }
    }
    static class CombineDoubleList<T> extends AbstractList<T> {
        protected List<T> list1, list2;

        public CombineDoubleList(List<T> list1, List<T> list2) {
            this.list1 = list1;
            this.list2 = list2;
        }

        @Override
        public int size() {
            return list1.size() + list2.size();
        }

        @Override
        public T get(int index) {
            int len = list1.size();
            if (index < len)
                return list1.get(index);
            else
                return list2.get(index - len);
        }

        @Override
        public T set(int index, T value) {
            int len = list1.size();
            if (index < len)
                return list1.set(index, value);
            else
                return list2.set(index - len, value);
        }

        @Override
        public void add(int index, T value) {
            int len = list1.size();
            if (index < len)
                list1.add(index, value);
            else
                list2.add(index - len, value);
        }

        @Override
        public T remove(int index) {
            int len = list1.size();
            if (index < len)
                return list1.remove(index);
            else
                return list2.remove(index - len);
        }
    }
    static class ReverseList<T> extends AbstractList<T> {
        protected List<T> list;

        public ReverseList(List<T> list) {
            this.list = list;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public T get(int index) {
            return list.get(list.size() - index - 1);
        }

        @Override
        public T set(int index, T value) {
            return list.set(list.size() - index - 1, value);
        }

        @Override
        public void add(int index, T value) {
            list.add(list.size() - index - 1, value);
        }

        @Override
        public T remove(int index) {
            return list.remove(list.size() - index - 1);
        }
    }
    static class RepeatList<T> extends AbstractList<T> {
        protected T value;
        protected int len;

        public RepeatList(T value, int len) {
            this.value = value;
            this.len = len;
        }

        @Override
        public int size() {
            return len;
        }

        @Override
        public T get(int index) {
            checkIndex(index, len);
            return value;
        }
    }
    static class LinearList extends AbstractList<Integer> {
        protected int start, len;

        public LinearList(int start, int len) {
            this.start = start;
            this.len = len;
        }

        @Override
        public int size() {
            return len;
        }

        @Override
        public Integer get(int index) {
            checkIndex(index, len);
            return start + index;
        }
    }
    static class RandomList<T> extends AbstractList<T> {
        protected int len;
        protected Randoms.Getter<T> getter;

        public RandomList(Randoms.Getter<T> getter, int len) {
            this.getter = getter;
            this.len = len;
        }

        @Override
        public T get(int index) {
            checkIndex(index, len);
            return getter.get();
        }

        @Override
        public int size() {
            return len;
        }
    }
    static class WriteCopyList<T> extends AbstractList<T> {
        protected List<T> list;
        protected HashMap<Integer, T> writeCopyMap = new HashMap<Integer, T>();

        public WriteCopyList(List<T> list) {
            this.list = list;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public T get(int index) {
            T r = writeCopyMap.get(index);
            return r != null ? r : list.get(index);
        }

        @Override
        public T set(int index, T value) {
            writeCopyMap.put(index, value);
            return list.set(index, value);
        }
    }
    static class ReadOnlyList<T> extends AbstractList<T> {
        protected List<T> list;

        public ReadOnlyList(List<T> list) {
            this.list = list;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public T get(int index) {
            return list.get(index);
        }
    }
    static class CacheList<T> extends AbstractList<T> {
        protected List<T> list;
        protected Object[] result;

        public CacheList(List<T> list) {
            this.list = list;
            result = new Object[list.size()];
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public T get(int index) {
            T r = (T) result[index];
            if (r == null) {
                r = list.get(index);
                result[index] = r;
            }
            return r;
        }
    }
    public static class MyArrayList<T, A> extends AbstractList<T> implements Cloneable {
        protected A arr;
        protected ArrayController<T, A> ctr;
        protected int size, cap;

        public MyArrayList(A arr, ArrayController<T, A> ctr, int size) {
            this.arr = arr;
            this.ctr = ctr;
            this.size = size;
            this.cap = ctr.getLength(arr);
        }

        public MyArrayList(A arr, ArrayController<T, A> ctr) {
            this(arr, ctr, ctr.getLength(arr));
        }

        public MyArrayList(ArrayController<T, A> ctr, int cap) {
            this(ctr.newInstance(cap), ctr, 0);
        }

        public MyArrayList(ArrayController<T, A> ctr) {
            this(ctr, 10);
        }

        public MyArrayList(Class<? super T> type, int cap) {
            this((ArrayController<T, A>) ArrayController.valueOf(type), cap);
        }

        public MyArrayList(Class<? super T> type) {
            this((ArrayController<T, A>) ArrayController.valueOf(type), 10);
        }

        public MyArrayList(int cap) {
            this((Class<T>) Object.class, cap);
        }

        public MyArrayList() {
            this((Class<T>) Object.class, 10);
        }

        public int capacity() {
            return cap;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public Object clone() {
            MyArrayList<T, A> list;
            try {
                list = (MyArrayList<T, A>) super.clone();
                int len = ctr.getLength(arr);
                list.arr = ctr.newInstance(len);
                Lists.copy(arr, ctr, 0, list.arr, ctr, 0, len);
                return list;
            } catch (CloneNotSupportedException e) {
                throw new Error();
            }
        }

        @Override
        public T get(int index) {
            Lists.checkIndex(index, size);
            return ctr.get(arr, index);
        }

        @Override
        public T set(int index, T value) {
            Lists.checkIndex(index, size);
            T old = ctr.get(arr, index);
            ctr.set(arr, index, value);
            return old;
        }

        @Override
        public boolean add(T element) {
            resize(1);
            ctr.set(arr, size++, element);
            return true;
        }

        @Override
        public void add(int index, T element) {
            if (index == size) {
                add(element);
            } else {
                Lists.checkIndex(index, size);
                resize(1);
                Lists.copy(arr, ctr, index, arr, ctr, index + 1, size++ - index);
                ctr.set(arr, index, element);
            }
        }

        @Override
        public T remove(int index) {
            Lists.checkIndex(index, size);
            T old = ctr.get(arr, index);
            Lists.copy(arr, ctr, index + 1, arr, ctr, index, --size - index);
            ctr.set(arr, size, ctr.zeroValue());
            return old;
        }

        @Override
        public void clear() {
            Lists.fill(this, null, 0, size, ctr.zeroValue());
            size = 0;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            resize(c.size());
            int nSize = size;
            for (Iterator<? extends T> i = c.iterator(); i.hasNext();)
                ctr.set(arr, nSize++, i.next());
            if (nSize == size)
                return false;
            size = nSize;
            return true;
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            if (index == size)
                return addAll(c);
            else {
                Lists.checkIndex(index, size);
                int len = c.size();
                if (len == 0)
                    return false;
                resize(len);
                Lists.copy(arr, ctr, index, arr, ctr, index + len, size - index);
                size += len;
                for (Iterator<? extends T> i = c.iterator(); i.hasNext()
                        && len >= 0; len--, index++) {
                    ctr.set(arr, index, i.next());
                }
                return true;
            }
        }

        @Override
        public void removeRange(int from, int to) {
            int len = to - from + 1;
            Lists.checkIndex(from, len, size);
            int newSize = size - len;
            Lists.copy(arr, ctr, to + 1, arr, ctr, from, newSize - from);
            Lists.fill(this, null, newSize, size, ctr.zeroValue());
            size = newSize;
        }


        protected void resize(int n) {
            int need = size + n;
            if (cap < need) {
                cap *= 2;
                if (cap < need)
                    cap = need;
                A newArr = ctr.newInstance(cap);
                Lists.copy(arr, ctr, 0, newArr, ctr, 0, size);
                arr = newArr;
            }
        }
    }
}
