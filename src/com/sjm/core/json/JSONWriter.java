package com.sjm.core.json;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sjm.core.util.ArrayController;
import com.sjm.core.util.MyStringBuilder;
import com.sjm.core.util.Reflection;


public abstract class JSONWriter<T> {
    public abstract void write(T value, MyStringBuilder sb);

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeObject(Object value, MyStringBuilder sb) {
        if (value == null) {
            sb.appendNull();
            return;
        }
        Class<?> cls = value.getClass();
        JSONWriter h = getWriter(cls);
        if (h != null) {
            h.write(value, sb);
            return;
        }
        if (cls.isArray())
            writeArray(value, sb);
        else if (value instanceof Collection)
            writeCollection((Collection<Object>) value, sb);
        else if (value instanceof Map)
            writeMap((Map<String, Object>) value, sb);
        else
            writeJavaObject(value, sb);
    }

    public static void writeList(List<Object> value, MyStringBuilder sb) {
        sb.append('[');
        int size = value.size();
        if (size != 0) {
            for (int i = 0; i < size; i++) {
                writeObject(value.get(i), sb);
                sb.append(',');
            }
            sb.deleteEnd();
        }
        sb.append(']');
    }

    public static void writeMap(Map<String, Object> value, MyStringBuilder sb) {
        sb.append('{');
        int beforeLen = sb.length();
        if (!value.isEmpty()) {
            for (Map.Entry<String, Object> e : value.entrySet()) {
                Object v = e.getValue();
                if (v != null) {
                    sb.append('\"').appendEscape(e.getKey(), null, -1, -1).append('\"');
                    sb.append(':');
                    writeObject(v, sb);
                    sb.append(',');
                }
            }
        }
        if (sb.length() != beforeLen)
            sb.deleteEnd();
        sb.append('}');
    }

    public static void writeCollection(Collection<Object> value, MyStringBuilder sb) {
        sb.append('[');
        if (!value.isEmpty()) {
            for (Object obj : value) {
                writeObject(obj, sb);
                sb.append(',');
            }
            sb.deleteEnd();
        }
        sb.append(']');
    }

    public static void writeArray(Object value, MyStringBuilder sb) {
        ArrayController<Object, Object> ctr = ArrayController.valueOf(value);
        int size = ctr.getLength(value);
        sb.append('[');
        if (size != 0) {
            for (int i = 0; i < size; i++) {
                writeObject(ctr.get(value, i), sb);
                sb.append(',');
            }
            sb.deleteEnd();
        }
        sb.append(']');
    }

    public static void writeJavaObject(Object value, MyStringBuilder sb) {
        sb.append('{');
        Collection<Reflection.Getter> gs =
                Reflection.forClass(value.getClass()).getGetterMap().values();
        int beforeLen = sb.length();
        if (!gs.isEmpty()) {
            for (Reflection.Getter g : gs) {
                Object v = g.get(value);
                if (v != null) {
                    sb.append('\"').appendEscape(g.getName(), null, -1, -1).append('\"');
                    sb.append(':');
                    writeObject(v, sb);
                    sb.append(',');
                }
            }
        }
        if (sb.length() != beforeLen)
            sb.deleteEnd();
        sb.append('}');
    }

    private static Map<Class<?>, JSONWriter<?>> writerMap = new HashMap<Class<?>, JSONWriter<?>>();

    public static void setWriter(Class<?> clazz, JSONWriter<?> writer) {
        writerMap.put(clazz, writer);
    }

    @SuppressWarnings("unchecked")
    public static <T> JSONWriter<T> getWriter(Class<T> clazz) {
        return (JSONWriter<T>) writerMap.get(clazz);
    }

    public static JSONWriter<JSONObject> ForJSONObject = new JSONWriter<JSONObject>() {
        @Override
        public void write(JSONObject value, MyStringBuilder sb) {
            writeMap(value, sb);
        }
    };
    public static JSONWriter<JSONArray> ForJSONArray = new JSONWriter<JSONArray>() {
        @Override
        public void write(JSONArray value, MyStringBuilder sb) {
            writeList(value, sb);
        }
    };

    public static JSONWriter<Integer> ForInteger = new JSONWriter<Integer>() {
        @Override
        public void write(Integer value, MyStringBuilder sb) {
            sb.append((int) value);
        }
    };
    public static JSONWriter<Long> ForLong = new JSONWriter<Long>() {
        @Override
        public void write(Long value, MyStringBuilder sb) {
            sb.append((long) value);
        }
    };
    public static JSONWriter<Character> ForCharacter = new JSONWriter<Character>() {
        @Override
        public void write(Character value, MyStringBuilder sb) {
            sb.append('\"').appendEscape((char) value).append('\"');
        }
    };
    public static JSONWriter<Short> ForShort = new JSONWriter<Short>() {
        @Override
        public void write(Short value, MyStringBuilder sb) {
            sb.append((int) value);
        }
    };
    public static JSONWriter<Byte> ForByte = new JSONWriter<Byte>() {
        @Override
        public void write(Byte value, MyStringBuilder sb) {
            sb.append((int) value);
        }
    };
    public static JSONWriter<Float> ForFloat = new JSONWriter<Float>() {
        @Override
        public void write(Float value, MyStringBuilder sb) {
            sb.append(new BigDecimal(value.toString()).toString());// TODO
        }
    };
    public static JSONWriter<Double> ForDouble = new JSONWriter<Double>() {
        @Override
        public void write(Double value, MyStringBuilder sb) {
            sb.append(new BigDecimal(value.toString()).toString());// TODO
        }
    };
    public static JSONWriter<Boolean> ForBoolean = new JSONWriter<Boolean>() {
        @Override
        public void write(Boolean value, MyStringBuilder sb) {
            sb.append((boolean) value);
        }
    };
    public static JSONWriter<BigDecimal> ForBigDecimal = new JSONWriter<BigDecimal>() {
        @Override
        public void write(BigDecimal value, MyStringBuilder sb) {
            sb.append(value.toString());// TODO
        }
    };
    public static JSONWriter<BigInteger> ForBigInteger = new JSONWriter<BigInteger>() {
        @Override
        public void write(BigInteger value, MyStringBuilder sb) {
            sb.append(value.toString());// TODO
        }
    };
    public static JSONWriter<String> ForString = new JSONWriter<String>() {
        @Override
        public void write(String value, MyStringBuilder sb) {
            sb.append('\"').appendEscape(value, null, -1, -1).append('\"');
        }
    };
    public static JSONWriter<byte[]> ForBytes = new JSONWriter<byte[]>() {
        @Override
        public void write(byte[] value, MyStringBuilder sb) {
            sb.append('\"').appendBase64(value).append('\"');
        }
    };
    public static JSONWriter<char[]> ForChars = new JSONWriter<char[]>() {
        @Override
        public void write(char[] value, MyStringBuilder sb) {
            sb.append('\"').appendEscape(value, null, -1, -1).append('\"');
        }
    };
    public static JSONWriter<Object> ForDoubleQuotation = new JSONWriter<Object>() {
        @Override
        public void write(Object value, MyStringBuilder sb) {
            sb.append('\"').appendEscape(value, null, -1, -1).append('\"');
        }
    };
    public static JSONWriter<Object> ForSingleQuotation = new JSONWriter<Object>() {
        @Override
        public void write(Object value, MyStringBuilder sb) {
            sb.append('\'').appendEscape(value, null, -1, -1).append('\'');
        }
    };
    public static JSONWriter<Date> ForDate = new JSONWriter<Date>() {
        @Override
        public void write(Date value, MyStringBuilder sb) {
            sb.append(value.getTime());
        }
    };
    public static JSONWriter<Calendar> ForCalendar = new JSONWriter<Calendar>() {
        @Override
        public void write(Calendar value, MyStringBuilder sb) {
            sb.append(value.getTimeInMillis());
        }
    };
    public static JSONWriter<File> ForFile = new JSONWriter<File>() {
        @Override
        public void write(File value, MyStringBuilder sb) {
            sb.append('\'').appendEscape(value.getPath(), null, -1, -1).append('\'');
        }
    };
    public static JSONWriter<Class<?>> ForClass = new JSONWriter<Class<?>>() {
        @Override
        public void write(Class<?> value, MyStringBuilder sb) {
            sb.append('\'').append(value.getName()).append('\'');
        }
    };
    static {
        setWriter(String.class, JSONWriter.ForDoubleQuotation);
        setWriter(Integer.class, JSONWriter.ForInteger);
        setWriter(Boolean.class, JSONWriter.ForBoolean);
        setWriter(Long.class, JSONWriter.ForLong);
        setWriter(BigDecimal.class, JSONWriter.ForBigDecimal);
        setWriter(BigInteger.class, JSONWriter.ForBigInteger);
        setWriter(Byte.class, JSONWriter.ForByte);
        setWriter(Short.class, JSONWriter.ForShort);
        setWriter(Character.class, JSONWriter.ForCharacter);
        setWriter(Double.class, JSONWriter.ForDouble);
        setWriter(Float.class, JSONWriter.ForFloat);
        setWriter(byte[].class, JSONWriter.ForBytes);
        setWriter(char[].class, JSONWriter.ForDoubleQuotation);
        setWriter(Date.class, JSONWriter.ForDate);
        setWriter(java.sql.Date.class, JSONWriter.ForDate);
        setWriter(Timestamp.class, JSONWriter.ForDate);
        setWriter(Calendar.class, JSONWriter.ForCalendar);
        setWriter(File.class, JSONWriter.ForFile);
        setWriter(Class.class, JSONWriter.ForClass);
        setWriter(JSONObject.class, ForJSONObject);
        setWriter(JSONArray.class, ForJSONArray);
    }
}
