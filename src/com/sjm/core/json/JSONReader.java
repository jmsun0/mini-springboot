package com.sjm.core.json;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.sjm.core.util.Analyzer;
import com.sjm.core.util.ArrayController;
import com.sjm.core.util.Converter;
import com.sjm.core.util.Converters;
import com.sjm.core.util.Filter;
import com.sjm.core.util.Filters;
import com.sjm.core.util.Maps;
import com.sjm.core.util.Misc;
import com.sjm.core.util.MyStringBuilder;
import com.sjm.core.util.Numbers;
import com.sjm.core.util.Reflection;
import com.sjm.core.util.Reflection.IType;
import com.sjm.core.util.Source;
import com.sjm.core.util.Strings;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JSONReader<T> {
    public abstract T read(JSONType word, Source<JSONType> src);

    public static enum JSONType {
        EOF, BLANK, TEXT, NUMBER, QUOTATION, TRUE, FALSE, NULL, LEFT_BIG_BRACKETS, RIGHT_BIG_BRACKETS, LEFT_MEDIUM_BRACKETS, RIGHT_MEDIUM_BRACKETS, COMMA, COLON
    }

    public static final Analyzer<JSONType> JSONAnalyzer;
    static {
        Analyzer<JSONType> alz = JSONAnalyzer = new Analyzer<JSONType>();
        alz.setText(JSONType.TEXT);
        alz.setEOF(JSONType.EOF);
        alz.setBlank(Strings.BlankChars, JSONType.BLANK);
        alz.setEscape('\'', JSONType.QUOTATION);
        alz.setEscape('\"', JSONType.QUOTATION);
        alz.setNumber(JSONType.NUMBER);
        alz.setString("true", JSONType.TRUE);
        alz.setString("false", JSONType.FALSE);
        alz.setString("null", JSONType.NULL);
        alz.setSymbol("{", JSONType.LEFT_BIG_BRACKETS);
        alz.setSymbol("}", JSONType.RIGHT_BIG_BRACKETS);
        alz.setSymbol("[", JSONType.LEFT_MEDIUM_BRACKETS);
        alz.setSymbol("]", JSONType.RIGHT_MEDIUM_BRACKETS);
        alz.setSymbol(",", JSONType.COMMA);
        alz.setSymbol(":", JSONType.COLON);
    }

    static final Filter<JSONType> NotBlank = Filters.not(Filters.identity(JSONType.BLANK));

    public static <T> T read(CharSequence str, JSONReader<T> jr) {
        Source<JSONType> src = Source.filter(JSONAnalyzer.analyze(str), NotBlank);
        return jr.read(src.next(), src);
    }

    // 复杂类型
    public static final JSONReader<Object> ForObject = new JSONReader<Object>() {
        @Override
        public Object read(JSONType word, Source<JSONType> src) {
            switch (word) {
                case QUOTATION:
                    return ForString.read(word, src);
                case NUMBER:
                    return ForNumber.read(word, src);
                case LEFT_BIG_BRACKETS:
                    return ForJSONObject.read(word, src);
                case LEFT_MEDIUM_BRACKETS:
                    return ForJSONArray.read(word, src);
                case TRUE:
                    return Boolean.TRUE;
                case FALSE:
                    return Boolean.FALSE;
                case NULL:
                    return null;
                default:
                    throw new RuntimeException();
            }
        }
    };
    public static final JSONReader<JSONObject> ForJSONObject = new JSONReader<JSONObject>() {
        @Override
        public JSONObject read(JSONType word, Source<JSONType> src) {
            JSONObject json = new JSONObject();
            readMap(word, src, json, ForString, ForObject);
            return json;
        }
    };
    public static final JSONReader<JSONArray> ForJSONArray = new JSONReader<JSONArray>() {
        @Override
        public JSONArray read(JSONType word, Source<JSONType> src) {
            JSONArray array = new JSONArray();
            readCollection(word, src, array, ForObject);
            return array;
        }
    };

    public static final JSONReader<Object> ForObject(final Reflection.IType type) {
        // System.out.println(type);
        Reflection.IClass clazz = type.getIClass();
        return new JSONReader<Object>() {
            @Override
            public Object read(JSONType word, Source<JSONType> src) {
                if (word != JSONType.LEFT_BIG_BRACKETS)
                    throw new RuntimeException();
                Object obj = clazz.getCreator().newInstance();
                while (true) {
                    word = src.next();
                    if (word == JSONType.RIGHT_BIG_BRACKETS)
                        break;
                    else if (word == JSONType.COMMA)
                        word = src.next();
                    CharSequence value = src.getValue();
                    Reflection.Setter st =
                            clazz.getSetter(value.subSequence(1, value.length() - 1));
                    word = src.next();
                    if (word != JSONType.COLON)
                        throw new RuntimeException();
                    word = src.next();
                    if (st != null) {
                        IType fieldType = st.getIType();
                        switch (fieldType.getFlag()) {
                            case Param:
                            case Variable:
                                fieldType = Reflection.Util.replaceVariable(fieldType,
                                        clazz.getTypeParameters(), type.getParams());
                                break;
                            default:
                                break;
                        }
                        st.set(obj, ForAny(fieldType).read(word, src));// TODO
                    } else
                        ForObject.read(word, src);// TODO
                }
                return obj;
            }
        };
    }

    public static final <T> JSONReader<Collection<T>> ForCollection(final Reflection.Creator ct,
            final JSONReader<T> reader) {
        return new JSONReader<Collection<T>>() {
            @Override
            public Collection<T> read(JSONType word, Source<JSONType> src) {
                Collection<T> col = (Collection<T>) ct.newInstance();;
                readCollection(word, src, col, reader);
                return col;
            }
        };
    }

    public static final <T> JSONReader<Object> ForArray(final ArrayController<T, Object> ctr,
            final JSONReader<T> reader) {
        return new JSONReader<Object>() {
            @Override
            public Object read(JSONType word, Source<JSONType> src) {
                List<T> list = new ArrayList<>();
                readCollection(word, src, list, reader);
                int len = list.size();
                Object array = ctr.newInstance(len);
                for (int i = 0; i < len; i++)
                    ctr.set(array, i, (T) list.get(i));
                return array;
            }
        };
    }

    public static final <K, V> JSONReader<Map<K, V>> ForMap(final Reflection.Creator ct,
            final JSONReader<K> kr, final JSONReader<V> vr) {
        return new JSONReader<Map<K, V>>() {
            @Override
            public Map<K, V> read(JSONType word, Source<JSONType> src) {
                Map<K, V> map = (Map<K, V>) ct.newInstance();
                readMap(word, src, map, kr, vr);
                return map;
            }
        };
    }

    public static final <T> void readCollection(JSONType word, Source<JSONType> src,
            Collection<T> col, JSONReader<T> reader) {
        if (word != JSONType.LEFT_MEDIUM_BRACKETS)
            throw new RuntimeException();
        while (true) {
            word = src.next();
            if (word == JSONType.RIGHT_MEDIUM_BRACKETS)
                break;
            else if (word == JSONType.COMMA)
                word = src.next();
            col.add(reader.read(word, src));
        }
    }

    public static final <K, V> void readMap(JSONType word, Source<JSONType> src, Map<K, V> map,
            JSONReader<K> kr, JSONReader<V> vr) {
        if (word != JSONType.LEFT_BIG_BRACKETS)
            throw new RuntimeException();
        while (true) {
            word = src.next();
            if (word == JSONType.RIGHT_BIG_BRACKETS)
                break;
            else if (word == JSONType.COMMA)
                word = src.next();
            K key = kr.read(word, src);
            word = src.next();
            if (word != JSONType.COLON)
                throw new RuntimeException();
            word = src.next();
            map.put(key, vr.read(word, src));
        }
    }

    public static final JSONReader<?> ForAny(Reflection.IType type) {
        JSONReader<?> reader = readerMap.get(type);
        Reflection.IClass clazz = type.getIClass();
        Class<?> clz = type.getClazz();
        if (reader == null) {
            if (type.getFlag() == Reflection.IType.TypeFlag.Array) {
                Reflection.IType ct = type.getComponent();
                reader = ForArray(ArrayController.valueOf(ct.getClazz()),
                        (JSONReader<Object>) ForAny(ct));
            } else if (clazz.isExtendsOf(Collection.class)) {
                reader = ForCollection(clazz.getCreator(), ForAny(getParamIType(type, 0)));
            } else if (clazz.isExtendsOf(Map.class)) {
                reader = ForMap(clazz.getCreator(), ForAny(getParamIType(type, 0)),
                        ForAny(getParamIType(type, 1)));
            } else if (isReference(clz)) {
                reader = creator(ForAny(getParamIType(type, 0)), clazz.getCreator());
            } else
                reader = ForObject(type);
            readerMap.put(type, reader);
        }
        return reader;
    }

    public static final <T> JSONReader<T> ForJavaObject(Class<T> clazz) {
        return (JSONReader<T>) ForAny(Reflection.forClass(clazz).getIType());
    }

    static final Reflection.Creator ArrayListCreator =
            Reflection.forClass(ArrayList.class).getCreator();

    public static final <T> JSONReader<List<T>> ForJavaList(Class<T> clazz) {
        return (JSONReader) ForCollection(ArrayListCreator, ForJavaObject(clazz));
    }

    static <D, S> JSONReader<D> convert(final JSONReader<S> jc,
            final Converter<? extends D, ? super S> cv) {
        return new JSONReader<D>() {
            @Override
            public D read(JSONType word, Source<JSONType> src) {
                return cv.convert(jc.read(word, src));
            }
        };
    }

    static <T> JSONReader<T> creator(final JSONReader<?> jc, final Reflection.Creator ct) {
        return new JSONReader<T>() {
            @Override
            public T read(JSONType word, Source<JSONType> src) {
                return (T) ct.newInstance(jc.read(word, src));
            }
        };
    }

    static <T> JSONReader<T> creator(JSONReader<?> jc, Class<T> clazz) {
        return creator(jc, Reflection.forClass(clazz).getCreator());
    }

    static boolean isReference(Class<?> clz) {
        return clz == WeakReference.class || clz == SoftReference.class
                || clz == AtomicReference.class;
    }

    static final Reflection.IType ObjectIType = Reflection.forClass(Object.class).getIType();

    static Reflection.IType getParamIType(Reflection.IType type, int index) {
        return type.getFlag() == Reflection.IType.TypeFlag.Param ? type.getParams().get(index)
                : ObjectIType;
    }

    // 基本类型
    public static final JSONReader<Integer> ForInteger = new JSONReader<Integer>() {
        @Override
        public Integer read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            switch (type) {
                case NUMBER:
                    return Numbers.parseIntWithRadix(value, null, -1, -1);
                case QUOTATION:
                    return Numbers.parseIntWithRadix(value, null, 1, value.length() - 1);
                default:
                    throw new RuntimeException();
            }
        }
    };
    public static final JSONReader<Long> ForLong = new JSONReader<Long>() {
        @Override
        public Long read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            switch (type) {
                case NUMBER:
                    return Numbers.parseLongWithRadix(value, null, -1, -1);
                case QUOTATION:
                    return Numbers.parseLongWithRadix(value, null, 1, value.length() - 1);
                default:
                    throw new RuntimeException();
            }
        }
    };
    public static final JSONReader<Character> ForCharacter = new JSONReader<Character>() {
        @Override
        public Character read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            if (type == JSONType.QUOTATION) {
                Misc.IntBox box = new Misc.IntBox(1);
                int ch = Strings.nextUnEscapeChar(value, box);
                if (ch != -1 && Strings.nextUnEscapeChar(value, box) == -1)
                    return (char) ch;
            }
            throw new RuntimeException();
        }
    };
    public static final JSONReader<Boolean> ForBoolean = new JSONReader<Boolean>() {
        @Override
        public Boolean read(JSONType type, Source<JSONType> src) {
            if (type == JSONType.TRUE)
                return Boolean.TRUE;
            else if (type == JSONType.FALSE)
                return Boolean.FALSE;
            throw new RuntimeException();
        }
    };
    public static final JSONReader<Float> ForFloat = new JSONReader<Float>() {
        @Override
        public Float read(JSONType type, Source<JSONType> src) {// TODO
            CharSequence value = src.getValue();
            if (type == JSONType.NUMBER)
                return Float.parseFloat(value.toString());
            if (type == JSONType.QUOTATION)
                return Float.parseFloat(value.subSequence(1, value.length() - 1).toString());
            throw new RuntimeException();
        }
    };
    public static final JSONReader<Double> ForDouble = new JSONReader<Double>() {
        @Override
        public Double read(JSONType type, Source<JSONType> src) {// TODO
            CharSequence value = src.getValue();
            if (type == JSONType.NUMBER)
                return Double.parseDouble(value.toString());
            if (type == JSONType.QUOTATION)
                return Double.parseDouble(value.subSequence(1, value.length() - 1).toString());
            throw new RuntimeException();
        }
    };
    public static final JSONReader<String> ForString = new JSONReader<String>() {
        @Override
        public String read(JSONType type, Source<JSONType> src) {
            if (type == JSONType.QUOTATION)
                return Strings.unEscape(src.getValue()).toString();
            else if (type == JSONType.TEXT || type == JSONType.NUMBER)
                return src.getValue().toString();
            else if (type == JSONType.NULL)
                return null;
            else if (type == JSONType.TRUE)
                return "true";
            else if (type == JSONType.FALSE)
                return "false";
            throw new RuntimeException();
        }
    };
    public static final JSONReader<char[]> ForCharArray = new JSONReader<char[]>() {
        @Override
        public char[] read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            if (type == JSONType.QUOTATION) {
                new MyStringBuilder().appendUnEscape(value, null, 1, value.length() - 1)
                        .toCharArray();
            } else if (type == JSONType.TEXT || type == JSONType.NUMBER) {
                char[] cs = new char[value.length()];
                for (int i = 0, len = value.length(); i < len; i++)
                    cs[i] = value.charAt(i);
                return cs;
            } else if (type == JSONType.NULL)
                return new char[] {'n', 'u', 'l', 'l'};
            else if (type == JSONType.TRUE)
                return new char[] {'t', 'r', 'u', 'e'};
            else if (type == JSONType.FALSE)
                return new char[] {'f', 'a', 'l', 's', 'e'};
            throw new RuntimeException();
        }
    };
    public static final JSONReader<byte[]> ForByterArray = new JSONReader<byte[]>() {
        @Override
        public byte[] read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            if (type == JSONType.QUOTATION)
                return Strings.decodeBase64(value, 1, value.length() - 1);
            throw new RuntimeException();
        }
    };
    public static final JSONReader<Long> ForTime = new JSONReader<Long>() {
        @Override
        public Long read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            if (type == JSONType.QUOTATION) {
                // TODO
            }
            return Numbers.parseLongWithRadix(value, null, -1, -1);
        }
    };
    public static final JSONReader<Number> ForNumber = new JSONReader<Number>() {
        @Override
        public Number read(JSONType type, Source<JSONType> src) {
            CharSequence value = src.getValue();
            if (type == JSONType.NUMBER)
                return Numbers.parseDeclareNumber(value, null, -1, -1);
            if (type == JSONType.QUOTATION)
                return Numbers.parseDeclareNumber(value, null, 1, value.length() - 1);
            throw new RuntimeException();
        }
    };
    public static final JSONReader<Byte> ForByte = convert(ForInteger, Converters.ToByte);
    public static final JSONReader<Short> ForShort = convert(ForInteger, Converters.ToShort);
    public static final JSONReader<BigDecimal> ForBigDecimal = creator(ForString, BigDecimal.class);// TODO
    public static final JSONReader<BigInteger> ForBigInteger = creator(ForString, BigInteger.class);// TODO
    public static final JSONReader<Date> ForDate = creator(ForTime, Date.class);
    public static final JSONReader<java.sql.Date> ForSQLDate =
            creator(ForTime, java.sql.Date.class);
    public static final JSONReader<Timestamp> ForTimestamp = creator(ForTime, Timestamp.class);
    public static final JSONReader<Calendar> ForCalendar = creator(ForTime, Calendar.class);
    public static final JSONReader<File> ForFile = creator(ForString, File.class);
    public static final JSONReader<Class> ForClass = creator(ForString, Class.class);
    public static final JSONReader<StringBuffer> ForStringBuffer =
            creator(ForString, StringBuffer.class);
    public static final JSONReader<StringBuilder> ForStringBuilder =
            creator(ForString, StringBuilder.class);
    public static final JSONReader<MyStringBuilder> ForMyStringBuilder =
            creator(ForString, MyStringBuilder.class);
    public static final JSONReader<AtomicBoolean> ForAtomicBoolean =
            creator(ForBoolean, AtomicBoolean.class);
    public static final JSONReader<AtomicInteger> ForAtomicInteger =
            creator(ForInteger, AtomicInteger.class);
    public static final JSONReader<AtomicLong> ForAtomicLong = creator(ForLong, AtomicLong.class);
    public static final JSONReader<URI> ForURI = creator(ForString, URI.class);
    public static final JSONReader<URL> ForURL = creator(ForString, URL.class);
    public static final JSONReader<Pattern> ForPattern = creator(ForString, Pattern.class);
    public static final JSONReader<Charset> ForCharset = creator(ForString, Charset.class);

    public static final JSONReader<AtomicReference> ForAtomicReference =
            creator(ForString, AtomicReference.class);

    public static final void setReader(Reflection.IType type, JSONReader<?> reader) {
        readerMap.put(type, reader);
    }

    public static final void setReader(Class<?> clazz, JSONReader<?> reader) {
        setReader(Reflection.forClass(clazz).getIType(), reader);
    }

    public static final Maps.MyHashMap<Reflection.IType, JSONReader<?>> readerMap;
    static {
        readerMap = new Maps.MyHashMap<>();
        readerMap.setHashFunction(new Maps.HashFunction<Reflection.IType>() {
            @Override
            public int hashCode(Reflection.IType value) {
                switch (value.getFlag()) {
                    case Class:
                    case Question:
                    case Variable:
                        return value.getClazz().hashCode();
                    case Array:
                        return 1237 ^ hashCode(value.getComponent());
                    case Param:
                        List<Reflection.IType> params = value.getParams();
                        int h = value.getClazz().hashCode();
                        for (int i = 0, len = params.size(); i < len; i++)
                            h ^= hashCode(params.get(0));
                        return h;
                    default:
                        throw new Error();
                }
            }
        });
        readerMap.setEqualsFunction(new Maps.EqualsFunction<Reflection.IType>() {
            @Override
            public boolean equals(Reflection.IType o1, Reflection.IType o2) {
                Reflection.IType.TypeFlag flag = o1.getFlag();
                if (flag != o2.getFlag())
                    return false;
                switch (flag) {
                    case Class:
                    case Question:
                    case Variable:
                        return o1.getClazz() == o2.getClazz();
                    case Array:
                        return equals(o1.getComponent(), o2.getComponent());
                    case Param:
                        List<Reflection.IType> p1 = o1.getParams();
                        List<Reflection.IType> p2 = o2.getParams();
                        int len = p1.size();
                        if (len != p2.size())
                            return false;
                        for (int i = 0; i < len; i++)
                            if (!equals(p1.get(i), p2.get(i)))
                                return false;
                        return true;
                    default:
                        throw new Error();
                }
            }
        });
    }

    static {
        setReader(int.class, ForInteger);
        setReader(Integer.class, ForInteger);
        setReader(long.class, ForLong);
        setReader(Long.class, ForLong);
        setReader(char.class, ForCharacter);
        setReader(Character.class, ForCharacter);
        setReader(boolean.class, ForBoolean);
        setReader(Boolean.class, ForBoolean);
        setReader(byte.class, ForByte);
        setReader(Byte.class, ForByte);
        setReader(short.class, ForShort);
        setReader(Short.class, ForShort);
        setReader(float.class, ForFloat);
        setReader(Float.class, ForFloat);
        setReader(double.class, ForDouble);
        setReader(Double.class, ForDouble);
        setReader(BigDecimal.class, ForBigDecimal);
        setReader(BigInteger.class, ForBigInteger);
        setReader(Number.class, ForNumber);
        setReader(String.class, ForString);
        setReader(CharSequence.class, ForString);
        setReader(char[].class, ForCharArray);
        setReader(byte[].class, ForByterArray);
        setReader(Date.class, ForDate);
        setReader(java.sql.Date.class, ForSQLDate);
        setReader(Timestamp.class, ForTimestamp);
        setReader(Calendar.class, ForCalendar);
        setReader(File.class, ForFile);
        setReader(Class.class, ForClass);
        setReader(MyStringBuilder.class, ForMyStringBuilder);
        setReader(StringBuffer.class, ForStringBuffer);
        setReader(StringBuilder.class, ForStringBuilder);
        setReader(AtomicBoolean.class, ForAtomicBoolean);
        setReader(AtomicInteger.class, ForAtomicInteger);
        setReader(AtomicLong.class, ForAtomicLong);
        setReader(URI.class, ForURI);
        setReader(URL.class, ForURL);
        setReader(Pattern.class, ForPattern);
        setReader(Charset.class, ForCharset);

        setReader(Object.class, ForObject);
        setReader(JSONObject.class, ForJSONObject);
        setReader(JSONArray.class, ForJSONArray);
    }
}
