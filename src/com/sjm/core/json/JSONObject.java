package com.sjm.core.json;

import java.util.List;
import java.util.Map;

import com.sjm.core.util.ArrayController;
import com.sjm.core.util.Converters;
import com.sjm.core.util.Maps;
import com.sjm.core.util.MyStringBuilder;
import com.sjm.core.util.Reflection;

public class JSONObject extends Maps.MyHashMap<String, Object> {
    public static String toJSONString(Object obj) {
        MyStringBuilder sb = new MyStringBuilder();
        JSONWriter.writeObject(obj, sb);
        return sb.toString();
    }

    public static Object parse(CharSequence str) {
        return JSONReader.read(str, JSONReader.ForObject);
    }

    public static JSONObject parseObject(CharSequence str) {
        return JSONReader.read(str, JSONReader.ForJSONObject);
    }

    public static <T> T parseObject(CharSequence str, Class<T> clazz) {
        return JSONReader.read(str, JSONReader.ForJavaObject(clazz));
    }

    public static Object parseObject(CharSequence str, Reflection.IType type) {
        return JSONReader.read(str, JSONReader.ForAny(type));
    }

    public static JSONArray parseArray(CharSequence str) {
        return JSONReader.read(str, JSONReader.ForJSONArray);
    }

    public static <T> List<T> parseArray(CharSequence str, Class<T> clazz) {
        return JSONReader.read(str, JSONReader.ForJavaList(clazz));
    }

    public static JSONObject Empty = new JSONObject();

    @SuppressWarnings("rawtypes")
    public static Object getForPath(Object obj, Object... path) {
        try {
            for (Object key : path) {
                if (obj instanceof Map)
                    obj = ((Map) obj).get(key);
                else if (ArrayController.isArray(obj))
                    obj = ArrayController.valueOf(obj).get(obj, (int) key);
                else
                    obj = Reflection.forClass(obj.getClass()).getGetter((CharSequence) key)
                            .get(obj);
            }
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    public static Maps.KV<Object[]> opsForPath(final Object obj) {
        return new Maps.KV<Object[]>() {
            @Override
            public Object get(Object[] key) {
                return getForPath(obj, key);
            }

            @Override
            public Iterable<Object[]> keySet() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public JSONObject() {}

    public JSONObject(Map<String, Object> map) {
        putAll(map);
    }

    public JSONObject(Object obj) {
        for (Reflection.Getter f : Reflection.forClass(obj.getClass()).getGetterMap().values())
            put(f.getName(), f.get(obj));
    }

    public void accumulate(String key, Object value) {
        Object v = get(key);
        if (v == null) {
            put(key, value);
        } else if (v instanceof JSONArray) {
            ((JSONArray) v).add(value);
        } else {
            JSONArray arr = new JSONArray();
            arr.add(v);
            arr.add(value);
            put(key, arr);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T toJavaObject(Class<T> clazz) {
        Reflection.IClass cls = Reflection.forClass(clazz);
        Object obj = cls.getCreator().newInstance();
        for (Reflection.Setter st : cls.getSetterMap().values()) {
            Object value = get(st.getName());
            if (value != null)
                st.set(obj, Converters.convert(value, st.getIType().getClazz()));
        }
        return (T) obj;
    }

    @Override
    public String toString() {
        MyStringBuilder sb = new MyStringBuilder();
        JSONWriter.writeMap(this, sb);
        return sb.toString();
    }
}
