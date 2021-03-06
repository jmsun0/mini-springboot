package com.sjm.core.util.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.sjm.core.util.core.Converter;
import com.sjm.core.util.core.MyStringBuilder;
import com.sjm.core.util.core.Strings;

public class Misc {
    @SuppressWarnings("unchecked")
    public static final Comparator<Object> DEFAULT_COMPARATOR =
            (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
    public static final Executor DEAULT_EXECUTOR = command -> new Thread(command).start();

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

    private static long time;

    public static void startRecordTime() {
        time = System.currentTimeMillis();
    }

    public static void showRecordTime() {
        System.out.println(System.currentTimeMillis() - time);
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

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
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
            throw new RuntimeException(e);
        }
    }

    public static <T> T getProperty(String key, T defaultValue, Class<T> clazz) {
        String value = System.getProperty(key);
        return Strings.isEmpty(value) ? defaultValue : Converter.INSTANCE.convert(value, clazz);
    }
}
