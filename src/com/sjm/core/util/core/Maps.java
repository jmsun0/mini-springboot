package com.sjm.core.util.core;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;



@SuppressWarnings({"unchecked", "rawtypes"})
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

    public static <K, E> Map<K, Integer> groupByIndex(List<E> list,
            Function<? super E, ? extends K> keyMapper) {
        Map<K, Integer> map = new HashMap<>();
        for (int i = 0, len = list.size(); i < len; i++)
            map.put(keyMapper.apply(list.get(i)), i);
        return map;
    }

    public static <K, E> Map<K, Integer> groupByCount(Iterable<E> itr,
            Function<? super E, ? extends K> keyMapper) {
        Map<K, Integer> map = new HashMap<>();
        for (E e : itr) {
            K key = keyMapper.apply(e);
            Integer n = map.get(key);
            if (n == null)
                map.put(key, 1);
            else
                map.put(key, n + 1);
        }
        return map;
    }

    public static <K, V> Map<K, V> merge(List<Map<K, V>> maps) {
        if (maps.size() == 1)
            return maps.get(0);
        return new MergeMap<>(maps);
    }

    public static <K, D, S> Map<K, D> convert(Map<K, S> map,
            Function<? super S, ? extends D> valueMapper) {
        return new ConvertMap<>(map, valueMapper);
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
        return new MyStringBuilder().append('{')
                .appends(map.entrySet(), data -> data.getKey() + "=" + data.getValue(), ",")
                .append('}').toString();
    }

    public static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) emptyMap;
    }

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
            Map.Entry that;
            return this == o || o instanceof Map.Entry
                    && Objects.equals(key, (that = (Map.Entry) o).getKey())
                    && Objects.equals(value, that.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Private>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */
    private static final Map emptyMap = new AbstractMap() {
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
            return Collections.emptySet();
        }

        @Override
        public Collection values() {
            return Collections.emptySet();
        }

        @Override
        public Set entrySet() {
            return Collections.emptySet();
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

    static class MergeMap<K, V> extends AbstractMap<K, V> {
        private List<Map<K, V>> maps;

        public MergeMap(List<Map<K, V>> maps) {
            this.maps = maps;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.concatSet((List) Lists.convert(maps, Map::entrySet));
        }

        @Override
        public Set<K> keySet() {
            return Collections.concatSet((List) Lists.convert(maps, Map::keySet));
        }

        @Override
        public Collection<V> values() {
            return Collections.concatCollection((List) Lists.convert(maps, Map::values));
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
        private Function<? super S, ? extends D> valueMapper;

        public ConvertMap(Map<K, S> map, Function<? super S, ? extends D> valueMapper) {
            this.map = map;
            this.valueMapper = valueMapper;
        }

        @Override
        public Set<Map.Entry<K, D>> entrySet() {
            return Collections.convert(map.entrySet(),
                    data -> new MyEntry(data.getKey(), valueMapper.apply(data.getValue())));
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<D> values() {
            return Collections.convert(map.values(), valueMapper);
        }

        @Override
        public D get(Object key) {
            return valueMapper.apply(map.get(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
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
                    return Collections.concat(copyMap.entrySet().iterator(),
                            Collections.filter(srcMap.entrySet().iterator(),
                                    data -> !copyMap.containsKey(data.getKey())));
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
                    return Collections.concat(copyMap.keySet().iterator(), Collections.filter(
                            srcMap.keySet().iterator(), data -> !copyMap.containsKey(data)));
                }

                @Override
                public int size() {
                    return WriteCopyMap.this.size();
                }
            };
        }

        @Override
        public Collection<V> values() {
            return Collections.convert(keySet(), key -> get(key));
        }
    }

    static class ArrayMap<K, V> extends AbstractMap<K, V> {
        private Comparator<? super K> cmp;
        private List<Map.Entry<K, V>> arr;

        public ArrayMap(Comparator<? super K> cmp, List<Map.Entry<K, V>> arr) {
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
                Map.Entry<K, V> e = arr.get(r);
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
            return Lists.<Map.Entry<K, V>, K>binarySearch(arr, Map.Entry::getKey, key, cmp, -1, -1);
        }
    }

    public static class MyHashMap<K, V> implements Map<K, V>, Cloneable {
        protected static final int DEFAULT_INITIAL_CAPACITY = 16;
        protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

        protected ToIntFunction<K> hashFunc = Objects::hashCode;
        protected BiPredicate<K, K> equalsFunc = Objects::equals;
        protected HashEntry<K, V>[] table;
        protected int size;
        protected int threshold;
        protected float loadFactor;

        public MyHashMap(int capacity, float loadFactor) {
            capacity = getCapacity(capacity);
            table = new HashEntry[capacity];
            this.loadFactor = loadFactor;
            threshold = (int) (capacity * loadFactor);
        }

        public static int getCapacity(int capacity) {
            capacity--;
            int n = 0;
            while (capacity != 0) {
                capacity /= 2;
                n++;
            }
            return 1 << n;
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
            int h = hash(hashFunc.applyAsInt((K) key));
            int index = h & (table.length - 1);
            HashEntry<K, V> e = table[index];
            HashEntry<K, V> prev = null;
            while (e != null) {
                if (h == e.hash && equalsFunc.test((K) key, e.key)) {
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
                keySet = Collections.convert(entrySet(), Map.Entry::getKey);
            return keySet;
        }

        private Collection<V> values;

        @Override
        public Collection<V> values() {
            if (values == null)
                values = Collections.convert(entrySet(), Map.Entry::getValue);
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
            return Maps.toString(this);
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

        public void setHashFunction(ToIntFunction<K> hashFunc) {
            this.hashFunc = hashFunc;
        }

        public void setEqualsFunction(BiPredicate<K, K> equalsFunc) {
            this.equalsFunc = equalsFunc;
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
            int h = hash(hashFunc.applyAsInt(key));
            HashEntry<K, V> e = table[h & (table.length - 1)];
            while (e != null) {
                if (h == e.hash && equalsFunc.test(key, e.key))
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
            int h = hash(hashFunc.applyAsInt(key));
            int index = h & (table.length - 1);
            HashEntry<K, V> e = table[index];
            while (e != null) {
                if (h == e.hash && equalsFunc.test(key, e.key)) {
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
                return Collections.convert(new EntryStream());
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
        private class EntryStream implements Collections.ObjectStream<Map.Entry<K, V>> {
            private int index;
            private HashEntry<K, V> e;

            @Override
            public Map.Entry<K, V> read() {
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
}
