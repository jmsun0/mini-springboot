package com.sjm.core.util.core;


import java.io.File;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sjm.core.util.misc.Analyzer.Pattern;

@SuppressWarnings({"unchecked"})
public class Converter {
    private Converter parent;
    private Map<Type, Function<?, ?>> converters = new HashMap<>();
    private Map<Type, Function<?, ?>> baseConverters = new HashMap<>();
    private Reflection reflection = Reflection.INSTANCE;

    public Converter(Converter parent) {
        this.parent = parent;
    }

    public void addBaseConverter(Type type, Function<?, ?> func) {
        baseConverters.put(type, func);
    }

    public Function<Object, Object> getConverter(Type type) {
        Function<?, ?> func = converters.get(type);
        if (func == null) {
            synchronized (this) {
                if ((func = converters.get(type)) == null) {
                    func = doGetConverter(type);
                    if (func != null)
                        converters.put(type, func);
                }
            }
        }
        return (Function<Object, Object>) func;
    }

    public Object convert(Object data, Type type) {
        Function<Object, Object> func = getConverter(type);
        if (func == null)
            throw new IllegalArgumentException("unsupported type '" + type.getTypeName() + "'");
        return func.apply(data);
    }

    private Function<?, ?> getBaseConverter(Type type) {
        Function<?, ?> func = baseConverters.get(type);
        if (func == null && parent != null)
            func = parent.getBaseConverter(type);
        return func;
    }

    private Function<?, ?> doGetConverter(Type type) {
        Function<?, ?> func = getBaseConverter(type);
        if (func != null)
            return func;
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                return getArrayConverter(clazz, componentType, componentType);
            }
            return getGenericConverter(clazz, clazz);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            return getArrayConverter(type, reflection.getRawType(gat).getComponentType(),
                    gat.getGenericComponentType());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Class<?> clazz = (Class<?>) pt.getRawType();
            return getGenericConverter(clazz, type);
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            return getConverter(tv.getBounds()[0]);
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            return getConverter(wt.getUpperBounds()[0]);
        } else
            throw new UnsupportedOperationException();
    }

    private Function<?, ?> getArrayConverter(Type type, Class<?> componentRawType,
            boolean isComponentBaseType, Function<Object, Object> componentConverter,
            ArrayController<Object, Object> ctr) {
        if (componentConverter == null)
            return null;
        return data -> toArray(data, type, componentRawType, isComponentBaseType,
                componentConverter, ctr);
    }

    private Function<?, ?> getArrayConverter(Type type, Class<?> componentRawType,
            Type componentType) {
        Function<?, ?> componentConverter = getBaseConverter(componentType);
        boolean isComponentBaseType = true;
        if (componentConverter == null) {
            isComponentBaseType = false;
            componentConverter = getConverter(componentType);
        }
        return getArrayConverter(componentType, componentRawType, isComponentBaseType,
                (Function<Object, Object>) componentConverter,
                ArrayController.valueOf(componentRawType));
    }

    private Function<?, ?> getCollectionConverter(Type type,
            Function<Object, Object> componentConverter, Supplier<Collection<Object>> supplier) {
        if (componentConverter == null)
            return null;
        return data -> toCollection(data, type, componentConverter, supplier);
    }

    private Function<?, ?> getCollectionConverter(Type type, Class<?> rawType, Type componentType,
            Supplier<Collection<Object>> defaultSupplier) {
        return getCollectionConverter(type, (Function<Object, Object>) getConverter(componentType),
                rawType.isInterface() ? defaultSupplier
                        : (Supplier<Collection<Object>>) getAllocator(rawType));
    }

    private Function<?, ?> getMapConverter(Type type, Function<Object, Object> keyConverter,
            Function<Object, Object> valueConverter, Supplier<Map<Object, Object>> supplier) {
        if (keyConverter == null || valueConverter == null)
            return null;
        return data -> toMap(data, type, keyConverter, valueConverter, supplier);
    }

    private Function<?, ?> getMapConverter(Type type, Class<?> rawType, Type keyType,
            Type valueType, Supplier<Map<Object, Object>> defaultSupplier) {
        return getMapConverter(valueType, (Function<Object, Object>) getConverter(keyType),
                (Function<Object, Object>) getConverter(valueType),
                rawType.isInterface() ? defaultSupplier
                        : (Supplier<Map<Object, Object>>) getAllocator(rawType));
    }

    private Function<?, ?> getBeanConverter(Type type, Map<String, MySetterInfo> setters,
            Supplier<Object> supplier) {
        return data -> toBean(data, type, setters, supplier);
    }

    private Function<?, ?> getBeanConverter(Type type, Class<?> rawType) {
        return getBeanConverter(rawType, getSetterInfoMap(type),
                (Supplier<Object>) getAllocator(rawType));
    }

    private static final Class<?>[] specialSuperClasses =
            new Class<?>[] {Set.class, Iterable.class, Map.class};

    private Function<?, ?> getGenericConverter(Class<?> rawType, Type type) {
        Type[] types = null;
        int index = -1;
        for (int i = 0; i < specialSuperClasses.length; i++) {
            Class<?> superClass = specialSuperClasses[i];
            if (superClass.isAssignableFrom(rawType)) {
                types = reflection.getGenericTypeMapping(type, superClass);
                index = i;
                break;
            }
        }
        if (index != -1) {
            if (index == 0)
                return getCollectionConverter(type, rawType, types[0], HashSet::new);
            else if (index == 1)
                return getCollectionConverter(type, rawType, types[0], ArrayList::new);
            else
                return getMapConverter(type, rawType, types[0], types[1], HashMap::new);
        } else
            return getBeanConverter(type, rawType);
    }

    private static <T> Supplier<T> getAllocator(Class<T> clazz) {
        return () -> {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Object toArray(Object data, Type type, Class<?> componentRawType,
            boolean isComponentBaseType, Function<Object, Object> componentConverter,
            ArrayController<Object, Object> ctr) {
        if (data == null)
            return null;
        if (data.getClass().isArray())
            return arrayToArray(data, componentRawType, isComponentBaseType, componentConverter,
                    ctr);
        if (data instanceof List)
            return listToArray((List<?>) data, componentConverter, ctr);
        if (data instanceof Collection)
            return collectionToArray((Collection<?>) data, componentConverter, ctr);
        if (data instanceof Iterable)
            return listToArray(Lists.from((Iterable<?>) data), componentConverter, ctr);
        if (data instanceof String)
            return JSON.parseObject((String) data, type);
        return null;
    }

    private Object arrayToArray(Object data, Class<?> componentRawType, boolean isComponentBaseType,
            Function<Object, Object> componentConverter, ArrayController<Object, Object> ctr) {
        Class<?> ac = data.getClass();
        if (isComponentBaseType && ac.getComponentType() == componentRawType)
            return ctr.clone(data);
        ArrayController<Object, Object> srcCtr = ArrayController.valueOf(ac.getComponentType());
        int len = srcCtr.getLength(data);
        Object result = ctr.newInstance(len);
        for (int i = 0; i < len; i++)
            ctr.set(result, i, componentConverter.apply(srcCtr.get(data, i)));
        return result;
    }

    private Object listToArray(List<?> data, Function<Object, Object> componentConverter,
            ArrayController<Object, Object> ctr) {
        Object result = ctr.newInstance(data.size());
        for (int i = 0, len = data.size(); i < len; i++)
            ctr.set(result, i, componentConverter.apply(data.get(i)));
        return result;
    }

    private Object collectionToArray(Collection<?> data,
            Function<Object, Object> componentConverter, ArrayController<Object, Object> ctr) {
        Object result = ctr.newInstance(data.size());
        int i = 0;
        for (Object element : data)
            ctr.set(result, i++, componentConverter.apply(element));
        return result;
    }

    private Object toCollection(Object data, Type type, Function<Object, Object> componentConverter,
            Supplier<Collection<Object>> supplier) {
        if (data == null)
            return null;
        if (data instanceof List)
            return listToCollection((List<?>) data, componentConverter, supplier);
        if (data instanceof Iterable)
            return iterableToCollection((Iterable<?>) data, componentConverter, supplier);
        if (data.getClass().isArray())
            return arrayToCollection(data, componentConverter, supplier);
        if (data instanceof String)
            return JSON.parseObject((String) data, type);
        return null;
    }

    private Object listToCollection(List<?> data, Function<Object, Object> componentConverter,
            Supplier<Collection<Object>> supplier) {
        Collection<Object> col = supplier.get();
        for (int i = 0, len = data.size(); i < len; i++)
            col.add(componentConverter.apply(data.get(i)));
        return col;
    }

    private Object iterableToCollection(Iterable<?> data,
            Function<Object, Object> componentConverter, Supplier<Collection<Object>> supplier) {
        Collection<Object> col = supplier.get();
        for (Object element : data)
            col.add(componentConverter.apply(element));
        return col;
    }

    private Object arrayToCollection(Object data, Function<Object, Object> componentConverter,
            Supplier<Collection<Object>> supplier) {
        ArrayController<Object, Object> ctr =
                ArrayController.valueOf(data.getClass().getComponentType());
        int len = ctr.getLength(data);
        Collection<Object> col = supplier.get();
        for (int i = 0; i < len; i++)
            col.add(componentConverter.apply(ctr.get(data, i)));
        return col;
    }

    private Object toMap(Object data, Type type, Function<Object, Object> keyConverter,
            Function<Object, Object> valueConverter, Supplier<Map<Object, Object>> supplier) {
        if (data == null)
            return null;
        if (data instanceof Map)
            return mapToMap((Map<?, ?>) data, keyConverter, valueConverter, supplier);
        if (data instanceof String)
            return JSON.parseObject((String) data, type);
        return beanToMap(data, keyConverter, valueConverter, supplier);
    }

    private Object mapToMap(Map<?, ?> data, Function<Object, Object> keyConverter,
            Function<Object, Object> valueConverter, Supplier<Map<Object, Object>> supplier) {
        Map<Object, Object> map = supplier.get();
        for (Map.Entry<?, ?> e : data.entrySet())
            map.put(keyConverter.apply(e.getKey()), valueConverter.apply(e.getValue()));
        return map;
    }

    private Object beanToMap(Object data, Function<Object, Object> keyConverter,
            Function<Object, Object> valueConverter, Supplier<Map<Object, Object>> supplier) {
        Map<Object, Object> map = supplier.get();
        Collection<Reflection.GetterInfo> getters =
                reflection.getGettersMap(data.getClass()).values();
        for (Reflection.GetterInfo getter : getters)
            try {
                map.put(keyConverter.apply(getter.name),
                        valueConverter.apply(getter.getter.get(data)));
            } catch (Exception e) {
            }
        return map;
    }

    static class MySetterInfo {
        public Reflection.Setter setter;
        public Function<Object, Object> converter;
    }

    private Object toBean(Object data, Type type, Map<String, MySetterInfo> setters,
            Supplier<Object> supplier) {
        if (data == null)
            return null;
        if (data instanceof Map)
            return mapToBean((Map<?, ?>) data, setters, supplier);
        if (data instanceof String)
            return JSON.parseObject((String) data, type);
        return beanToBean(data, setters, supplier);
    }

    private Map<String, MySetterInfo> getSetterInfoMap(Type type) {
        Class<?> clazz = reflection.getRawType(type);
        Map<String, Reflection.SetterInfo> setterMap = reflection.getSettersMap(clazz);
        Map<String, MySetterInfo> setters = new HashMap<>();
        for (Map.Entry<String, Reflection.SetterInfo> e : setterMap.entrySet()) {
            Reflection.SetterInfo osi = e.getValue();
            MySetterInfo si = new MySetterInfo();
            si.setter = osi.setter;
            si.converter = (Function<Object, Object>) getConverter(reflection
                    .calculateGenericType(type, osi.member.getDeclaringClass(), osi.type));
            setters.put(e.getKey(), si);
        }
        return setters;
    }

    private Object mapToBean(Map<?, ?> data, Map<String, MySetterInfo> setters,
            Supplier<Object> supplier) {
        Object bean = supplier.get();
        for (Map.Entry<?, ?> e : data.entrySet()) {
            MySetterInfo setter = setters.get(String.valueOf(e.getKey()));
            if (setter != null)
                try {
                    setter.setter.set(bean, setter.converter.apply(e.getValue()));
                } catch (Exception ex) {
                }
        }
        return bean;
    }

    private Object beanToBean(Object data, Map<String, MySetterInfo> setters,
            Supplier<Object> supplier) {
        Object bean = supplier.get();
        Collection<Reflection.GetterInfo> getters =
                reflection.getGettersMap(data.getClass()).values();
        for (Reflection.GetterInfo getter : getters) {
            MySetterInfo setter = setters.get(String.valueOf(getter.name));
            if (setter != null)
                try {
                    setter.setter.set(bean, setter.converter.apply(getter.getter.get(data)));
                } catch (Exception e) {
                }
        }
        return bean;
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Static Methods >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    public static byte[] toBytes(Object data) {
        if (data == null)
            return null;
        if (data instanceof byte[])
            return ((byte[]) data).clone();
        if (data instanceof String)
            return Strings.decodeBase64((String) data);
        return null;
    }

    public static char[] toChars(Object data) {
        if (data == null)
            return null;
        if (data instanceof char[])
            return ((char[]) data).clone();
        if (data instanceof String)
            return ((String) data).toCharArray();
        return null;
    }

    public static BigDecimal toBigDecimal(Object data) {
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

    public static BigInteger toBigInteger(Object data) {
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

    public static Date toDate(Object data) {
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

    public static java.sql.Date toSQLDate(Object data) {
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

    public static Timestamp toTimestamp(Object data) {
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

    public static File toFile(Object data) {
        if (data == null)
            return null;
        if (data instanceof File)
            return (File) data;
        if (data instanceof String)
            return new File((String) data);
        return null;
    }

    public static Class<?> toClass(Object data) {
        if (data == null)
            return null;
        if (data instanceof Class<?>)
            return (Class<?>) data;
        if (data instanceof String) {
            try {
                return Class.forName((String) data, false,
                        Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static URL toURL(Object data) {
        if (data == null)
            return null;
        if (data instanceof URL)
            return (URL) data;
        if (data instanceof String)
            try {
                return new URL((String) data);
            } catch (MalformedURLException e) {
            }
        return null;
    }

    public static URI toURI(Object data) {
        if (data == null)
            return null;
        if (data instanceof URI)
            return (URI) data;
        if (data instanceof String)
            try {
                return new URI((String) data);
            } catch (URISyntaxException e) {
            }
        return null;
    }

    public static Pattern toPattern(Object data) {
        if (data == null)
            return null;
        if (data instanceof Pattern)
            return (Pattern) data;
        if (data instanceof CharSequence)
            return Pattern.compile((CharSequence) data);
        return null;
    }

    public static Charset toCharset(Object data) {
        if (data == null)
            return null;
        if (data instanceof Charset)
            return (Charset) data;
        if (data instanceof String)
            return Charset.forName((String) data);
        return null;
    }

    public static final Converter INSTANCE = new Converter(null);
    static {
        INSTANCE.baseConverters.putAll(BaseConverter.converters);
        INSTANCE.addBaseConverter(byte[].class, Converter::toBytes);
        INSTANCE.addBaseConverter(char[].class, Converter::toChars);
        INSTANCE.addBaseConverter(BigDecimal.class, Converter::toBigDecimal);
        INSTANCE.addBaseConverter(BigInteger.class, Converter::toBigInteger);
        INSTANCE.addBaseConverter(Date.class, Converter::toDate);
        INSTANCE.addBaseConverter(java.sql.Date.class, Converter::toSQLDate);
        INSTANCE.addBaseConverter(Timestamp.class, Converter::toTimestamp);
        INSTANCE.addBaseConverter(File.class, Converter::toFile);
        INSTANCE.addBaseConverter(Class.class, Converter::toClass);
        INSTANCE.addBaseConverter(URL.class, Converter::toURL);
        INSTANCE.addBaseConverter(URI.class, Converter::toURI);
        INSTANCE.addBaseConverter(Pattern.class, Converter::toPattern);
        INSTANCE.addBaseConverter(Charset.class, Converter::toCharset);
        INSTANCE.addBaseConverter(Object.class, data -> data);
    }
}
