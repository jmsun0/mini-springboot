package com.sjm.core.util;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sjm.core.json.JSONArray;
import com.sjm.core.json.JSONObject;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Converters {
    public static void main(String[] args) {

    }

    /**
     * 将对象转为指定类型
     * 
     * @param value 待转换的对象
     * @param type 转换的类型
     * @return 返回转换结果，返回空表示转换失败
     */
    public static <T> T convert(Object value, Class<T> type) {
        return valueOf(type).convert(value);
    }

    public static <T> Converter<T, Object> valueOf(Class<T> type) {
        Converter conv = converterMap.get(type);
        if (conv == null) {
            if (type.isArray())
                conv = ToArray(type.getComponentType());
            else if (type.isEnum()) {
                conv = ToEnum(type);
            } else {
                final Reflection.IClass clazz = Reflection.forClass(type);
                if (clazz.isExtendsOf(List.class)) {
                    conv = new Converter() {
                        @Override
                        public Object convert(Object data) {
                            List list = (List) clazz.getCreator().newInstance();
                            list.addAll(ToList.convert(data));
                            return list;
                        }
                    };
                } else if (clazz.isExtendsOf(Map.class)) {
                    conv = new Converter() {
                        @Override
                        public Object convert(Object data) {
                            Map map = (Map) clazz.getCreator().newInstance();
                            map.putAll(ToMap.convert(data));
                            return map;
                        }
                    };
                } else
                    conv = ToObject(type);
            }
            converterMap.put(type, conv);
        }
        return conv;
    }

    public static Map<Type, Converter<?, ?>> getConverterMap() {
        return converterMap;
    }

    public interface ConverterContext {
        public <T> Converter<T, Object> getConverter(Type type);
    }

    public static final ConverterContext DEFAULT_CONTEXT = new ConverterContext() {
        @Override
        public <T> Converter<T, Object> getConverter(Type type) {
            Class<?> clazz;
            if (type instanceof Class)
                clazz = (Class<?>) type;
            else
                clazz = Reflection.forType(type).getClazz();
            return (Converter<T, Object>) valueOf(clazz);
        }
    };

    public static <T1, T2, T3> Converter<T1, T3> link(final Converter<T1, ? super T2> c1,
            final Converter<T2, T3> c2) {
        return new Converter<T1, T3>() {
            @Override
            public T1 convert(T3 data) {
                return c1.convert(c2.convert(data));
            }
        };
    }

    public static <T1, T2, T3, T4> Converter<T1, T4> link(final Converter<T1, ? super T2> c1,
            final Converter<T2, ? super T3> c2, final Converter<T3, T4> c3) {
        return new Converter<T1, T4>() {
            @Override
            public T1 convert(T4 data) {
                return c1.convert(c2.convert(c3.convert(data)));
            }
        };
    }

    public static <T1, T2, T3, T4, T5> Converter<T1, T5> link(final Converter<T1, ? super T2> c1,
            final Converter<T2, ? super T3> c2, final Converter<T3, ? super T4> c3,
            final Converter<T4, T5> c4) {
        return new Converter<T1, T5>() {
            @Override
            public T1 convert(T5 data) {
                return c1.convert(c2.convert(c3.convert(c4.convert(data))));
            }
        };
    }

    public static <D, S> Converter<D, S> map(final Map<S, D> map, final D defaultValue) {
        return new Converter<D, S>() {
            @Override
            public D convert(S data) {
                D v = map.get(data);
                return v == null ? defaultValue : v;
            }
        };
    }

    public static <K> Converter<K, K> map(final Map<K, K> map) {
        return new Converter<K, K>() {
            @Override
            public K convert(K data) {
                K v = map.get(data);
                return v == null ? data : v;
            }
        };
    }

    public static <D, S> Converter<D, S> nullWithDefault(final Converter<D, S> conv,
            final D defaultValue) {
        return new Converter<D, S>() {
            @Override
            public D convert(S data) {
                D v = conv.convert(data);
                return v == null ? defaultValue : v;
            }
        };
    }

    public static final Converter CollectionSize = new Converter<Integer, Collection>() {
        @Override
        public Integer convert(Collection data) {
            return data.size();
        }
    };
    public static final Converter NotConvert = new Converter() {
        @Override
        public Object convert(Object data) {
            return data;
        }
    };

    public static <D, S> Converter<D, S> notConvert() {
        return NotConvert;
    }

    public static final Converter<Integer, Object> ToInteger = new Converter<Integer, Object>() {
        @Override
        public Integer convert(Object data) {
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
    };
    public static final Converter<Long, Object> ToLong = new Converter<Long, Object>() {
        @Override
        public Long convert(Object data) {
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
            }
            return null;
        }
    };
    public static final Converter<Boolean, Object> ToBoolean = new Converter<Boolean, Object>() {
        @Override
        public Boolean convert(Object data) {
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
    };
    public static final Converter<Character, Object> ToCharacter =
            new Converter<Character, Object>() {
                @Override
                public Character convert(Object data) {
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
            };
    public static final Converter<Short, Object> ToShort = new Converter<Short, Object>() {
        @Override
        public Short convert(Object data) {
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
    };
    public static final Converter<Byte, Object> ToByte = new Converter<Byte, Object>() {
        @Override
        public Byte convert(Object data) {
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
    };
    public static final Converter<Float, Object> ToFloat = new Converter<Float, Object>() {
        @Override
        public Float convert(Object data) {
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
    };
    public static final Converter<Double, Object> ToDouble = new Converter<Double, Object>() {
        @Override
        public Double convert(Object data) {
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
    };
    public static final Converter<Integer, Object> Toint = nullWithDefault(ToInteger, 0);
    public static final Converter<Long, Object> Tolong = nullWithDefault(ToLong, 0L);
    public static final Converter<Boolean, Object> Toboolean = nullWithDefault(ToBoolean, false);
    public static final Converter<Character, Object> Tochar = nullWithDefault(ToCharacter, '\0');
    public static final Converter<Short, Object> Toshort = nullWithDefault(ToShort, (short) 0);
    public static final Converter<Byte, Object> Tobyte = nullWithDefault(ToByte, (byte) 0);
    public static final Converter<Float, Object> Tofloat = nullWithDefault(ToFloat, 0f);
    public static final Converter<Double, Object> Todouble = nullWithDefault(ToDouble, 0.0);
    public static final Converter<String, Object> ToString = new Converter<String, Object>() {
        @Override
        public String convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof String)
                return (String) data;
            if (data instanceof byte[])
                return new String(Strings.encodeBase64((byte[]) data));
            if (data instanceof char[])
                return new String((char[]) data);
            if (data instanceof Character)
                return data.toString();
            return JSONObject.toJSONString(data);
        }
    };
    public static final Converter<JSONObject, Object> ToJSONObject =
            new Converter<JSONObject, Object>() {
                @Override
                public JSONObject convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof JSONObject)
                        return (JSONObject) data;
                    if (data instanceof String)
                        return JSONObject.parseObject((String) data);
                    if (data instanceof Map)
                        return new JSONObject((Map<String, Object>) data);
                    // return new JSONObject(data);
                    return JSONObject.parseObject(JSONObject.toJSONString(data));
                }
            };
    public static final Converter<JSONArray, Object> ToJSONArray =
            new Converter<JSONArray, Object>() {
                @Override
                public JSONArray convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof JSONArray)
                        return (JSONArray) data;
                    if (data instanceof String)
                        return JSONObject.parseArray((String) data);
                    if (data instanceof List)
                        return new JSONArray((List) data);
                    if (data.getClass().isArray())
                        return new JSONArray(Lists.from(data));
                    return null;
                }
            };
    public static final Converter<byte[], Object> ToBytes = new Converter<byte[], Object>() {
        @Override
        public byte[] convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof byte[])
                return (byte[]) data;
            if (data instanceof String)
                return Strings.decodeBase64((String) data);
            return null;
        }
    };
    public static final Converter<char[], Object> ToChars = new Converter<char[], Object>() {
        @Override
        public char[] convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof char[])
                return (char[]) data;
            if (data instanceof String)
                return ((String) data).toCharArray();
            return null;
        }
    };
    public static final Converter<BigDecimal, Object> ToBigDecimal =
            new Converter<BigDecimal, Object>() {
                @Override
                public BigDecimal convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof BigDecimal)
                        return (BigDecimal) data;
                    if (data instanceof BigInteger)
                        return new BigDecimal((BigInteger) data);
                    if (data instanceof Float)
                        return new BigDecimal((Float) data);
                    if (data instanceof Double)
                        return new BigDecimal((Double) data);
                    if (data instanceof Long)
                        return new BigDecimal((Long) data);
                    if (data instanceof Number)
                        return new BigDecimal(((Number) data).intValue());
                    if (data instanceof String)
                        return new BigDecimal((String) data);
                    return null;
                }
            };
    public static final Converter<BigInteger, Object> ToBigInteger =
            new Converter<BigInteger, Object>() {
                @Override
                public BigInteger convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof BigInteger)
                        return (BigInteger) data;
                    if (data instanceof BigDecimal)
                        return ((BigDecimal) data).toBigInteger();
                    if (data instanceof Number)
                        return new BigInteger(((Number) data).toString());
                    if (data instanceof String)
                        return new BigInteger(data.toString());
                    return null;
                }
            };
    public static final Converter<Date, Object> ToDate = new Converter<Date, Object>() {
        @Override
        public Date convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof Date)
                return (Date) data;
            if (data instanceof Number)
                return new Date(((Number) data).longValue());
            if (data instanceof String)
                return new Date(Long.parseLong((String) data));
            return null;
        }
    };
    public static final Converter<java.sql.Date, Object> ToSQLDate =
            new Converter<java.sql.Date, Object>() {
                @Override
                public java.sql.Date convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof java.sql.Date)
                        return (java.sql.Date) data;
                    if (data instanceof Number)
                        return new java.sql.Date(((Number) data).longValue());
                    if (data instanceof String)
                        return new java.sql.Date(Long.parseLong((String) data));
                    if (data instanceof Date)
                        return new java.sql.Date(((Date) data).getTime());
                    return null;
                }
            };
    public static final Converter<Timestamp, Object> ToTimestamp =
            new Converter<Timestamp, Object>() {
                @Override
                public Timestamp convert(Object data) {
                    if (data == null)
                        return null;
                    if (data instanceof Timestamp)
                        return (Timestamp) data;
                    if (data instanceof Date)
                        return new Timestamp(((Date) data).getTime());
                    if (data instanceof Number)
                        return new Timestamp(((Number) data).longValue());
                    if (data instanceof String)
                        return new Timestamp(Long.parseLong((String) data));
                    return null;
                }
            };
    public static final Converter<File, Object> ToFile = new Converter<File, Object>() {
        @Override
        public File convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof File)
                return (File) data;
            if (data instanceof String)
                return new File((String) data);
            return null;
        }
    };
    public static final Converter<Class<?>, Object> ToClass = new Converter<Class<?>, Object>() {
        @Override
        public Class<?> convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof Class<?>)
                return (Class<?>) data;
            if (data instanceof String) {
                try {
                    return Reflection.forName((String) data).getClazz();
                } catch (Exception e) {
                }
            }
            return null;
        }
    };
    public static final Converter<List, Object> ToList = new Converter<List, Object>() {
        @Override
        public List convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof List)
                return (List) data;
            if (data.getClass().isArray())
                return Lists.from(data);
            if (data instanceof String)
                return Lists.from((String) data);
            return null;
        }
    };
    public static final Converter<Map, Object> ToMap = new Converter<Map, Object>() {
        @Override
        public Map convert(Object data) {
            if (data == null)
                return null;
            if (data instanceof Map)
                return (Map) data;
            if (data instanceof String)
                return JSONObject.parseObject((String) data);
            return null;
        }
    };

    public static Converter<Object, Object> ToArray(final Class<?> type) {
        return new Converter<Object, Object>() {
            @Override
            public Object convert(Object data) {
                if (data == null)
                    return null;
                Class<?> ac = data.getClass();
                if (ac.isArray()) {
                    if (ac.getComponentType() == type)
                        return data;
                    return Lists.toArray(Lists.convert(Lists.from(data), valueOf(type)), type);
                }
                if (data instanceof List)
                    return Lists.toArray(Lists.convert((List) data, valueOf(type)), type);
                if (data instanceof String)
                    return Lists.toArray(
                            Lists.convert(JSONObject.parseArray((String) data), valueOf(type)),
                            type);
                return null;
            }
        };
    }

    public static <T> Converter<List<T>, Object> ToList(final Class<T> cType) {
        return new Converter<List<T>, Object>() {
            @Override
            public List<T> convert(Object data) {
                if (data == null)
                    return null;
                if (data instanceof List)
                    return Lists.toArrayList(Lists.convert((List) data, valueOf(cType)));
                if (data instanceof String)
                    return JSONObject.parseArray((String) data, cType);
                if (data.getClass().isArray())
                    return Lists.toArrayList(Lists.convert(Lists.from(data), valueOf(cType)));
                return null;
            }
        };
    }

    public static <T> Converter<T, Object> ToObject(final Class<T> type) {
        return new Converter<T, Object>() {
            @Override
            public T convert(Object data) {
                if (data == null)
                    return null;
                if (type.isInstance(data))
                    return (T) data;
                if (data instanceof JSONObject)
                    return ((JSONObject) data).toJavaObject(type);
                if (data instanceof Map)
                    return new JSONObject((Map<String, Object>) data).toJavaObject(type);
                if (data instanceof String)
                    return JSONObject.parseObject((String) data, type);
                return null;
            }
        };
    }


    private static <T> Converter<T, Object> ToEnum(Class<T> type) {
        final Enum[] values = (Enum[]) type.getEnumConstants();
        final Map<String, Enum> map = new HashMap<>();
        for (Enum value : values)
            map.put(value.name(), value);
        return new Converter<T, Object>() {
            @Override
            public T convert(Object data) {
                if (data == null)
                    return null;
                if (data instanceof String)
                    return (T) map.get((String) data);
                if (data instanceof Integer)
                    return (T) values[(int) data];
                return null;
            }
        };
    }

    private static Map<Type, Converter<?, ?>> converterMap = new HashMap<>();

    static {
        converterMap.put(Integer.class, ToInteger);
        converterMap.put(Long.class, ToLong);
        converterMap.put(Short.class, ToShort);
        converterMap.put(Byte.class, ToByte);
        converterMap.put(Character.class, ToCharacter);
        converterMap.put(Boolean.class, ToBoolean);
        converterMap.put(Float.class, ToFloat);
        converterMap.put(Double.class, ToDouble);

        converterMap.put(int.class, Toint);
        converterMap.put(long.class, Tolong);
        converterMap.put(short.class, Toshort);
        converterMap.put(byte.class, Tobyte);
        converterMap.put(char.class, Tochar);
        converterMap.put(boolean.class, Toboolean);
        converterMap.put(float.class, Tofloat);
        converterMap.put(double.class, Todouble);

        converterMap.put(int[].class, ToArray(int.class));
        converterMap.put(long[].class, ToArray(long.class));
        converterMap.put(short[].class, ToArray(short.class));
        converterMap.put(boolean[].class, ToArray(boolean.class));
        converterMap.put(float[].class, ToArray(float.class));
        converterMap.put(double[].class, ToArray(double.class));

        converterMap.put(String.class, ToString);
        converterMap.put(JSONObject.class, ToJSONObject);
        converterMap.put(JSONArray.class, ToJSONArray);
        converterMap.put(byte[].class, ToBytes);
        converterMap.put(char[].class, ToChars);
        converterMap.put(BigDecimal.class, ToBigDecimal);
        converterMap.put(BigInteger.class, ToBigInteger);
        converterMap.put(Date.class, ToDate);
        converterMap.put(java.sql.Date.class, ToSQLDate);
        converterMap.put(Timestamp.class, ToTimestamp);
        converterMap.put(File.class, ToFile);
        converterMap.put(Class.class, ToClass);

        converterMap.put(List.class, ToList);
        converterMap.put(Collection.class, ToList);
        converterMap.put(Iterable.class, ToList);

        converterMap.put(Map.class, ToMap);

        converterMap.put(Object.class, NotConvert);
        converterMap.put(void.class, NotConvert);
        converterMap.put(Void.class, NotConvert);
    }
}
