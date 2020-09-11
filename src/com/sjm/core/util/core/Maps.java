package com.sjm.core.util.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;


/**
 * Map工具类
 */
@SuppressWarnings({"unchecked"})
public class Maps {
    public static Map<String, Object> newMap(Object... kvs) {
        return newObjectMap(kvs);
    }

    public static <K, V> Map<K, V> newObjectMap(Object... kvs) {
        if (kvs.length % 2 != 0)
            throw new IllegalArgumentException("kvs.length % 2 != 0");
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
            throw new IllegalArgumentException("kvs.length % 2 != 0");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            Object value = kvs[i + 1];
            if (value != null)
                map.put(String.valueOf(kvs[i]), String.valueOf(value));
        }
        return map;
    }

    public static <K, V> Map<K, V> putAll(Map<K, V> map, Iterable<V> values,
            Function<? super V, ? extends K> keyMapper) {
        for (V value : values)
            map.put(keyMapper.apply(value), value);
        return map;
    }

    public static <K, V, E> Map<K, V> putAll(Map<K, V> map, Iterable<E> values,
            Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        for (E e : values)
            map.put(keyMapper.apply(e), valueMapper.apply(e));
        return map;
    }

    public static <K, E> Map<K, List<E>> accumulate(Map<K, List<E>> map, Iterable<E> values,
            Function<? super E, ? extends K> keyMapper) {
        for (E e : values) {
            K key = keyMapper.apply(e);
            List<E> ls = map.get(key);
            if (ls == null)
                map.put(key, ls = new ArrayList<>());
            ls.add(e);
        }
        return map;
    }

    public static <K, V, E> Map<K, List<V>> accumulate(Map<K, List<V>> map, Iterable<E> values,
            Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        for (E e : values) {
            K key = keyMapper.apply(e);
            List<V> ls = map.get(key);
            if (ls == null)
                map.put(key, ls = new ArrayList<>());
            ls.add(valueMapper.apply(e));
        }
        return map;
    }

    public static <K, E> Map<K, List<E>> groupBy(Iterable<E> itr,
            Function<? super E, ? extends K> keyMapper) {
        return accumulate(new HashMap<>(), itr, keyMapper);
    }

    public static <K, V, E> Map<K, List<V>> groupBy(Iterable<E> values,
            Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        return accumulate(new HashMap<>(), values, keyMapper, valueMapper);
    }

    public static <K, E> Map<K, E> groupByOne(Iterable<E> values,
            Function<? super E, ? extends K> keyMapper) {
        return putAll(new HashMap<>(), values, keyMapper);
    }

    public static <K, V, E> Map<K, V> groupByOne(Iterable<E> values,
            Function<? super E, ? extends K> keyMapper,
            Function<? super E, ? extends V> valueMapper) {
        return putAll(new HashMap<>(), values, keyMapper, valueMapper);
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
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (!map.isEmpty()) {
            for (Map.Entry<?, ?> e : map.entrySet())
                sb.append(e.getKey()).append("=").append(e.getValue()).append(',');
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append('}');
        return sb.toString();
    }
}
