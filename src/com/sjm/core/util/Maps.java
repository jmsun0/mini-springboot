package com.sjm.core.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sjm.core.json.JSONArray;
import com.sjm.core.json.JSONObject;

import java.util.Set;
import java.util.Stack;



@SuppressWarnings({"unchecked", "rawtypes"})
public class Maps {

    public static Map<String, Object> newMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        if (value != null)
            map.put(key, value);
        return map;
    }

    public static Map<String, Object> newMap(Object... kvs) {
        return newObjectMap(kvs);
    }

    public static <K, V> Map<K, V> newObjectMap(Object... kvs) {
        if (kvs.length % 2 != 0)
            throw new IllegalArgumentException();
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            Object value = kvs[i + 1];
            if (value != null)
                map.put((K) kvs[i], (V) value);
        }
        return map;
    }

    public static Map<String, String> newStringMap(Object... kvs) {
        if (kvs.length % 2 != 0)
            throw new IllegalArgumentException();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            Object value = kvs[i + 1];
            if (value != null)
                map.put(String.valueOf(kvs[i]), String.valueOf(value));
        }
        return map;
    }

    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        V value = map.get(key);
        return value == null ? defaultValue : value;
    }

    public static <K, V> V getOrDefault(Map map, K[] keys, V defaultValue) {
        Object value = map;
        for (K key : keys) {
            value = ((Map) value).get(key);
            if (value == null)
                return defaultValue;
        }
        return (V) value;
    }

    public static <K, V> Map<K, V> asMap(List<K> keys, List<V> values) {
        int len = keys.size();
        if (len != values.size())
            throw new IllegalArgumentException();
        HashMap<K, V> map = new HashMap<K, V>(len);
        for (int i = 0; i < len; i++)
            map.put(keys.get(i), values.get(i));
        return map;
    }

    public static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map)
            return (Map<String, Object>) obj;
        return new ObjectMap(obj, Collections.<String>emptySet());
    }

    public static Map<String, Object> asMap(Object obj, Set<String> keyset) {
        return new ObjectMap(obj, keyset);
    }

    public static <K, V> Map<K, V> putAll(Map<K, V> map, Iterable<V> values,
            Converter<? extends K, ? super V> toKey) {
        for (V value : values)
            map.put(toKey.convert(value), value);
        return map;
    }

    public static <K, V, E> Map<K, V> putAllConvert(Map<K, V> map, Iterable<E> values,
            Converter<? extends K, ? super E> toKey, Converter<? extends V, ? super E> toValue) {
        for (E e : values)
            map.put(toKey.convert(e), toValue.convert(e));
        return map;
    }

    public static <K, E> Map<K, List<E>> accumulate(Map<K, List<E>> map, Iterable<E> values,
            Converter<? extends K, ? super E> toKey) {
        for (E e : values) {
            K key = toKey.convert(e);
            List<E> ls = map.get(key);
            if (ls == null)
                map.put(key, ls = new ArrayList<>());
            ls.add(e);
        }
        return map;
    }

    public static <K, V, E> Map<K, List<V>> accumulateConvert(Map<K, List<V>> map,
            Iterable<E> values, Converter<? extends K, ? super E> toKey,
            Converter<? extends V, ? super E> toValue) {
        for (E e : values) {
            K key = toKey.convert(e);
            List<V> ls = map.get(key);
            if (ls == null)
                map.put(key, ls = new ArrayList<>());
            ls.add(toValue.convert(e));
        }
        return map;
    }

    public static <K, E> Map<K, List<E>> groupBy(Iterable<E> itr,
            Converter<? extends K, ? super E> c) {
        return accumulate(new HashMap<K, List<E>>(), itr, c);
    }

    public static <K, V, E> Map<K, List<V>> groupByAndConvert(Iterable<E> values,
            Converter<? extends K, ? super E> toKey, Converter<? extends V, ? super E> toValue) {
        return accumulateConvert(new HashMap<K, List<V>>(), values, toKey, toValue);
    }

    public static <K, E> Map<K, E> groupByOne(Iterable<E> values,
            Converter<? extends K, ? super E> toKey) {
        return putAll(new HashMap<K, E>(), values, toKey);
    }

    public static <K, V, E> Map<K, V> groupByOneAndConvert(Iterable<E> values,
            Converter<? extends K, ? super E> toKey, Converter<? extends V, ? super E> toValue) {
        return putAllConvert(new HashMap<K, V>(), values, toKey, toValue);
    }

    // key对应的索引
    public static <K, E> Map<K, Integer> groupByIndex(List<E> list,
            Converter<? extends K, ? super E> c) {
        Map<K, Integer> map = new HashMap<>();
        for (int i = 0, len = list.size(); i < len; i++)
            map.put(c.convert(list.get(i)), i);
        return map;
    }

    // Key对应的数量
    public static <K, E> Map<K, Integer> groupByCount(Iterable<E> itr,
            Converter<? extends K, ? super E> c) {
        Map<K, Integer> map = new HashMap<>();
        for (E e : itr) {
            K key = c.convert(e);
            Integer n = map.get(key);
            if (n == null)
                map.put(key, 1);
            else
                map.put(key, n + 1);
        }
        return map;
    }

    // 支持两个Key
    public static <K1, K2, E> Map<K1, Map<K2, List<E>>> groupBy(Iterable<E> values,
            Converter<? extends K1, ? super E> toKey1, Converter<? extends K2, ? super E> toKey2) {
        Map<K1, Map<K2, List<E>>> map1 = new HashMap<>();
        for (E e : values) {
            K1 key1 = toKey1.convert(e);
            K2 key2 = toKey2.convert(e);
            Map<K2, List<E>> map2 = map1.get(key1);
            if (map2 == null)
                map1.put(key1, map2 = new HashMap<>());
            List<E> ls = map2.get(key2);
            if (ls == null)
                map2.put(key2, ls = new ArrayList<>());
            ls.add(e);
        }
        return map1;
    }

    public static <K1, K2, E> Map<K1, Map<K2, E>> groupByOne(Iterable<E> values,
            Converter<? extends K1, ? super E> toKey1, Converter<? extends K2, ? super E> toKey2) {
        Map<K1, Map<K2, E>> map1 = new HashMap<>();
        for (E e : values) {
            K1 key1 = toKey1.convert(e);
            K2 key2 = toKey2.convert(e);
            Map<K2, E> map2 = map1.get(key1);
            if (map2 == null)
                map1.put(key1, map2 = new HashMap<>());
            map2.put(key2, e);
        }
        return map1;
    }

    // 支持任意个Key
    public static Map groupBy(Iterable itr, Converter... toKeys) {
        Map map = new HashMap<>();
        for (Object e : itr) {
            int len = toKeys.length - 1;
            Map map1 = map;
            for (int i = 0; i < len; i++) {
                Object key = toKeys[i].convert(e);
                Map map2 = (Map) map1.get(key);
                if (map2 == null)
                    map1.put(key, map2 = new HashMap<>());
                map1 = map2;
            }
            Object key = toKeys[len].convert(e);
            List ls = (List) map1.get(key);
            if (ls == null)
                map1.put(key, ls = new ArrayList<>());
            ls.add(e);
        }
        return map;
    }

    public static Map groupByOne(Iterable itr, Converter... toKeys) {
        Map map = new HashMap<>();
        for (Object e : itr) {
            int len = toKeys.length - 1;
            Map map1 = map;
            for (int i = 0; i < len; i++) {
                Object key = toKeys[i].convert(e);
                Map map2 = (Map) map1.get(key);
                if (map2 == null)
                    map1.put(key, map2 = new HashMap<>());
                map1 = map2;
            }
            map1.put(toKeys[len].convert(e), e);
        }
        return map;
    }

    public static <K, V> Map<K, V> combine(List<Map<K, V>> maps) {
        if (maps.size() == 1)
            return maps.get(0);
        return new CombineMap<K, V>(maps);
    }

    public static <K, V> Map<K, V> combine(Map<K, V>... maps) {
        return combine(Lists.from(maps));
    }

    public static <K, D, S> Map<K, D> convert(Map<K, S> map,
            Converter<? extends D, ? super S> toDst, Converter<? extends S, ? super D> toSrc) {
        return new ConvertMap<K, D, S>(map, toDst, toSrc);
    }

    public static <K, D, S> Map<K, D> convert(Map<K, S> map,
            Converter<? extends D, ? super S> conv) {
        return convert(map, conv, null);
    }

    public static <K, V> Map<K, V> writeCopy(Map<K, V> map) {
        return new WriteCopyMap<>(map);
    }

    public static <K, V> Map<K, V> readOnly(Map<K, V> map) {
        return new ReadOnlyMap<>(map);
    }

    public static <K, V> Map<K, V> array(Comparator<? super K> cmp, List<Entry<K, V>> arr) {
        return new ArrayMap<>(cmp, arr);
    }

    public static boolean equals(Map<?, ?> map1, Map<?, ?> map2) {
        if (map1 == map2)
            return true;
        if (map1.size() != map2.size())
            return false;
        for (Entry<?, ?> e : map1.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            if (value == null) {
                if (!(map2.get(key) == null && map2.containsKey(key)))
                    return false;
            } else {
                if (!value.equals(map2.get(key)))
                    return false;
            }
        }
        return true;
    }

    public static int hashCode(Map<?, ?> map) {
        int h = 0;
        for (Map.Entry<?, ?> e : map.entrySet())
            h += e.hashCode();
        return h;
    }

    public static String toString(Map<?, ?> map) {
        return new MyStringBuilder().append('{').appends(map.entrySet(), Maps.EntryToString, ",")
                .append('}').toString();
    }

    public static String toURLString(Map<String, Object> map) {
        if (map.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public static final Map emptyMap = new AbstractMap() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Set keySet() {
            return Collections.emptySet;
        }

        @Override
        public Collection values() {
            return Collections.emptySet;
        }

        @Override
        public Set entrySet() {
            return Collections.emptySet;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Map) && ((Map<?, ?>) o).isEmpty();
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };

    public static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) emptyMap;
    }

    public static final Converter MapToEntrySet = new Converter<Set, Map>() {
        @Override
        public Set convert(Map data) {
            return data.entrySet();
        }
    };
    public static final Converter MapToKeySet = new Converter<Set, Map>() {
        @Override
        public Set convert(Map data) {
            return data.keySet();
        }
    };
    public static final Converter MapToValues = new Converter<Collection, Map>() {
        @Override
        public Collection convert(Map data) {
            return data.values();
        }
    };
    public static final Converter EntryToKey = new Converter<Object, Map.Entry>() {
        @Override
        public Object convert(Map.Entry data) {
            return data.getKey();
        }
    };
    public static final Converter EntryToValue = new Converter<Object, Map.Entry>() {
        @Override
        public Object convert(Map.Entry data) {
            return data.getValue();
        }
    };
    public static final Converter EntryToString = new Converter<String, Map.Entry>() {
        @Override
        public String convert(Map.Entry data) {
            return data.getKey() + "=" + data.getValue();
        }
    };

    public static class MyEntry<K, V> implements Map.Entry<K, V> {
        public K key;
        public V value;

        public MyEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry e = (Map.Entry) o;
                return eq(key, e.getKey()) && eq(value, e.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        private static boolean eq(Object o1, Object o2) {
            return o1 == null ? o2 == null : o1.equals(o2);
        }
    }

    public static abstract class KV<K> {
        public abstract Object get(K key);

        public abstract Iterable<K> keySet();

        public <T> T getObject(K key, Converter<T, Object> conv) {
            return conv.convert(get(key));
        }

        public <T> T getObject(K key, Class<T> cls) {
            return (T) getObject(key, Converters.valueOf(cls));
        }

        public final Integer getInteger(K key) {
            return getObject(key, Converters.ToInteger);
        }

        public final Long getLong(K key) {
            return getObject(key, Converters.ToLong);
        }

        public final Short getShort(K key) {
            return getObject(key, Converters.ToShort);
        }

        public final Byte getByte(K key) {
            return getObject(key, Converters.ToByte);
        }

        public final Character getCharacter(K key) {
            return getObject(key, Converters.ToCharacter);
        }

        public final Boolean getBoolean(K key) {
            return getObject(key, Converters.ToBoolean);
        }

        public final Float getFloat(K key) {
            return getObject(key, Converters.ToFloat);
        }

        public final Double getDouble(K key) {
            return getObject(key, Converters.ToDouble);
        }

        public final String getString(K key) {
            return getObject(key, Converters.ToString);
        }

        public final JSONObject getJSONObject(K key) {
            return getObject(key, Converters.ToJSONObject);
        }

        public final JSONArray getJSONArray(K key) {
            return getObject(key, Converters.ToJSONArray);
        }

        public final byte[] getBytes(K key) {
            return getObject(key, Converters.ToBytes);
        }

        public final BigDecimal getBigDecimal(K key) {
            return getObject(key, Converters.ToBigDecimal);
        }

        public final BigInteger getBigInteger(K key) {
            return getObject(key, Converters.ToBigInteger);
        }

        public final Date getDate(K key) {
            return getObject(key, Converters.ToDate);
        }

        public final int getInteger(K key, int defaultValue) {
            Integer value = getObject(key, Converters.ToInteger);
            return value == null ? defaultValue : value;
        }

        public final long getLong(K key, long defaultValue) {
            Long value = getObject(key, Converters.ToLong);
            return value == null ? defaultValue : value;
        }

        public final short getShort(K key, short defaultValue) {
            Short value = getObject(key, Converters.ToShort);
            return value == null ? defaultValue : value;
        }

        public final byte getByte(K key, byte defaultValue) {
            Byte value = getObject(key, Converters.ToByte);
            return value == null ? defaultValue : value;
        }

        public final char getCharacter(K key, char defaultValue) {
            Character value = getObject(key, Converters.ToCharacter);
            return value == null ? defaultValue : value;
        }

        public final boolean getBoolean(K key, boolean defaultValue) {
            Boolean value = getObject(key, Converters.ToBoolean);
            return value == null ? defaultValue : value;
        }

        public final float getFloat(K key, float defaultValue) {
            Float value = getObject(key, Converters.ToFloat);
            return value == null ? defaultValue : value;
        }

        public final double getDouble(K key, double defaultValue) {
            Double value = getObject(key, Converters.ToDouble);
            return value == null ? defaultValue : value;
        }

        public final String getString(K key, String defaultValue) {
            String value = getObject(key, Converters.ToString);
            return value == null ? defaultValue : value;
        }

        public final JSONObject getJSONObject(K key, JSONObject defaultValue) {
            JSONObject value = getObject(key, Converters.ToJSONObject);
            return value == null ? defaultValue : value;
        }

        public final JSONArray getJSONArray(K key, JSONArray defaultValue) {
            JSONArray value = getObject(key, Converters.ToJSONArray);
            return value == null ? defaultValue : value;
        }
    }
    public interface MapGetter {
        public Object get(Object map, String key);
    }

    public static final MapGetter DEFAULT_MAP_GETTER = new MapGetter() {
        @Override
        public Object get(Object map, String key) {
            return ((Map<String, Object>) map).get(key);
        }
    };
    public static final MapGetter BEAN_MAP_GETTER = new MapGetter() {
        @Override
        public Object get(Object map, String key) {
            return Reflection.Util.getValue(map, Reflection.forClass(map.getClass()), key);
        }
    };
    public static final MapGetter GENERAL_MAP_GETTER = new MapGetter() {
        @Override
        public Object get(Object map, String key) {
            if (map instanceof Map)
                return DEFAULT_MAP_GETTER.get(map, key);
            else
                return BEAN_MAP_GETTER.get(map, key);
        }
    };

    public interface Tree<T> {
        public T value();

        public Collection<? extends Tree<T>> child();

        public static final Converter TO_VALUE = new Converter<Object, Tree>() {
            @Override
            public Object convert(Tree data) {
                return data.value();
            }
        };
        public static final Converter TO_CHILD = new Converter<Collection, Tree>() {
            @Override
            public Collection convert(Tree data) {
                return data.child();
            }
        };
    }
    public interface EntryNode<K, V> extends Tree<Map.Entry<K, V>>, Map.Entry<K, V> {
        @Override
        public EntryNode<K, V> value();

        @Override
        public Collection<EntryNode<K, V>> child();

        @Override
        public K getKey();

        @Override
        public V getValue();

        @Override
        public V setValue(V value);
    }
    public static abstract class SimpleEntryNode<K, V> extends MyEntry<K, V>
            implements EntryNode<K, V> {
        public SimpleEntryNode(K key, V value) {
            super(key, value);
        }

        @Override
        public EntryNode<K, V> value() {
            return this;
        }
    }
    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    static class CombineMap<K, V> extends AbstractMap<K, V> {
        private List<Map<K, V>> maps;

        public CombineMap(List<Map<K, V>> maps) {
            this.maps = maps;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.combineSet((List) Lists.convert(maps, MapToEntrySet));
        }

        @Override
        public Set<K> keySet() {
            return Collections.combineSet((List) Lists.convert(maps, MapToKeySet));
        }

        @Override
        public Collection<V> values() {
            return Collections.combineCollection((List) Lists.convert(maps, MapToValues));
        }

        @Override
        public V get(Object key) {
            int len = maps.size();
            for (int i = 0; i < len; i++) {
                V obj = maps.get(i).get(key);
                if (obj != null)
                    return obj;
            }
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            int len = maps.size();
            for (int i = 0; i < len; i++) {
                if (maps.get(i).containsKey(key))
                    return true;
            }
            return false;
        }

        @Override
        public V remove(Object key) {
            int len = maps.size();
            for (int i = 0; i < len; i++) {
                V v = maps.get(i).remove(key);
                if (v != null)
                    return v;
            }
            return null;
        }

        @Override
        public void clear() {
            int len = maps.size();
            for (int i = 0; i < len; i++) {
                maps.get(i).clear();
            }
        }

        public V put(K key, V value) {
            return maps.get(0).put(key, value);
        };
    }
    static class ConvertMap<K, D, S> extends AbstractMap<K, D> {
        private Map<K, S> map;
        private Converter<? extends D, ? super S> toDst;
        private Converter<? extends S, ? super D> toSrc;

        public ConvertMap(Map<K, S> map, Converter<? extends D, ? super S> toDst,
                Converter<? extends S, ? super D> toSrc) {
            this.map = map;
            this.toDst = toDst;
            this.toSrc = toSrc;
        }

        @Override
        public Set<Map.Entry<K, D>> entrySet() {
            return Collections.convert(map.entrySet(),
                    ConvertMap.<K, D, S>convertEntryValue(toDst));
        }

        private static <K, D, S> Converter<Map.Entry<K, D>, Map.Entry<K, S>> convertEntryValue(
                final Converter<? extends D, ? super S> conv) {
            return new Converter<Map.Entry<K, D>, Map.Entry<K, S>>() {
                @Override
                public Map.Entry<K, D> convert(Map.Entry<K, S> data) {
                    return new MyEntry(data.getKey(), conv.convert(data.getValue()));
                }
            };
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<D> values() {
            return Collections.convert(map.values(), toDst);
        }

        @Override
        public D get(Object key) {
            return toDst.convert(map.get(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public D remove(Object key) {
            return toDst.convert(map.remove(key));
        }

        @Override
        public D put(K key, D value) {
            return toDst.convert(map.put(key, toSrc.convert(value)));
        }
    }
    static class WriteCopyMap<K, V> extends AbstractMap<K, V> {
        private Map<K, V> srcMap;
        private HashMap<K, V> copyMap;

        public WriteCopyMap(Map<K, V> srcMap) {
            this.srcMap = srcMap;
            copyMap = new HashMap<K, V>();
        }

        @Override
        public V put(K key, V value) {
            copyMap.put(key, value);
            return srcMap.get(key);
        }

        @Override
        public V get(Object key) {
            return copyMap.containsKey(key) ? copyMap.get(key) : srcMap.get(key);
        }

        @Override
        public V remove(Object key) {
            return copyMap.remove(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return copyMap.containsKey(key) || srcMap.containsKey(key);
        }

        @Override
        public void clear() {
            copyMap.clear();
        }

        @Override
        public int size() {
            int size = srcMap.size();
            for (K k : copyMap.keySet()) {
                if (!srcMap.containsKey(k))
                    size++;
            }
            return size;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return Collections.combine(copyMap.entrySet().iterator(),
                            Collections.filter(srcMap.entrySet().iterator(), Filters.convert(
                                    Filters.not(Filters.containsKey(copyMap)), Maps.EntryToKey)));
                }

                @Override
                public int size() {
                    return WriteCopyMap.this.size();
                }
            };
        }

        @Override
        public Set<K> keySet() {
            return new AbstractSet<K>() {
                @Override
                public Iterator<K> iterator() {
                    return Collections.combine(copyMap.keySet().iterator(), Collections.filter(
                            srcMap.keySet().iterator(), Filters.not(Filters.containsKey(copyMap))));
                }

                @Override
                public int size() {
                    return WriteCopyMap.this.size();
                }
            };
        }

        @Override
        public Collection<V> values() {
            return Collections.convert(keySet(), Converters.map(this, null));
        }
    }
    static class ArrayMap<K, V> extends AbstractMap<K, V> {
        private Comparator<? super K> cmp;
        private List<Entry<K, V>> arr;

        public ArrayMap(Comparator<? super K> cmp, List<Entry<K, V>> arr) {
            this.cmp = cmp;
            this.arr = arr;
        }

        @Override
        public V put(K key, V value) {
            int r = search(key);
            if (r < 0) {
                arr.add(-r - 1, new MyEntry<K, V>(key, value));
                return null;
            } else {
                Entry<K, V> e = arr.get(r);
                return e.setValue(value);
            }
        }

        @Override
        public V get(Object key) {
            int r = search((K) key);
            return r < 0 ? null : arr.get(r).getValue();
        }

        @Override
        public V remove(Object key) {
            int r = search((K) key);
            return r < 0 ? null : arr.remove(r).getValue();
        }

        @Override
        public boolean containsKey(Object key) {
            return search((K) key) >= 0;
        }

        @Override
        public void clear() {
            arr.clear();
        }

        @Override
        public int size() {
            return arr.size();
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return arr.iterator();
                }

                @Override
                public int size() {
                    return arr.size();
                }
            };
        }

        private int search(K key) {
            return Lists.binarySearch(arr, null, Maps.EntryToKey, key, cmp, -1, -1);
        }
    }

    public interface HashFunction<T> {
        public int hashCode(T value);
    }

    public interface EqualsFunction<T> {
        public boolean equals(T o1, T o2);
    }

    public static final HashFunction<Object> DEFAULT_HASH_FUNCTION = new HashFunction<Object>() {
        @Override
        public int hashCode(Object value) {
            return value.hashCode();
        }
    };
    public static final EqualsFunction<Object> DEFAULT_EQUALS_FUNCTION =
            new EqualsFunction<Object>() {
                @Override
                public boolean equals(Object o1, Object o2) {
                    return o1 == o2 || o1 != null && o1.equals(o2);
                }
            };

    public static class MyHashMap<K, V> extends KV<K> implements Map<K, V>, Cloneable {
        protected static final int DEFAULT_INITIAL_CAPACITY = 16;
        protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

        protected HashFunction<? super K> hf = DEFAULT_HASH_FUNCTION;
        protected EqualsFunction<? super K> ef = DEFAULT_EQUALS_FUNCTION;
        protected HashEntry<K, V>[] table;
        protected int size;
        protected int threshold;
        protected float loadFactor;

        public MyHashMap(int capacity, float loadFactor) {
            capacity = 1 << Numbers.getBit(capacity - 1, 2);
            table = new HashEntry[capacity];
            this.loadFactor = loadFactor;
            threshold = (int) (capacity * loadFactor);
        }

        public MyHashMap(int capacity) {
            this(capacity, DEFAULT_LOAD_FACTOR);
        }

        public MyHashMap() {
            this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
        }

        @Override
        public V get(Object key) {
            HashEntry<K, V> e = getEntry((K) key);
            return e == null ? null : e.value;
        }

        @Override
        public V put(K key, V value) {
            V v = putNotResize(key, value);
            size++;
            resize();
            return v;
        }

        @Override
        public V remove(Object key) {
            int h = hash(hf.hashCode((K) key));
            int index = h & (table.length - 1);
            HashEntry<K, V> e = table[index];
            HashEntry<K, V> prev = null;
            while (e != null) {
                if (h == e.hash && ef.equals((K) key, e.key)) {
                    size--;
                    if (prev == null) {
                        table[index] = e.next;
                    } else {
                        e = prev.next;
                        prev.next = e.next;
                    }
                    return e.value;
                }
                prev = e;
                e = e.next;
            }
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            return getEntry((K) key) != null;
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }

        @Override
        public void clear() {
            Arrays.fill(table, null);
            size = 0;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        private Set<Map.Entry<K, V>> entrySet;

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            if (entrySet == null)
                entrySet = new EntrySet();
            return entrySet;
        }

        private Set<K> keySet;

        @Override
        public Set<K> keySet() {
            if (keySet == null)
                keySet = Collections.convert(entrySet(), Maps.EntryToKey);
            return keySet;
        }

        private Collection<V> values;

        @Override
        public Collection<V> values() {
            if (values == null)
                values = Collections.convert(entrySet(), Maps.EntryToValue);
            return values;
        }

        @Override
        public int hashCode() {
            return Maps.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Map && Maps.equals(this, (Map<?, ?>) obj);
        }

        @Override
        public String toString() {
            return "{" + Strings.combine(Collections.convert(entrySet(), Maps.EntryToString), ",")
                    + "}";
        }

        @Override
        public Object clone() {
            try {
                MyHashMap<K, V> result = (MyHashMap<K, V>) super.clone();
                result.keySet = null;
                result.values = null;
                result.table = new HashEntry[table.length];
                for (Map.Entry<? extends K, ? extends V> e : entrySet()) {
                    result.putNotResize(e.getKey(), e.getValue());
                }
                return result;
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }

        public void setHashFunction(HashFunction<? super K> hf) {
            this.hf = hf;
        }

        public void setEqualsFunction(EqualsFunction<? super K> ef) {
            this.ef = ef;
        }

        public int getBlockSize() {
            int size = 0;
            for (HashEntry<K, V> e : table) {
                if (e != null)
                    size++;
            }
            return size;
        }

        private HashEntry<K, V> getEntry(K key) {
            int h = hash(hf.hashCode(key));
            HashEntry<K, V> e = table[h & (table.length - 1)];
            while (e != null) {
                if (h == e.hash && ef.equals(key, e.key))
                    return e;
                e = e.next;
            }
            return null;
        }

        private void resize() {
            if (size > threshold) {
                HashEntry<K, V>[] oldTable = table;
                HashEntry<K, V>[] newTable = new HashEntry[oldTable.length * 2];
                int f = newTable.length - 1;
                for (HashEntry<K, V> e : oldTable) {
                    while (e != null) {
                        HashEntry<K, V> next = e.next;
                        int index = e.hash & f;
                        e.next = newTable[index];
                        newTable[index] = e;
                        e = next;
                    }
                }
                table = newTable;
                threshold = (int) (table.length * loadFactor);
            }
        }

        private V putNotResize(K key, V value) {
            int h = hash(hf.hashCode(key));
            int index = h & (table.length - 1);
            HashEntry<K, V> e = table[index];
            while (e != null) {
                if (h == e.hash && ef.equals(key, e.key)) {
                    return e.setValue(value);
                }
                e = e.next;
            }
            table[index] = new HashEntry<K, V>(key, value, h, table[index]);
            return null;
        }

        private static int hash(int h) {
            h ^= (h >>> 20) ^ (h >>> 12);
            return h ^ (h >>> 7) ^ (h >>> 4);
        }

        private static class HashEntry<K, V> extends Maps.MyEntry<K, V> {
            int hash;
            HashEntry<K, V> next;

            HashEntry(K key, V value, int hash, HashEntry<K, V> next) {
                super(key, value);
                this.hash = hash;
                this.next = next;
            }
        }

        private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return IReader.convert(new Itr());
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof Map.Entry) {
                    Map.Entry<K, V> e = (Map.Entry<K, V>) o;
                    V v1 = e.getValue();
                    V v2 = get(e.getKey());
                    return v1 == v2 || (v1 != null && v1.equals(v2));
                }
                return false;
            }
        }

        private class Itr extends IReader<Map.Entry<K, V>> {
            int index;
            HashEntry<K, V> e;

            @Override
            public Map.Entry<K, V> read() throws IOException {
                if (e == null) {
                    HashEntry<K, V>[] tb = table;
                    for (; index < tb.length; index++) {
                        if (tb[index] != null) {
                            e = tb[index++];
                            break;
                        }
                    }
                    if (e == null)
                        return null;
                }
                HashEntry<K, V> r = e;
                e = e.next;
                return r;
            }
        }
    }
    static class ReadOnlyMap<K, V> extends AbstractMap<K, V> {
        private Map<K, V> map;

        public ReadOnlyMap(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.readOnly(map.entrySet());
        }

        @Override
        public Set<K> keySet() {
            return Collections.readOnly(map.keySet());
        }

        @Override
        public Collection<V> values() {
            return Collections.readOnly(map.values());
        }
    }
    static class ObjectMap extends AbstractMap<String, Object> {
        private Object obj;
        private Reflection.IClass clazz;
        private Set<String> keyset;

        public ObjectMap(Object obj, Set<String> keyset) {
            this.obj = obj;
            this.clazz = Reflection.forClass(obj.getClass());
            this.keyset = keyset;
        }

        @Override
        public Object put(String key, Object value) {
            if (!keyset.isEmpty() && !keyset.contains(key))
                return null;
            Reflection.Getter gt = clazz.getGetter(key);
            if (gt == null)
                return null;
            Object oldValue = gt.get(obj);
            Reflection.Util.setValue(obj, value, clazz, key);
            return oldValue;
        }

        @Override
        public Object get(Object key) {
            if (!keyset.isEmpty() && !keyset.contains(key))
                return null;
            return Reflection.Util.getValue(obj, clazz, (String) key);
        }

        @Override
        public boolean containsKey(Object key) {
            return (keyset.isEmpty() || keyset.contains(key))
                    && clazz.getGetter((String) key) != null;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return Collections.convert(keySet(),
                    new Converter<Map.Entry<String, Object>, String>() {
                        @Override
                        public Map.Entry<String, Object> convert(String data) {
                            return new MyEntry<String, Object>(data,
                                    Reflection.Util.getValue(obj, clazz, data));
                        }
                    });
        }

        @Override
        public Set<String> keySet() {
            return keyset.isEmpty() ? (Set) clazz.getGetterMap().keySet() : keyset;
        }

        @Override
        public Collection<Object> values() {
            return Collections.convert(keySet(), new Converter<Object, String>() {
                @Override
                public Object convert(String data) {
                    return Reflection.Util.getValue(obj, clazz, data);
                }
            });
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return keyset.isEmpty() ? clazz.getGetterMap().size() : keyset.size();
        }
    }
    public static class AVLTree<K, V> extends AbstractMap<K, V> {
        protected TNode<K, V> root;
        protected Comparator<? super K> comp;
        protected int size;

        public AVLTree(Comparator<? super K> comp) {
            this.comp = comp;
        }

        public AVLTree() {
            this(Misc.DEFAULT_COMPARATOR);
        }

        public Tree<Map.Entry<K, V>> root() {
            return root;
        }

        @Override
        public V get(Object key) {
            TNode<K, V> e = getEntryAt(root, comp, (K) key);
            return e != null ? e.value : null;
        }

        @Override
        public V put(K key, V value) {
            TNode<K, V> node = root;
            if (node == null) {
                root = new TNode<K, V>(key, value, null);
                size++;
                return null;
            }
            node = getEntryNear(node, comp, key);
            int c = comp.compare(key, node.key);
            if (c < 0) {
                size++;
                fix(node.left = new TNode<K, V>(key, value, node));
                return null;
            } else if (c > 0) {
                size++;
                fix(node.right = new TNode<K, V>(key, value, node));
                return null;
            } else
                return node.setValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return getEntryAt(root, comp, (K) key) != null;
        }

        @Override
        public V remove(Object key) {
            TNode<K, V> node = getEntryAt(root, comp, (K) key);
            if (node == null)
                return null;
            size--;
            if (node.left == null) {
                if (node.right == null)
                    replaceNode(node, null);
                else
                    replaceNode(node, node.right);
            } else {
                if (node.right == null)
                    replaceNode(node, node.left);
                else {
                    TNode<K, V> last = getEntryAtLast(node.left);
                    swapEntry(node, last);
                    replaceNode(last, last.left);
                    fix(last.parent);
                    return last.value;
                }
            }
            fix(node.parent);
            return node.value;
        }

        @Override
        public void clear() {
            root = null;
            size = 0;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new EntrySet();
        }

        public IReader.DoubleIReader<Map.Entry<K, V>> readerAt(K key) {
            TNode<K, V> node = getEntryAt(root, comp, key);
            if (node == null)
                return null;
            return new EntryDoubleGetter(node);
        }

        public IReader.DoubleIReader<Map.Entry<K, V>> readerAtFirst() {
            return new EntryDoubleGetter(0);
        }

        public IReader.DoubleIReader<Map.Entry<K, V>> readerAtLast() {
            return new EntryDoubleGetter(2);
        }

        public IReader.DoubleIReader<Map.Entry<K, V>> readerNear(K key) {
            return new EntryDoubleGetter(getEntryNear(root, comp, key));
        }

        private void replaceNode(TNode<K, V> node, TNode<K, V> newNode) {
            TNode<K, V> parent = node.parent;
            if (parent == null) {
                root = newNode;
            } else if (node == parent.left)
                parent.left = newNode;
            else
                parent.right = newNode;
            if (newNode != null)
                newNode.parent = parent;
        }

        static <K, V> void fix(TNode<K, V> node) {
            while (node != null) {
                TNode<K, V> left = node.left;
                TNode<K, V> right = node.right;
                int lh = getHeight(left), rh = getHeight(right);
                int f = lh - rh;
                if (f > 1) {
                    if (getHeight(left.left) > getHeight(left.right)) {
                        TNode<K, V> leftleft = left.left;
                        swapEntry(node, left);
                        setLeft(left, left.right);
                        setRight(left, node.right);
                        setLeft(node, leftleft);
                        setRight(node, left);
                    } else {
                        TNode<K, V> leftright = left.right;
                        swapEntry(node, leftright);
                        setRight(left, leftright.left);
                        setLeft(leftright, leftright.right);
                        setRight(leftright, node.right);
                        setRight(node, leftright);
                        fixHeight(leftright);
                    }
                    fixHeight(left);
                    break;
                } else if (f < -1) {
                    if (getHeight(right.right) > getHeight(right.left)) {
                        TNode<K, V> rightright = right.right;
                        swapEntry(node, right);
                        setRight(right, right.left);
                        setLeft(right, node.left);
                        setRight(node, rightright);
                        setLeft(node, right);
                    } else {
                        TNode<K, V> rightleft = right.left;
                        swapEntry(node, rightleft);
                        setLeft(right, rightleft.right);
                        setRight(rightleft, rightleft.left);
                        setLeft(rightleft, node.left);
                        setLeft(node, rightleft);
                        fixHeight(rightleft);
                    }
                    fixHeight(right);
                    break;
                }
                int h = Math.max(lh, rh) + 1;
                if (node.height == h)
                    break;
                node.height = h;
                node = node.parent;
            }
        }

        static <K, V> int getHeight(TNode<K, V> node) {
            return node == null ? 0 : node.height;
        }

        static <K, V> boolean fixHeight(TNode<K, V> node) {
            int h = Math.max(getHeight(node.left), getHeight(node.right)) + 1;
            if (h == node.height)
                return false;
            node.height = h;
            return true;
        }

        static <K, V> void setLeft(TNode<K, V> node, TNode<K, V> left) {
            node.left = left;
            if (left != null)
                left.parent = node;
        }

        static <K, V> void setRight(TNode<K, V> node, TNode<K, V> right) {
            node.right = right;
            if (right != null)
                right.parent = node;
        }

        static <K, V> void swapEntry(TNode<K, V> node1, TNode<K, V> node2) {
            K key = node1.key;
            node1.key = node2.key;
            node2.key = key;
            V value = node1.value;
            node1.value = node2.value;
            node2.value = value;
        }

        static <K, V> TNode<K, V> getEntryAt(TNode<K, V> node, Comparator<? super K> comp, K key) {
            while (node != null) {
                int c = comp.compare(key, node.key);
                if (c < 0)
                    node = node.left;
                else if (c > 0)
                    node = node.right;
                else
                    return node;
            }
            return null;
        }

        static <K, V> TNode<K, V> getEntryAtFirst(TNode<K, V> node) {
            while (true) {
                TNode<K, V> left = node.left;
                if (left == null)
                    return node;
                node = left;
            }
        }

        static <K, V> TNode<K, V> getEntryAtLast(TNode<K, V> node) {
            while (true) {
                TNode<K, V> right = node.right;
                if (right == null)
                    return node;
                node = right;
            }
        }

        static <K, V> TNode<K, V> getEntryNear(TNode<K, V> node, Comparator<? super K> comp,
                K key) {
            TNode<K, V> tmp;
            while (true) {
                int c = comp.compare(key, node.key);
                if (c < 0)
                    tmp = node.left;
                else if (c > 0)
                    tmp = node.right;
                else
                    return node;
                if (tmp == null)
                    return node;
                node = tmp;
            }
        }

        static class TNode<K, V> extends SimpleEntryNode<K, V> {
            TNode<K, V> left;
            TNode<K, V> right;
            TNode<K, V> parent;
            int height;

            TNode(K key, V value, TNode<K, V> parent) {
                super(key, value);
                this.parent = parent;
            }

            @Override
            public Collection<EntryNode<K, V>> child() {
                ArrayList<EntryNode<K, V>> list = new ArrayList<EntryNode<K, V>>(2);
                if (left != null)
                    list.add(left);
                if (right != null)
                    list.add(right);
                return list;
            }
        }

        class EntryDoubleGetter extends IReader.StageDoubleIReader<Map.Entry<K, V>> {
            TNode<K, V> node;

            EntryDoubleGetter(int stage) {
                super(stage);
            }

            EntryDoubleGetter(TNode<K, V> node) {
                super(1);
                this.node = node;
            }

            @Override
            protected Map.Entry<K, V> firstGetNext() {
                return root == null ? null : (node = getEntryAtFirst(root));
            }

            @Override
            protected Map.Entry<K, V> midGetNext() {
                if (node.right != null)
                    return node = getEntryAtFirst(node.right);
                for (TNode<K, V> parent; (parent = node.parent) != null; node = parent)
                    if (node != parent.right)
                        return node = parent;
                return null;
            }

            @Override
            protected Map.Entry<K, V> midGetCurrent() {
                return node;
            }

            @Override
            protected Map.Entry<K, V> midGetPrev() {
                if (node.left != null)
                    return node = getEntryAtLast(node.left);
                for (TNode<K, V> parent; (parent = node.parent) != null; node = parent)
                    if (node != parent.left)
                        return node = parent;
                return null;
            }

            @Override
            protected Map.Entry<K, V> lastGetPrev() {
                return root == null ? null : (node = getEntryAtLast(root));
            }
        }

        class EntrySet extends AbstractSet<Map.Entry<K, V>> {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return IReader.convert(readerAtFirst());
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof Map.Entry) {
                    Entry<K, V> e = (Map.Entry<K, V>) o;
                    V v = get(e.getKey());
                    if (v == e.getValue() || v != null && v.equals(e.getValue()))
                        return true;
                }
                return false;
            }
        }
    }
    public static class DictTree<K, V> extends AbstractMap<List<K>, V> {
        protected Node<K, V> root;
        protected int size;
        protected Comparator<? super K> comp;

        public DictTree(Comparator<? super K> comp) {
            this.comp = comp;
            root = new Node<K, V>(comp, null);
        }

        public DictTree() {
            this(Misc.DEFAULT_COMPARATOR);
        }

        public Node<K, V> root() {
            return root;
        }

        @Override
        public V put(List<K> key, V value) {
            return put(key, null, value);
        }

        @Override
        public V get(Object key) {
            return get(key, null);
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public V remove(Object key) {
            return remove(key, null);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            root = new Node<K, V>(comp, null);
            size = 0;
        }

        @Override
        public Set<Map.Entry<List<K>, V>> entrySet() {
            return new EntrySet();
        }

        @Override
        public Collection<V> values() {
            return super.values();
        }

        public <A> EntryNode<K, V> getNode(A arr, ArrayController<K, A> ctr) {
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Node<K, V> node = root;
            for (int i = 0, len = ctr.getLength(arr); i < len; i++) {
                node = node.child.get(ctr.get(arr, i));
                if (node == null)
                    return null;
            }
            return node;
        }

        public <A> V get(A arr, ArrayController<K, A> ctr) {
            EntryNode<K, V> node = getNode(arr, ctr);
            return node == null ? null : node.getValue();
        }

        public <A> V put(A arr, ArrayController<K, A> ctr, V value) {
            if (value == null)
                throw new NullPointerException("value can not be null");
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Node<K, V> node = root, tmp;
            for (int i = 0, len = ctr.getLength(arr); i < len; i++) {
                tmp = node.child.get(ctr.get(arr, i));
                if (tmp != null)
                    node = tmp;
                else {
                    for (; i < len; i++) {
                        tmp = node;
                        K eKey = ctr.get(arr, i);
                        node = new Node<K, V>(comp, eKey);
                        tmp.child.put(eKey, node);
                    }
                    node.value = value;
                    size++;
                    return null;
                }
            }
            V old = node.value;
            if (old == null)
                size++;
            node.value = value;
            return old;
        }

        public <A> V remove(A arr, ArrayController<K, A> ctr) {
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Stack<Node<K, V>> stack = new Stack<Node<K, V>>();
            trace(arr, ctr, 0, stack);
            if (stack.size() == ctr.getLength(arr)) {
                Node<K, V> node = stack.peek();
                V old = node.value;
                if (old != null) {
                    node.value = null;
                    while (node.value == null && node.child.isEmpty()) {
                        K k = node.key;
                        stack.pop();
                        node = stack.peek();
                        node.child.remove(k);
                    }
                    size--;
                }
                return old;
            }
            return null;
        }

        public <A> MatchResult<V> match(A arr, ArrayController<K, A> ctr, int off) {
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Stack<Node<K, V>> stack = new Stack<Node<K, V>>();
            trace(arr, ctr, off, stack);
            while (!stack.isEmpty()) {
                Node<K, V> r = stack.pop();
                if (r.value != null)
                    return new MatchResult<V>(stack.size() + 1, r.value);
            }
            return null;
        }

        private <A> void trace(A arr, ArrayController<K, A> ctr, int off, Stack<Node<K, V>> stack) {
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Node<K, V> node = root;
            for (int i = off, len = ctr.getLength(arr); i < len; i++) {
                node = node.child.get(ctr.get(arr, i));
                if (node == null)
                    return;
                stack.push(node);
            }
        }

        public IReader.DoubleIReader<Map.Entry<List<K>, V>> atFirst() {
            return createIterator(new EntryGetter(0));
        }

        public IReader.DoubleIReader<Map.Entry<List<K>, V>> atLast() {
            return createIterator(new EntryGetter(2));
        }

        public <A> IReader.DoubleIReader<Map.Entry<List<K>, V>> atMatch(A arr,
                ArrayController<K, A> ctr, int off) {
            if (ctr == null)
                ctr = ArrayController.valueOf(arr);
            Node<K, V> node = root;
            Stack<IReader.DoubleIReader<Map.Entry<K, Node<K, V>>>> stack = new Stack<>();
            IReader.DoubleIReader<Map.Entry<K, Node<K, V>>> it;
            for (int i = off, len = ctr.getLength(arr); i < len; i++) {
                it = node.child.readerAt(ctr.get(arr, i));
                if (it == null)
                    break;
                stack.push(it);
                try {
                    node = it.current().getValue();
                } catch (IOException e) {
                    throw new Error();
                }
            }
            if (stack.isEmpty())
                return null;
            return createIterator(new EntryGetter(stack));
        }

        private static final Filter NotNullValue =
                Filters.convert(Filters.NotNull, Maps.EntryToValue);

        private IReader.DoubleIReader<Map.Entry<List<K>, V>> createIterator(EntryGetter getter) {
            return IReader.filter(getter, NotNullValue);
        }

        private static class Node<K, V> extends SimpleEntryNode<K, V> {
            AVLTree<K, Node<K, V>> child;

            Node(Comparator<? super K> comp, K key) {
                super(key, null);
                child = new AVLTree<K, Node<K, V>>(comp);
            }

            @Override
            public Collection<EntryNode<K, V>> child() {
                return (Collection) child.values();
            }
        }

        public static class MatchResult<V> {
            public int len;
            public V value;

            MatchResult(int len, V value) {
                this.len = len;
                this.value = value;
            }

            @Override
            public String toString() {
                return "len=" + len + ",value=" + value;
            }
        }

        class EntrySet extends AbstractSet<Map.Entry<List<K>, V>> {
            @Override
            public Iterator<Map.Entry<List<K>, V>> iterator() {
                return IReader.convert(atFirst());
            }

            @Override
            public int size() {
                return size;
            }
        }

        static class ItrToKey<K, V>
                implements Converter<K, IReader.DoubleIReader<Map.Entry<K, Node<K, V>>>> {
            @Override
            public K convert(IReader.DoubleIReader<Map.Entry<K, Node<K, V>>> data) {
                try {
                    return data.current().getKey();
                } catch (IOException e) {
                    return null;
                }
            }
        }

        static class FullEntry<K, V> extends MyEntry<List<K>, V> {
            Stack<IReader.DoubleIReader<Map.Entry<K, Node<K, V>>>> stack;

            FullEntry(Stack<IReader.DoubleIReader<Map.Entry<K, Node<K, V>>>> stack) {
                super((List<K>) Lists.convert(stack, new ItrToKey<K, V>()), null);
                this.stack = stack;
            }

            @Override
            public V getValue() {
                try {
                    return stack.peek().current().getValue().value;
                } catch (IOException e) {
                    throw new Error();
                }
            }

            @Override
            public V setValue(V value) {
                try {
                    return stack.peek().current().getValue().setValue(value);
                } catch (IOException e) {
                    throw new Error();
                }
            };
        }

        class EntryGetter extends IReader.StageDoubleIReader<Map.Entry<List<K>, V>> {
            Stack<DoubleIReader<Map.Entry<K, Node<K, V>>>> stack;
            FullEntry<K, V> entry;

            EntryGetter(int stage) {
                super(stage);
                stack = new Stack<DoubleIReader<Map.Entry<K, Node<K, V>>>>();
                entry = new FullEntry<K, V>(stack);
            }

            EntryGetter(Stack<DoubleIReader<Map.Entry<K, Node<K, V>>>> stack) {
                super(1);
                this.stack = stack;
                entry = new FullEntry<K, V>(stack);
            }

            @Override
            protected Map.Entry<List<K>, V> firstGetNext() throws IOException {
                DoubleIReader<Map.Entry<K, Node<K, V>>> it = root.child.readerAtFirst();
                Entry<K, Node<K, V>> value;
                value = it.read();
                if (value == null)
                    return null;
                stack.push(it);
                return entry;
            }

            @SuppressWarnings("resource")
            @Override
            protected Map.Entry<List<K>, V> midGetNext() throws IOException {
                DoubleIReader<Map.Entry<K, Node<K, V>>> it = stack.peek();
                AVLTree<K, Node<K, V>> child = it.current().getValue().child;
                if (child.isEmpty()) {
                    while (it.read() == null) {
                        stack.pop();
                        if (stack.isEmpty())
                            return null;
                        it = stack.peek();
                    }
                } else {
                    it = child.readerAtFirst();
                    it.read();
                    stack.push(it);
                }
                return entry;
            }

            @Override
            protected Map.Entry<List<K>, V> midGetCurrent() {
                return entry;
            }

            @Override
            protected Map.Entry<List<K>, V> midGetPrev() throws IOException {
                DoubleIReader<Map.Entry<K, Node<K, V>>> it = stack.peek();
                Entry<K, Node<K, V>> prev = it.prev();
                if (prev != null) {
                    Node<K, V> node = prev.getValue();
                    while (!node.child.isEmpty()) {
                        it = node.child.readerAtLast();
                        node = it.prev().getValue();
                        stack.push(it);
                    }
                } else {
                    stack.pop();
                    if (stack.isEmpty())
                        return null;
                }
                return entry;
            }

            @Override
            protected Map.Entry<List<K>, V> lastGetPrev() throws IOException {
                DoubleIReader<Map.Entry<K, Node<K, V>>> it;
                Node<K, V> node = root;
                while (!node.child.isEmpty()) {
                    it = node.child.readerAtLast();
                    node = it.prev().getValue();
                    stack.push(it);
                }
                if (stack.isEmpty())
                    return null;
                return entry;
            }
        }
    }
}
