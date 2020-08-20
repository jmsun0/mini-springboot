package com.sjm.core.util.core;

import java.io.File;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class JSON {
    public static String toJSONString(Object obj) {
        MyStringBuilder sb = new MyStringBuilder();
        Serializers.forAny.serialize(obj, sb);
        return sb.toString();
    }

    public static Object parse(String str, Deserializer deserializer) {
        StringJSONLex lex = new StringJSONLex();
        lex.resetAndNext(str);
        return deserializer.deserialize(lex);
    }

    public static Object parse(String str) {
        return parse(str, Deserializers.forDefault);
    }

    public static Map<String, Object> parseObject(String str) {
        return (Map<String, Object>) parse(str, Deserializers.forDefaultMap);
    }

    public static Object parseObject(String str, Type type) {
        return parse(str, Deserializers.forType(type));
    }

    public static <T> T parseObject(String str, Class<T> clazz) {
        return (T) parseObject(str, (Type) clazz);
    }

    public static List<Object> parseArray(String str) {
        return (List<Object>) parse(str, Deserializers.forDefaultList);
    }

    public static <T> List<T> parseArray(String str, Class<T> clazz) {
        return (List<T>) parse(str,
                Deserializers.forCollection(ArrayList::new, Deserializers.forType(clazz)));
    }

    public static enum Key {
        EOF, TEXT, NUM, LITERAL, //
        TRUE("true"), FALSE("false"), NULL("null"), //
        LBB('{'), RBB('}'), LMB('['), RMB(']'), COMMA(','), COLON(':'),//
        ;

        public final char ch;
        public final String str;

        private Key(char ch, String str) {
            this.ch = ch;
            this.str = str;
        }

        private Key(char ch) {
            this(ch, null);
        }

        private Key(String str) {
            this('\0', str);
        }

        private Key() {
            this('\0', null);
        }
    }

    public interface Serializer {
        public void serialize(Object value, MyStringBuilder sb);
    }

    public interface Deserializer {
        public Object deserialize(JSONLex lex);
    }

    public static class JSONGetterInfo {
        public String name;
        public Reflection.Getter getter;
        public Serializer serializer;
    }

    public static class JSONSetterInfo {
        public Reflection.Setter setter;
        public Deserializer deserializer;
    }

    public static class JSONException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JSONException(String message, Throwable cause) {
            super(message, cause);
        }

        public JSONException(Throwable cause) {
            super(cause);
        }

        public JSONException(String message) {
            super(message);
        }

        public JSONException() {
            super();
        }
    }

    public static class Serializers {
        private static final Map<Type, Serializer> serializers = new HashMap<>();

        public static void add(Serializer serializer, Type... types) {
            for (Type type : types)
                serializers.put(type, serializer);
        }

        public static <T, R> Serializer forMapperObject(Function<T, R> mapper) {
            return (value, sb) -> sb.append(mapper.apply((T) value));
        }

        public static <T> Serializer forMapperString(Function<T, String> mapper) {
            return (value, sb) -> sb.append(mapper.apply((T) value));
        }

        public static <T> Serializer forEscapeString(Function<T, String> mapper) {
            return (value, sb) -> sb.append('\"').appendEscape(mapper.apply((T) value), -1, -1)
                    .append('\"');
        }

        public static <T> Serializer forEscapeChars(Function<T, char[]> mapper) {
            return (value, sb) -> sb.append('\"').appendEscape(mapper.apply((T) value), -1, -1)
                    .append('\"');
        }

        public static <T> Serializer forBase64(Function<T, byte[]> mapper) {
            return (value, sb) -> sb.append('\"').appendBase64(mapper.apply((T) value))
                    .append('\"');
        }

        public static <T> Serializer forDate(Function<T, Date> mapper) {
            return (value, sb) -> {
                Date date = mapper.apply((T) value);
                SimpleDateFormat fmt = Configure.get().dateFormat;
                if (fmt == null)
                    sb.append(date.getTime());
                else
                    sb.append('\"').appendEscape(fmt.format(date), -1, -1).append('\"');
            };
        }

        public static Serializer forArray(Serializer itemSerializer) {
            return (value, sb) -> {
                ArrayController<Object, Object> ctr = ArrayController.valueOf(value);
                int size = ctr.getLength(value);
                sb.append('[');
                if (size != 0) {
                    for (int i = 0; i < size; i++) {
                        itemSerializer.serialize(ctr.get(value, i), sb);
                        sb.append(',');
                    }
                    sb.deleteEnd();
                }
                sb.append(']');
            };
        }

        public static Serializer forIterable(Serializer itemSerializer) {
            return (value, sb) -> {
                sb.append('[');
                int beforeLen = sb.length();
                for (Object item : (Iterable<?>) value) {
                    itemSerializer.serialize(item, sb);
                    sb.append(',');
                }
                if (sb.length() != beforeLen)
                    sb.deleteEnd();
                sb.append(']');
            };
        }

        public static Serializer forMap(Serializer keySerializer, Serializer valueSerializer) {
            return (value, sb) -> {
                Map<?, ?> map = (Map<?, ?>) value;
                sb.append('{');
                int beforeLen = sb.length();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    Object v = e.getValue();
                    if (v != null) {
                        keySerializer.serialize(e.getKey(), sb);
                        sb.append(':');
                        valueSerializer.serialize(v, sb);
                        sb.append(',');
                    }
                }
                if (sb.length() != beforeLen)
                    sb.deleteEnd();
                sb.append('}');
            };
        }

        public static Serializer forBean(Iterable<JSONGetterInfo> getters) {
            return (value, sb) -> {
                sb.append('{');
                int beforeLen = sb.length();
                for (JSONGetterInfo gi : getters) {
                    Object v;
                    try {
                        v = gi.getter.get(value);
                    } catch (Exception e) {
                        throw new JSONException(e);
                    }
                    if (v != null) {
                        sb.append('\"').appendEscape(gi.name, -1, -1).append('\"');
                        sb.append(':');
                        gi.serializer.serialize(v, sb);
                        sb.append(',');
                    }
                }
                if (sb.length() != beforeLen)
                    sb.deleteEnd();
                sb.append('}');
            };
        }

        public static Serializer forBean(Type type) {
            Class<?> clazz = ReflectionSupport.getRawType(type);
            Map<String, Reflection.GetterInfo> getterMap = ReflectionSupport.getGettersMap(clazz);
            List<JSONGetterInfo> getters = new ArrayList<>();
            for (Map.Entry<String, Reflection.GetterInfo> e : getterMap.entrySet()) {
                Reflection.GetterInfo ogi = e.getValue();
                JSONGetterInfo gi = new JSONGetterInfo();
                gi.name = e.getKey();
                gi.getter = ogi.getter;
                gi.serializer = Serializers.forType(ReflectionSupport.calculateGenericType(type,
                        ogi.member.getDeclaringClass(), ogi.type));
                getters.add(gi);
            }
            return forBean(getters);
        }

        public static Serializer forType(Type type) {
            Serializer serializer = serializers.get(type);
            if (serializer == null) {
                synchronized (Serializers.class) {
                    if ((serializer = serializers.get(type)) == null)
                        serializers.put(type, serializer = forTypeWithoutCache(type));
                }
            }
            return serializer;
        }

        private static Serializer forTypeWithoutCache(Type type) {
            if (type instanceof Class) {
                Class<?> clazz = (Class<?>) type;
                if (clazz.isArray())
                    return forArray(forType(clazz.getComponentType()));
                return forGenericType(clazz, clazz);
            } else if (type instanceof GenericArrayType) {
                return forArray(forType(((GenericArrayType) type).getGenericComponentType()));
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Class<?> clazz = (Class<?>) pt.getRawType();
                return forGenericType(clazz, type);
            } else if (type instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) type;
                return forType(tv.getBounds()[0]);
            } else if (type instanceof WildcardType) {
                WildcardType wt = (WildcardType) type;
                return forType(wt.getUpperBounds()[0]);
            }
            throw new UnsupportedOperationException();
        }

        private static final Class<?>[] specialSuperClasses =
                new Class<?>[] {List.class, Iterable.class, Map.class};

        private static Serializer forGenericType(Class<?> rawType, Type type) {
            Type[] types = null;
            int index = -1;
            for (int i = 0; i < specialSuperClasses.length; i++) {
                Class<?> superClass = specialSuperClasses[i];
                if (superClass.isAssignableFrom(rawType)) {
                    types = ReflectionSupport.getGenericTypeMapping(type, superClass);
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                if (index == 0)
                    return forArray(forType(types[0]));
                else if (index == 1)
                    return forIterable(forType(types[0]));
                else
                    return forMap(forType(types[0]), forType(types[1]));
            } else
                return forBean(type);
        }

        public static final Serializer forObject = (value, sb) -> sb.append(value);
        public static final Serializer forNull = (value, sb) -> sb.appendNull();
        public static final Serializer forToString = forMapperString(Object::toString);
        public static final Serializer forEscapeToString = forEscapeString(Object::toString);
        public static final Serializer forAny = (value,
                sb) -> (value == null ? forNull : forType(value.getClass())).serialize(value, sb);
        static {
            add((value, sb) -> sb.append((int) value), int.class, Integer.class);
            add((value, sb) -> sb.append((short) value), short.class, Short.class);
            add((value, sb) -> sb.append((byte) value), byte.class, Byte.class);
            add((value, sb) -> sb.append((long) value), long.class, Long.class);
            add((value, sb) -> sb.append((float) value), float.class, Float.class);
            add((value, sb) -> sb.append((double) value), double.class, Double.class);
            add((value, sb) -> sb.append((boolean) value), boolean.class, Boolean.class);
            add(forEscapeString(v -> String.valueOf((char) v)), char.class, Character.class);
            add(forEscapeString(v -> ((Class<?>) v).getName()), Class.class);
            add(forBase64(v -> (byte[]) v), byte[].class);
            add(forEscapeChars(v -> (char[]) v), char[].class);
            add(forDate(v -> (Date) v), Date.class, java.sql.Date.class, Timestamp.class);
            add(forDate(v -> ((Calendar) v).getTime()), Calendar.class);
            add(forToString, BigDecimal.class, BigInteger.class, AtomicBoolean.class,
                    AtomicInteger.class, AtomicLong.class, Number.class, CharSequence.class);
            add(forEscapeToString, String.class, File.class, StringBuffer.class,
                    StringBuilder.class, MyStringBuilder.class, URI.class, URL.class, Pattern.class,
                    Charset.class);
            add(forAny, Object.class);
        }
    }

    public static class Deserializers {
        private static final Map<Type, Deserializer> deserializers = new HashMap<>();

        public static void add(Deserializer deserializer, Type... types) {
            for (Type type : types)
                deserializers.put(type, deserializer);
        }

        public static <T, R> Deserializer convert(Deserializer deserializer,
                Function<T, R> mapper) {
            return lex -> mapper.apply((T) deserializer.deserialize(lex));
        }

        public static final Deserializer forInteger = lex -> {
            switch (lex.getKey()) {
                case NUM:
                    return lex.getInt();
                case LITERAL:
                    return lex.getLiteralInt();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forLong = lex -> {
            switch (lex.getKey()) {
                case NUM:
                    return lex.getLong();
                case LITERAL:
                    return lex.getLiteralLong();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forCharacter = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return lex.getUnescapeChar();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forBoolean = lex -> {
            switch (lex.getKey()) {
                case TRUE:
                    return Boolean.TRUE;
                case FALSE:
                    return Boolean.FALSE;
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forFloat = lex -> {
            switch (lex.getKey()) {
                case NUM:
                    return Float.parseFloat(lex.getString());
                case LITERAL:
                    return Float.parseFloat(lex.getLiteralString());
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forDouble = lex -> {
            switch (lex.getKey()) {
                case NUM:
                    return Double.parseDouble(lex.getString());
                case LITERAL:
                    return Double.parseDouble(lex.getLiteralString());
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forString = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return lex.getUnescapeString();
                case TEXT:
                case NUM:
                    return lex.getString();
                case TRUE:
                    return "true";
                case FALSE:
                    return "false";
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forCharArray = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return lex.getUnescapeCharArray();
                case TEXT:
                case NUM:
                    return lex.getCharArray();
                case TRUE:
                    return Strings.TRUE_CHARS;
                case FALSE:
                    return Strings.FALSE_CHARS;
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forByterArray = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return lex.getBase64();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forDate = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return parseDate(lex.getUnescapeString());
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forDateMillis = lex -> {
            switch (lex.getKey()) {
                case LITERAL:
                    return parseDate(lex.getUnescapeString()).getTime();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forNumber = lex -> {
            switch (lex.getKey()) {
                case NUM:
                    return lex.getNumber();
                case LITERAL:
                    return lex.getLiteralNumber();
                default:
                    throw lex.newError();
            }
        };
        public static final Deserializer forDefault = Deserializers::readDefaultObject;

        private static Object readDefaultObject(JSONLex lex) {
            switch (lex.getKey()) {
                case NUM:
                    return lex.getNumber();
                case LITERAL:
                    return lex.getUnescapeString();
                case TEXT:
                    return lex.getString();
                case NULL:
                    return null;
                case TRUE:
                    return Boolean.TRUE;
                case FALSE:
                    return Boolean.FALSE;
                case LMB:
                    List<Object> list = new ArrayList<>();
                    readCollection(lex, list, forDefault);
                    return list;
                case LBB:
                    Map<Object, Object> map = new HashMap<>();
                    readMap(lex, map, forDefault, forDefault);
                    return map;
                default:
                    throw lex.newError();
            }
        }

        private static <T> void readCollection(JSONLex lex, Collection<T> col,
                Deserializer itemDeserializer) {
            if (lex.getKey() != Key.LMB)
                throw lex.newError();
            while (true) {
                lex.next();
                if (lex.getKey() == Key.RMB)
                    break;
                else if (lex.getKey() == Key.COMMA)
                    lex.next();
                T value = (T) itemDeserializer.deserialize(lex);
                col.add(value);
            }
        }

        public static Deserializer forCollection(Supplier<?> allocator,
                Deserializer itemDeserializer) {
            return lex -> {
                Collection<?> col = (Collection<?>) allocator.get();
                readCollection(lex, col, itemDeserializer);
                return col;
            };
        }

        public static final Deserializer forDefaultList = forCollection(ArrayList::new, forDefault);

        public static Deserializer forArray(Class<?> arrayClass, Deserializer itemDeserializer) {
            ArrayController<Object, Object> ctr = ArrayController.valueOf(arrayClass);
            return convert(forCollection(ArrayList::new, itemDeserializer), v -> {
                ArrayList<Object> list = (ArrayList<Object>) v;
                int len = list.size();
                Object arr = ctr.newInstance(len);
                for (int i = 0; i < len; i++) {
                    ctr.set(arr, i, list.get(i));
                }
                return arr;
            });
        }

        private static <K, V> void readMap(JSONLex lex, Map<K, V> map, Deserializer keyDeserializer,
                Deserializer valueDeserializer) {
            if (lex.getKey() != Key.LBB)
                throw lex.newError();
            while (true) {
                lex.next();
                if (lex.getKey() == Key.RBB)
                    break;
                if (lex.getKey() == Key.COMMA)
                    lex.next();
                K k = (K) keyDeserializer.deserialize(lex);
                lex.next();
                if (lex.getKey() != Key.COLON)
                    throw lex.newError();
                lex.next();
                V v = (V) keyDeserializer.deserialize(lex);
                map.put(k, v);
            }
        }

        public static Deserializer forMap(Supplier<?> allocator, Deserializer keyDeserializer,
                Deserializer valueDeserializer) {
            return lex -> {
                Map<?, ?> map = (Map<?, ?>) allocator.get();
                readMap(lex, map, keyDeserializer, valueDeserializer);
                return map;
            };
        }

        public static final Deserializer forDefaultMap =
                forMap(HashMap::new, forString, forDefault);

        public static Deserializer forBean(Supplier<?> allocator,
                Map<String, JSONSetterInfo> setters) {
            return lex -> {
                if (lex.getKey() != Key.LBB)
                    throw lex.newError();
                Object target = allocator.get();
                while (true) {
                    lex.next();
                    if (lex.getKey() == Key.RBB)
                        break;
                    if (lex.getKey() == Key.COMMA)
                        lex.next();
                    String name = (String) forString.deserialize(lex);
                    JSONSetterInfo setter = setters.get(name);
                    if (setter == null)
                        throw new JSONException("Setter [" + name + "] not found");
                    lex.next();
                    if (lex.getKey() != Key.COLON)
                        throw lex.newError();
                    lex.next();
                    Object value = setter.deserializer.deserialize(lex);
                    try {
                        setter.setter.set(target, value);
                    } catch (Exception e) {
                        throw new JSONException(e);
                    }
                }
                return target;
            };
        }

        public static Deserializer forBean(Type type) {
            Class<?> clazz = ReflectionSupport.getRawType(type);
            Map<String, Reflection.SetterInfo> setterMap = ReflectionSupport.getSettersMap(clazz);
            Map<String, JSONSetterInfo> setters = new HashMap<>();
            for (Map.Entry<String, Reflection.SetterInfo> e : setterMap.entrySet()) {
                Reflection.SetterInfo osi = e.getValue();
                JSONSetterInfo si = new JSONSetterInfo();
                si.setter = osi.setter;
                si.deserializer = forType(ReflectionSupport.calculateGenericType(type,
                        osi.member.getDeclaringClass(), osi.type));
                setters.put(e.getKey(), si);
            }
            return forBean(getAllocator(clazz), setters);
        }

        public static Deserializer forType(Type type) {
            Deserializer deserializer = deserializers.get(type);
            if (deserializer == null) {
                synchronized (Deserializers.class) {
                    if ((deserializer = deserializers.get(type)) == null)
                        deserializers.put(type, deserializer = forTypeWithoutCache(type));
                }
            }
            return deserializer;
        }

        private static Deserializer forTypeWithoutCache(Type type) {
            if (type instanceof Class) {
                Class<?> clazz = (Class<?>) type;
                if (clazz.isArray())
                    return forArray(clazz, forType(clazz.getComponentType()));
                return forGenericType(clazz, clazz);
            } else if (type instanceof GenericArrayType) {
                GenericArrayType gat = (GenericArrayType) type;
                return forArray(ReflectionSupport.getRawType(gat),
                        forType(gat.getGenericComponentType()));
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Class<?> clazz = (Class<?>) pt.getRawType();
                return forGenericType(clazz, type);
            } else if (type instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) type;
                return forType(tv.getBounds()[0]);
            } else if (type instanceof WildcardType) {
                WildcardType wt = (WildcardType) type;
                return forType(wt.getUpperBounds()[0]);
            } else
                throw new UnsupportedOperationException();
        }

        private static final Class<?>[] specialSuperClasses =
                new Class<?>[] {Set.class, Iterable.class, Map.class};

        private static Deserializer forGenericType(Class<?> rawType, Type type) {
            Type[] types = null;
            int index = -1;
            for (int i = 0; i < specialSuperClasses.length; i++) {
                Class<?> superClass = specialSuperClasses[i];
                if (superClass.isAssignableFrom(rawType)) {
                    types = ReflectionSupport.getGenericTypeMapping(type, superClass);
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                if (index == 0)
                    return forCollection(
                            rawType.isInterface() ? HashSet::new : getAllocator(rawType),
                            forType(types[0]));
                else if (index == 1)
                    return forCollection(
                            rawType.isInterface() ? ArrayList::new : getAllocator(rawType),
                            forType(types[0]));
                else
                    return forMap(rawType.isInterface() ? HashMap::new : getAllocator(rawType),
                            forType(types[0]), forType(types[1]));
            } else
                return forBean(type);
        }

        private static <T> Supplier<T> getAllocator(Class<T> clazz) {
            return () -> {
                try {
                    return clazz.newInstance();
                } catch (Exception e) {
                    throw new JSONException(e);
                }
            };
        }

        private static Date parseDate(String value) {
            SimpleDateFormat fmt = Configure.get().dateFormat;
            if (fmt == null)
                throw new JSONException("You need to set date format to JSON Configure");
            try {
                return fmt.parse(value);
            } catch (Exception e) {
                throw new JSONException(e);
            }
        }

        static {
            add(forInteger, int.class, Integer.class);
            add(convert(forInteger, v -> (short) (int) v), short.class, Short.class);
            add(convert(forInteger, v -> (byte) (int) v), byte.class, Byte.class);
            add(forLong, long.class, Long.class);
            add(forFloat, float.class, Float.class);
            add(forDouble, double.class, Double.class);
            add(forBoolean, boolean.class, Boolean.class);
            add(forCharacter, char.class, Character.class);
            add(convert(forString, ReflectionSupport::getClassByName), Class.class);
            add(forByterArray, byte[].class);
            add(forCharArray, char[].class);
            add(forDate, Date.class);
            add(convert(forDateMillis, v -> new java.sql.Date((long) v)), java.sql.Date.class);
            add(convert(forDateMillis, v -> new Timestamp((long) v)), Timestamp.class);
            add(convert(forDateMillis, v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) v);
                return cal;
            }), Calendar.class);
            add(convert(forString, v -> new BigDecimal((String) v)), BigDecimal.class);
            add(convert(forString, v -> new BigInteger((String) v)), BigInteger.class);
            add(convert(forBoolean, v -> new AtomicBoolean((boolean) v)), AtomicBoolean.class);
            add(convert(forInteger, v -> new AtomicInteger((int) v)), AtomicInteger.class);
            add(convert(forLong, v -> new AtomicLong((long) v)), AtomicLong.class);
            add(forNumber, Number.class);
            add(forString, String.class, CharSequence.class);
            add(convert(forString, v -> new File((String) v)), File.class);
            add(convert(forString, v -> new StringBuffer((String) v)), StringBuffer.class);
            add(convert(forString, v -> new StringBuilder((String) v)), StringBuilder.class);
            add(convert(forString, v -> new MyStringBuilder((String) v)), MyStringBuilder.class);
            add(convert(forString, v -> {
                try {
                    return new URI((String) v);
                } catch (Exception e) {
                    throw new JSONException(e);
                }
            }), URI.class);
            add(convert(forString, v -> {
                try {
                    return new URL((String) v);
                } catch (Exception e) {
                    throw new JSONException(e);
                }
            }), URL.class);
            add(convert(forString, v -> Pattern.compile((String) v)), Pattern.class);
            add(convert(forString, v -> Charset.forName((String) v)), Charset.class);
            add(forDefault, Object.class);
        }
    }

    public static class Configure implements Cloneable {
        private static final ThreadLocal<Configure> current = new ThreadLocal<>();
        public static final Configure DEFAULT = new Configure();

        static Configure get() {
            Configure conf = current.get();
            if (conf == null)
                conf = DEFAULT;
            return conf;
        }

        public static Configure current() {
            Configure conf = current.get();
            if (conf == null)
                current.set(conf = DEFAULT.clone());
            return conf;
        }

        public static void reset() {
            current.remove();
        }

        public SimpleDateFormat dateFormat;

        @Override
        public Configure clone() {
            try {
                return (Configure) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }
    }

    static class ReflectionSupport {
        private static final Reflection r = Reflection.INSTANCE;

        public static Map<String, Reflection.GetterInfo> getGettersMap(Class<?> clazz) {
            return r.getGettersMap(clazz);
        }

        public static Map<String, Reflection.SetterInfo> getSettersMap(Class<?> clazz) {
            return r.getSettersMap(clazz);
        }

        public static Class<?> getClassByName(String name) {
            return r.getClassByName(name);
        }

        public static Class<?> getRawType(Type type) {
            return r.getRawType(type);
        }

        public static Type[] getGenericTypeMapping(Type type, Class<?> superClass) {
            return r.getGenericTypeMapping(type, superClass);
        }

        public static Type calculateGenericType(Type type, Class<?> superClass,
                Type calculateType) {
            return r.calculateGenericType(type, superClass, calculateType);
        }
    }

    public interface JSONLex {
        public Key getKey();

        public Key next();

        public RuntimeException newError();

        public String getString();

        public String getLiteralString();

        public String getUnescapeString();

        public char[] getCharArray();

        public char[] getUnescapeCharArray();

        public byte[] getBase64();

        public char getUnescapeChar();

        public int getInt();

        public int getLiteralInt();

        public long getLong();

        public long getLiteralLong();

        public Number getNumber();

        public Number getLiteralNumber();
    }

    static class StringJSONLex extends Lex.StringLex<Key> implements JSONLex {
        private static final DFAState START = new StringJSONLexBuilder().build();

        public StringJSONLex() {
            super(START);
        }

        static class StringJSONLexBuilder extends Lex.Builder<StringJSONLex> {
            public DFAState build() {
                initNFA();

                defineActionTemplate("finish", (lex, a) -> lex.finish((Key) a[0]));
                defineActionTemplate("rollback", (lex, a) -> lex.rollback());

                Key[] keys = Key.values();
                for (Key key : keys)
                    defineVariable(key.name(), key);

                definePattern("ESCAPE", "\\\\(u[0-9a-fA-F]{4})|([^u])");
                definePattern("LITERAL_1", "\'(${ESCAPE}|[^(\\\\\\\')])*\'");
                definePattern("LITERAL_2", "\"(${ESCAPE}|[^(\\\\\\\")])*\"");
                definePattern("NOT_NAMED_CHAR", "[^(a-zA-Z0-9_)]");

                addPattern("START", "[\r\n\t\b\f ]+#{finish(null)}");
                addPattern("START", "[$]#{finish(EOF)}");
                addPattern("START", "[a-zA-Z_][a-zA-Z0-9_]*#{finish(TEXT)}");
                addPattern("START", "-?[0-9]+(\\.[0-9]+)?#{finish(NUM)}");
                addPattern("START", "${LITERAL_1}|${LITERAL_2}#{finish(LITERAL)}");
                for (Key key : keys) {
                    if (key.ch != 0)
                        addPattern("START", "\\" + key.ch + "#{finish(" + key.name() + ")}");
                    else if (key.str != null)
                        addPattern("START", key.str + "${NOT_NAMED_CHAR}#{finish(" + key.name()
                                + ")}#{rollback()}");
                }
                return buildDFA("START");
            }
        }


        @Override
        public RuntimeException newError(String message) {
            return new JSONException(message);
        }

        public Key getKey() {
            return key;
        }

        private MyStringBuilder buffer = new MyStringBuilder();

        public String getString() {
            return str.substring(begin, index);
        }

        public String getLiteralString() {
            return str.substring(begin + 1, index - 1);
        }

        public String getUnescapeString() {
            return buffer.clear().appendUnEscape(str, begin + 1, index - 1).toString();
        }

        public char[] getCharArray() {
            char[] chars = new char[index - begin];
            str.getChars(begin, index, chars, 0);
            return chars;
        }

        public char[] getUnescapeCharArray() {
            return buffer.clear().appendUnEscape(str, begin + 1, index - 1).toCharArray();
        }

        public byte[] getBase64() {
            return Strings.decodeBase64(str, begin + 1, index - 1);
        }

        public char getUnescapeChar() {
            return buffer.clear().appendUnEscape(str, begin + 1, index - 1).getLocalChars()[0];
        }

        public int getInt() {
            return Numbers.parseIntWithRadix(str, begin, index);
        }

        public int getLiteralInt() {
            return Numbers.parseIntWithRadix(str, begin + 1, index - 1);
        }

        public long getLong() {
            return Numbers.parseLongWithRadix(str, begin, index);
        }

        public long getLiteralLong() {
            return Numbers.parseLongWithRadix(str, begin + 1, index - 1);
        }

        public Number getNumber() {
            return Numbers.parseDeclareNumber(str, begin, index);
        }

        public Number getLiteralNumber() {
            return Numbers.parseDeclareNumber(str, begin + 1, index - 1);
        }
    }
}
