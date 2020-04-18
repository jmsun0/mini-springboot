package com.sjm.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class Misc {
    public static boolean DEBUG = true;
    public static final Comparator<Object> DEFAULT_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }
    };
    public static final Comparator<Object> STRING_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return Strings.compare(o1, null, -1, -1, o2, null, -1, -1);
        }
    };
    public static final Comparator<Object> STRING_NOCASE_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return Strings.compareIgnoreCase(o1, null, -1, -1, o2, null, -1, -1);
        }
    };

    public static <T, Q> Comparator<Q> convertComparator(final Comparator<T> cmp,
            final Converter<? extends T, Q> conv) {
        return new Comparator<Q>() {
            @Override
            public int compare(Q o1, Q o2) {
                return cmp.compare(conv.convert(o1), conv.convert(o2));
            }
        };
    }

    private static long time;

    public static void startRecordTime() {
        time = System.currentTimeMillis();
    }

    public static void showRecordTime() {
        System.out.println(System.currentTimeMillis() - time);
    }

    public static void asserts(boolean exp) {
        if (!exp)
            throw new RuntimeException();
    }

    public static void asserts(boolean exp, Object msg) {
        if (!exp)
            throw new RuntimeException(String.valueOf(msg));
    }

    public static class Box<T> {
        public T value;

        public Box() {}

        public Box(T value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Box && Objects.equals(value, ((Box<T>) obj).value);
        }
    }

    public static class Box2<T1, T2> {
        public T1 value1;
        public T2 value2;

        public Box2() {}

        public Box2(T1 value1, T2 value2) {
            this.value1 = value1;
            this.value2 = value2;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value1) ^ Objects.hashCode(value2);
        }

        @Override
        public boolean equals(Object obj) {
            Box2<T1, T2> box;
            return obj instanceof Box2 && Objects.equals(value1, (box = (Box2<T1, T2>) obj).value1)
                    && Objects.equals(value2, box.value2);
        }
    }

    public static class Box3<T1, T2, T3> {
        public T1 value1;
        public T2 value2;
        public T3 value3;

        public Box3() {}

        public Box3(T1 value1, T2 value2, T3 value3) {
            this.value1 = value1;
            this.value2 = value2;
            this.value3 = value3;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value1) ^ Objects.hashCode(value2) ^ Objects.hashCode(value3);
        }

        @Override
        public boolean equals(Object obj) {
            Box3<T1, T2, T3> box;
            return obj instanceof Box3
                    && Objects.equals(value1, (box = (Box3<T1, T2, T3>) obj).value1)
                    && Objects.equals(value2, box.value2) && Objects.equals(value3, box.value3);
        }
    }

    public static class IntBox {
        public int value;

        public IntBox() {}

        public IntBox(int value) {
            this.value = value;
        }
    }

    public static class DateRange {
        public Date start;
        public Date end;

        public DateRange() {}

        public DateRange(Date start, Date end) {
            this.start = start;
            this.end = end;
        }
    }

    public static void close(AutoCloseable ac) {
        if (ac != null)
            try {
                ac.close();
            } catch (Exception e) {
            }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static <T> boolean isEmpty(Collection<T> col) {
        return col == null || col.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> col) {
        return !isEmpty(col);
    }

    public static boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNotEmpty(Object[] arr) {
        return !isEmpty(arr);
    }

    public static <T> T getProperty(String key, T defaultValue, Class<T> clazz) {
        String value = System.getProperty(key);
        return Misc.isEmpty(value) ? defaultValue : Converters.convert(value, clazz);
    }

    public static String toString(File file) throws IOException {
        try (ByteData data = ByteData.valueOf(file)) {
            return data.toString(null);
        }
    }

    public static String prefix(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(0, index);
    }

    public static String suffix(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(index + 1);
    }

    public static String[] getAllGroup(Pattern pattern, String str) {
        Matcher mc = pattern.matcher(str);
        if (mc.find()) {
            int count = mc.groupCount();
            String[] result = new String[count];
            for (int i = 0; i < count; i++)
                result[i] = mc.group(i + 1);
            return result;
        }
        return null;
    }

    public static final Executor DEAULT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    };

    public interface Consumer<T> {
        public void accept(T t);
    }

    public static <T> void forEach(Iterable<T> arr, Consumer<T> cs) {
        for (T obj : arr)
            cs.accept(obj);
    }

    public static <T> void forEach(T[] arr, Consumer<T> cs) {
        for (T obj : arr)
            cs.accept(obj);
    }

    public static <T, A> void forEach(A arr, ArrayController<T, A> ctr, Consumer<T> cs) {
        if (ctr == null)
            ctr = ArrayController.valueOf(arr);
        for (int i = 0, len = ctr.getLength(arr); i < len; i++)
            cs.accept(ctr.get(arr, i));
    }

    public static <T> void forEach(Object arr, Consumer<T> cs) {
        if (arr == null) {
        } else if (arr instanceof Iterable)
            forEach((Iterable<T>) arr, cs);
        else if (arr instanceof Object[])
            forEach((T[]) arr, cs);
        else if (arr.getClass().isArray()) {
            forEach(arr, null, cs);
        }
    }

    public static <T> T get(Iterable<T> arr, int index, T defaultValue) {
        if (arr == null)
            return defaultValue;
        if (index < 0)
            throw new IndexOutOfBoundsException(index + "");
        int i = 0;
        for (T obj : arr) {
            if (i == index)
                return obj;
            i++;
        }
        return defaultValue;
    }

    public static <T> T get(Object arr, int index, T defaultValue) {
        if (arr == null)
            return defaultValue;
        else if (ArrayController.isArray(arr))
            return Lists.get(arr, null, index, defaultValue);
        else if (arr instanceof Iterable)
            return get((Iterable<T>) arr, index, defaultValue);
        else
            return defaultValue;
    }

    public static InterruptedException sleep(long millis) {
        try {
            Thread.sleep(millis);
            return null;
        } catch (InterruptedException e) {
            return e;
        }
    }

    public static <T> T getFutureResult(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T extends Throwable> T findCause(Throwable error, Class<T> type, int level) {
        for (int i = 0; i < level; i++) {
            if (error == null)
                break;
            if (type.isInstance(error))
                return (T) error;
            error = error.getCause();
        }
        return null;
    }

    public static byte[] toBytes(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeObject(obj);
            return baos.toByteArray();
        }
    }

    public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));) {
            return ois.readObject();
        }
    }
}
