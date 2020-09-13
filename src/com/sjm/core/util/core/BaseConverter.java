package com.sjm.core.util.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Java基本数据类型之间的转换
 */
public class BaseConverter {
    /**
     * 获取目标类型转换器
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<Object, T> getConverter(Class<T> clazz) {
        return (Function<Object, T>) converters.get(clazz);
    }

    /**
     * 转换为指定基本数据类型
     */
    public static <T> T convert(Object data, Class<T> clazz) {
        return getConverter(clazz).apply(data);
    }

    private static final Integer ZERO_INTEGER = 0;
    private static final Long ZERO_LONG = 0l;
    private static final Short ZERO_SHORT = 0;
    private static final Byte ZERO_BYTE = 0;
    private static final Character ZERO_CHARACTER = 0;
    private static final Boolean ZERO_BOOLEAN = false;
    private static final Float ZERO_FLOAT = 0f;
    private static final Double ZERO_DOUBLE = 0.0;

    private static <T> T nullToDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static final Map<Class<?>, Function<?, ?>> converters = new HashMap<>();
    static {
        converters.put(Integer.class, BaseConverter::toInteger);
        converters.put(Long.class, BaseConverter::toLong);
        converters.put(Short.class, BaseConverter::toShort);
        converters.put(Byte.class, BaseConverter::toByte);
        converters.put(Character.class, BaseConverter::toCharacter);
        converters.put(Boolean.class, BaseConverter::toBoolean);
        converters.put(Float.class, BaseConverter::toFloat);
        converters.put(Double.class, BaseConverter::toDouble);
        converters.put(String.class, BaseConverter::toString);

        converters.put(int.class, BaseConverter::toint);
        converters.put(long.class, BaseConverter::tolong);
        converters.put(short.class, BaseConverter::toshort);
        converters.put(byte.class, BaseConverter::tobyte);
        converters.put(char.class, BaseConverter::tochar);
        converters.put(boolean.class, BaseConverter::toboolean);
        converters.put(float.class, BaseConverter::tofloat);
        converters.put(double.class, BaseConverter::todouble);
    }

    public static Integer toInteger(Object data) {
        if (data != null) {
            if (data instanceof Integer)
                return (Integer) data;
            if (data instanceof Number)
                return ((Number) data).intValue();
            if (data instanceof String)
                try {
                    return Integer.parseInt((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? 1 : 0;
        }
        return null;
    }

    public static Long toLong(Object data) {
        if (data != null) {
            if (data instanceof Long)
                return (Long) data;
            if (data instanceof Number)
                return ((Number) data).longValue();
            if (data instanceof String)
                try {
                    return Long.parseLong((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? 1L : 0L;
            if (data instanceof Date)
                return ((Date) data).getTime();

        }
        return null;
    }

    public static Boolean toBoolean(Object data) {
        if (data != null) {
            if (data instanceof Boolean)
                return (Boolean) data;
            if (data instanceof Number)
                return ((Number) data).intValue() != 0;
            if (data instanceof String) {
                String s = (String) data;
                if (s.equalsIgnoreCase("true"))
                    return true;
                else if (s.equalsIgnoreCase("false"))
                    return false;
            }
        }
        return null;
    }

    public static Character toCharacter(Object data) {
        if (data != null) {
            if (data instanceof Character)
                return (Character) data;
            if (data instanceof Number)
                return (char) ((Number) data).shortValue();
            if (data instanceof String) {
                String s = (String) data;
                if (!s.isEmpty())
                    return s.charAt(0);
            }
        }
        return null;
    }

    public static Short toShort(Object data) {
        if (data != null) {
            if (data instanceof Short)
                return (Short) data;
            if (data instanceof Number)
                return ((Number) data).shortValue();
            if (data instanceof String)
                try {
                    return Short.parseShort((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? (short) 1 : (short) 0;
        }
        return null;
    }

    public static Byte toByte(Object data) {
        if (data != null) {
            if (data instanceof Byte)
                return (Byte) data;
            if (data instanceof Number)
                return ((Number) data).byteValue();
            if (data instanceof String)
                try {
                    return Byte.parseByte((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? (byte) 1 : (byte) 0;
        }
        return null;
    }

    public static Float toFloat(Object data) {
        if (data != null) {
            if (data instanceof Float)
                return (Float) data;
            if (data instanceof Number)
                return ((Number) data).floatValue();
            if (data instanceof String)
                try {
                    return Float.parseFloat((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? 1f : 0f;
        }
        return null;
    }

    public static Double toDouble(Object data) {
        if (data != null) {
            if (data instanceof Double)
                return (Double) data;
            if (data instanceof Number)
                return ((Number) data).doubleValue();
            if (data instanceof String)
                try {
                    return Double.parseDouble((String) data);
                } catch (Exception e) {
                }
            if (data instanceof Boolean)
                return ((Boolean) data) ? 1.0 : 0.0;
        }
        return null;
    }

    public static String toString(Object data) {
        if (data != null) {
            if (data instanceof String)
                return (String) data;
            return data.toString();
        }
        return null;
    }

    public static Integer toint(Object data) {
        return nullToDefault(toInteger(data), ZERO_INTEGER);
    }

    public static Long tolong(Object data) {
        return nullToDefault(toLong(data), ZERO_LONG);
    }

    public static Short toshort(Object data) {
        return nullToDefault(toShort(data), ZERO_SHORT);
    }

    public static Byte tobyte(Object data) {
        return nullToDefault(toByte(data), ZERO_BYTE);
    }

    public static Character tochar(Object data) {
        return nullToDefault(toCharacter(data), ZERO_CHARACTER);
    }

    public static Boolean toboolean(Object data) {
        return nullToDefault(toBoolean(data), ZERO_BOOLEAN);
    }

    public static Float tofloat(Object data) {
        return nullToDefault(toFloat(data), ZERO_FLOAT);
    }

    public static Double todouble(Object data) {
        return nullToDefault(toDouble(data), ZERO_DOUBLE);
    }
}
