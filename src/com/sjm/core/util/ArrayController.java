package com.sjm.core.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 数组控制器接口，用于统一操作不同类型的数组
 *
 * @param <T> 成员类型
 * @param <A> 数组类型
 */
public abstract class ArrayController<T, A> {
    public abstract T get(A arr, int index);

    public abstract void set(A arr, int index, T value);

    public abstract int getLength(A arr);

    public abstract A newInstance(int len);

    public abstract T zeroValue();

    @SuppressWarnings("unchecked")
    public static <T, A> ArrayController<T, A> valueOf(Class<?> cls) {
        ArrayController<?, ?> ctrl = ctrlMap.get(cls);
        if (ctrl == null)
            ctrlMap.put(cls, ctrl = new NormalArrayController<>(cls));
        return (ArrayController<T, A>) ctrl;
    }

    @SuppressWarnings("unchecked")
    public static <T, A> ArrayController<T, A> valueOf(Object arr) {
        ArrayController<?, ?> ctrl;
        if (arr instanceof List)
            ctrl = FOR_LIST;
        else {
            Class<?> cls = arr.getClass();
            if (cls.isArray()) {
                ctrl = valueOf(cls.getComponentType());
            } else if (arr instanceof CharSequence)
                ctrl = FOR_CHARSEQENCE;
            else
                throw new IllegalArgumentException("不是一个数组");
        }
        return (ArrayController<T, A>) ctrl;
    }

    public static boolean isArray(Object arr) {
        return arr.getClass().isArray() || arr instanceof List || arr instanceof CharSequence;
    }

    public static final ArrayController<Integer, int[]> FOR_INT =
            new ArrayController<Integer, int[]>() {
                @Override
                public Integer get(int[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(int[] arr, int index, Integer value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(int[] arr) {
                    return arr.length;
                }

                @Override
                public int[] newInstance(int len) {
                    return new int[len];
                }

                @Override
                public Integer zeroValue() {
                    return Integer.valueOf(0);
                }
            };
    public static final ArrayController<Long, long[]> FOR_LONG =
            new ArrayController<Long, long[]>() {
                @Override
                public Long get(long[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(long[] arr, int index, Long value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(long[] arr) {
                    return arr.length;
                }

                @Override
                public long[] newInstance(int len) {
                    return new long[len];
                }

                @Override
                public Long zeroValue() {
                    return Long.valueOf(0);
                }
            };
    public static final ArrayController<Character, char[]> FOR_CHAR =
            new ArrayController<Character, char[]>() {
                @Override
                public Character get(char[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(char[] arr, int index, Character value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(char[] arr) {
                    return arr.length;
                }

                @Override
                public char[] newInstance(int len) {
                    return new char[len];
                }

                @Override
                public Character zeroValue() {
                    return Character.valueOf('\0');
                }
            };
    public static final ArrayController<Short, short[]> FOR_SHORT =
            new ArrayController<Short, short[]>() {
                @Override
                public Short get(short[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(short[] arr, int index, Short value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(short[] arr) {
                    return arr.length;
                }

                @Override
                public short[] newInstance(int len) {
                    return new short[len];
                }

                @Override
                public Short zeroValue() {
                    return Short.valueOf((short) 0);
                }
            };
    public static final ArrayController<Byte, byte[]> FOR_BYTE =
            new ArrayController<Byte, byte[]>() {
                @Override
                public Byte get(byte[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(byte[] arr, int index, Byte value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(byte[] arr) {
                    return arr.length;
                }

                @Override
                public byte[] newInstance(int len) {
                    return new byte[len];
                }

                @Override
                public Byte zeroValue() {
                    return Byte.valueOf((byte) 0);
                }
            };
    public static final ArrayController<Float, float[]> FOR_FLOAT =
            new ArrayController<Float, float[]>() {
                @Override
                public Float get(float[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(float[] arr, int index, Float value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(float[] arr) {
                    return arr.length;
                }

                @Override
                public float[] newInstance(int len) {
                    return new float[len];
                }

                @Override
                public Float zeroValue() {
                    return Float.valueOf(0);
                }
            };
    public static final ArrayController<Double, double[]> FOR_DOUBLE =
            new ArrayController<Double, double[]>() {
                @Override
                public Double get(double[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(double[] arr, int index, Double value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(double[] arr) {
                    return arr.length;
                }

                @Override
                public double[] newInstance(int len) {
                    return new double[len];
                }

                @Override
                public Double zeroValue() {
                    return Double.valueOf(0);
                }
            };
    public static final ArrayController<Boolean, boolean[]> FOR_BOOLEAN =
            new ArrayController<Boolean, boolean[]>() {
                @Override
                public Boolean get(boolean[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(boolean[] arr, int index, Boolean value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(boolean[] arr) {
                    return arr.length;
                }

                @Override
                public boolean[] newInstance(int len) {
                    return new boolean[len];
                }

                @Override
                public Boolean zeroValue() {
                    return Boolean.FALSE;
                }
            };
    public static final ArrayController<Object, Object[]> FOR_OBJECT =
            new ArrayController<Object, Object[]>() {
                @Override
                public Object get(Object[] arr, int index) {
                    return arr[index];
                }

                @Override
                public void set(Object[] arr, int index, Object value) {
                    arr[index] = value;
                }

                @Override
                public int getLength(Object[] arr) {
                    return arr.length;
                }

                @Override
                public Object[] newInstance(int len) {
                    return new Object[len];
                }

                @Override
                public Object zeroValue() {
                    return null;
                }
            };
    @SuppressWarnings("rawtypes")
    public static final ArrayController FOR_LIST = new ArrayController<Object, List<Object>>() {
        @Override
        public Object get(List<Object> arr, int index) {
            return arr.get(index);
        }

        @Override
        public void set(List<Object> arr, int index, Object value) {
            arr.set(index, value);
        }

        @Override
        public int getLength(List<Object> arr) {
            return arr.size();
        }

        @Override
        public List<Object> newInstance(int len) {
            return new ArrayList<Object>();
        }

        @Override
        public Object zeroValue() {
            return null;
        }
    };
    public static final ArrayController<Character, CharSequence> FOR_CHARSEQENCE =
            new ArrayController<Character, CharSequence>() {
                @Override
                public Character get(CharSequence arr, int index) {
                    return arr.charAt(index);
                }

                @Override
                public void set(CharSequence arr, int index, Character value) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getLength(CharSequence arr) {
                    return arr.length();
                }

                @Override
                public CharSequence newInstance(int len) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Character zeroValue() {
                    return Character.valueOf('\0');
                }
            };

    public static class NormalArrayController<T> extends ArrayController<T, T[]> {
        protected Class<T> cls;

        public NormalArrayController(Class<T> cls) {
            this.cls = cls;
        }

        @Override
        public T get(T[] arr, int index) {
            return arr[index];
        }

        @Override
        public void set(T[] arr, int index, T value) {
            arr[index] = value;
        }

        @Override
        public int getLength(T[] arr) {
            return arr.length;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T[] newInstance(int len) {
            return (T[]) Array.newInstance(cls, len);
        }

        @Override
        public T zeroValue() {
            return null;
        }
    }

    private static HashMap<Class<?>, ArrayController<?, ?>> ctrlMap;

    static {
        ctrlMap = new HashMap<Class<?>, ArrayController<?, ?>>();
        ctrlMap.put(int.class, FOR_INT);
        ctrlMap.put(long.class, FOR_LONG);
        ctrlMap.put(char.class, FOR_CHAR);
        ctrlMap.put(short.class, FOR_SHORT);
        ctrlMap.put(byte.class, FOR_BYTE);
        ctrlMap.put(float.class, FOR_FLOAT);
        ctrlMap.put(double.class, FOR_DOUBLE);
        ctrlMap.put(boolean.class, FOR_BOOLEAN);
        ctrlMap.put(Object.class, FOR_OBJECT);
    }
}
