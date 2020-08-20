package com.sjm.core.util.core;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class Cache {
    public static final Cache INSTANCE = Cache.newInstance();
    public static final Cache LOCAL_INSTANCE = Cache.newThreadLocalInstance();

    public static Cache newInstance(boolean threadSafe, Supplier<Map<?, ?>> supplier) {
        return threadSafe ? new ThreadSafeCache(supplier) : new ThreadUnsafeCache(supplier);
    }

    public static Cache newInstance(boolean threadSafe) {
        return newInstance(threadSafe, WeakHashMap::new);
    }

    public static Cache newInstance() {
        return newInstance(true);
    }

    public static Cache newThreadLocalInstance(Supplier<Cache> supplier) {
        return new ThreadLocalCache(supplier);
    }

    public static Cache newThreadLocalInstance() {
        return newThreadLocalInstance(Cache::newInstance);
    }

    /**
     * 第一个数字为参数总个数，第二个数字为缓存key个数（从左开始），第三个数字为maker参数个数（从右开始）
     */
    public <V, K1> V get1_1_1(Cache.Maker1<V, K1> maker, K1 k1) {
        return get(k1, 1, maker);
    }

    public <V, K1, K2> V get2_1_2(Cache.Maker2<V, K1, K2> maker, K1 k1, K2 k2) {
        return get(k1, k2, 1, maker);
    }

    public <V, K1, K2> V get2_2_1(Cache.Maker1<V, K2> maker, K1 k1, K2 k2) {
        return get(k1, k2, 2, maker);
    }

    public <V, K1, K2, K3> V get3_2_2(Cache.Maker2<V, K2, K3> maker, K1 k1, K2 k2, K3 k3) {
        return get(k1, k2, k3, 2, maker);
    }

    public <V, K1, K2, K3> V get3_2_3(Cache.Maker3<V, K1, K2, K3> maker, K1 k1, K2 k2, K3 k3) {
        return get(k1, k2, k3, 2, maker);
    }

    public <V, K1, K2, K3> V get3_3_2(Cache.Maker2<V, K2, K3> maker, K1 k1, K2 k2, K3 k3) {
        return get(k1, k2, k3, 3, maker);
    }

    public <V, K1, K2, K3> V get3_3_3(Cache.Maker3<V, K1, K2, K3> maker, K1 k1, K2 k2, K3 k3) {
        return get(k1, k2, k3, 3, maker);
    }

    public <V, K1, K2, K3, K4> V get4_3_3(Cache.Maker3<V, K2, K3, K4> maker, K1 k1, K2 k2, K3 k3,
            K4 k4) {
        return get(k1, k2, k3, k4, 3, maker);
    }

    public <V, K1, K2, K3, K4> V get4_3_4(Cache.Maker4<V, K1, K2, K3, K4> maker, K1 k1, K2 k2,
            K3 k3, K4 k4) {
        return get(k1, k2, k3, k4, 3, maker);
    }

    public <V, K1, K2, K3, K4> V get4_4_3(Cache.Maker3<V, K2, K3, K4> maker, K1 k1, K2 k2, K3 k3,
            K4 k4) {
        return get(k1, k2, k3, k4, 4, maker);
    }

    public <V, K1, K2, K3, K4, K5> V get5_4_5(Cache.Maker5<V, K1, K2, K3, K4, K5> maker, K1 k1,
            K2 k2, K3 k3, K4 k4, K5 k5) {
        return get(k1, k2, k3, k4, k5, 4, maker);
    }

    public <V, K1, K2, K3, K4, K5> V get5_4_4(Cache.Maker4<V, K2, K3, K4, K5> maker, K1 k1, K2 k2,
            K3 k3, K4 k4, K5 k5) {
        return get(k1, k2, k3, k4, k5, 4, maker);
    }

    public <V, K1, K2, K3, K4, K5, K6> V get6_5_6(Cache.Maker6<V, K1, K2, K3, K4, K5, K6> maker,
            K1 k1, K2 k2, K3 k3, K4 k4, K5 k5, K6 k6) {
        return get(k1, k2, k3, k4, k5, k6, 5, maker);
    }

    public <V, K1, K2, K3, K4, K5, K6> V get6_5_5(Cache.Maker5<V, K2, K3, K4, K5, K6> maker, K1 k1,
            K2 k2, K3 k3, K4 k4, K5 k5, K6 k6) {
        return get(k1, k2, k3, k4, k5, k6, 5, maker);
    }

    protected abstract <V> V get(int totalCount, Object k1, Object k2, Object k3, Object k4,
            Object k5, Object k6, int keyCount, Maker<V> maker);

    private <V> V get(Object k1, int keyCount, Maker<V> maker) {
        return get(1, k1, null, null, null, null, null, keyCount, maker);
    }

    private <V> V get(Object k1, Object k2, int keyCount, Maker<V> maker) {
        return get(2, k1, k2, null, null, null, null, keyCount, maker);
    }

    private <V> V get(Object k1, Object k2, Object k3, int keyCount, Maker<V> maker) {
        return get(3, k1, k2, k3, null, null, null, keyCount, maker);
    }

    private <V> V get(Object k1, Object k2, Object k3, Object k4, int keyCount, Maker<V> maker) {
        return get(4, k1, k2, k3, k4, null, null, keyCount, maker);
    }

    private <V> V get(Object k1, Object k2, Object k3, Object k4, Object k5, int keyCount,
            Maker<V> maker) {
        return get(5, k1, k2, k3, k4, k5, null, keyCount, maker);
    }

    private <V> V get(Object k1, Object k2, Object k3, Object k4, Object k5, Object k6,
            int keyCount, Maker<V> maker) {
        return get(6, k1, k2, k3, k4, k5, k6, keyCount, maker);
    }

    private static final int MAX_KEY_COUNT = 6;

    static class ThreadSafeCache extends Cache {
        protected CacheKey key = new CacheKey(new Object[MAX_KEY_COUNT]);
        protected Map<CacheKey, CacheValue> map;

        public ThreadSafeCache(Supplier<Map<?, ?>> supplier) {
            map = (Map<CacheKey, CacheValue>) supplier.get();
        }

        @Override
        protected <V> V get(int totalCount, Object k1, Object k2, Object k3, Object k4, Object k5,
                Object k6, int keyCount, Maker<V> maker) {
            CacheKey newKeys;
            CacheValue cv;
            synchronized (this) {
                key.set(keyCount, k1, k2, k3, k4, k5, k6);
                if ((cv = map.get(key)) != null) {
                    Object value = cv.value;
                    if (value != null)
                        return (V) value;
                    else
                        newKeys = new CacheKey(key);
                } else {
                    map.put(newKeys = new CacheKey(key), cv = new CacheValue());
                }
            }
            synchronized (cv) {
                if (cv.value == null)
                    cv.value = maker.make(newKeys.keys, totalCount);
            }
            return (V) cv.value;
        }
    }
    static class ThreadUnsafeCache extends Cache {
        protected CacheKey key = new CacheKey(new Object[MAX_KEY_COUNT]);
        protected Map<CacheKey, Object> map;

        public ThreadUnsafeCache(Supplier<Map<?, ?>> supplier) {
            map = (Map<CacheKey, Object>) supplier.get();
        }

        @Override
        protected <V> V get(int totalCount, Object k1, Object k2, Object k3, Object k4, Object k5,
                Object k6, int keyCount, Maker<V> maker) {
            key.set(keyCount, k1, k2, k3, k4, k5, k6);
            Object value = map.get(key);
            if (value == null)
                map.put(new CacheKey(key), value = maker.make(key.keys, totalCount));
            return (V) value;
        }
    }
    static class ThreadLocalCache extends Cache {
        private ThreadLocal<Cache> cacheLocal = new ThreadLocal<>();
        private Supplier<Cache> supplier;

        public ThreadLocalCache(Supplier<Cache> supplier) {
            this.supplier = supplier;
        }

        @Override
        protected <V> V get(int totalCount, Object k1, Object k2, Object k3, Object k4, Object k5,
                Object k6, int keyCount, Maker<V> maker) {
            Cache cache = cacheLocal.get();
            if (cache == null)
                cacheLocal.set(cache = supplier.get());
            return cache.get(totalCount, k1, k2, k3, k4, k5, k6, keyCount, maker);
        }
    }
    public interface Maker<V> {
        public V make(Object[] ks, int i);
    }
    public interface Maker1<V, K1> extends Maker<V> {
        public V make(K1 k1);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 1]);
        }
    }
    public interface Maker2<V, K1, K2> extends Maker<V> {
        public V make(K1 k1, K2 k2);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 2], (K2) ks[i - 1]);
        }
    }
    public interface Maker3<V, K1, K2, K3> extends Maker<V> {
        public V make(K1 k1, K2 k2, K3 k3);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 3], (K2) ks[i - 2], (K3) ks[i - 1]);
        }
    }
    public interface Maker4<V, K1, K2, K3, K4> extends Maker<V> {
        public V make(K1 k1, K2 k2, K3 k3, K4 k4);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 4], (K2) ks[i - 3], (K3) ks[i - 2], (K4) ks[i - 1]);
        }
    }
    public interface Maker5<V, K1, K2, K3, K4, K5> extends Maker<V> {
        public V make(K1 k1, K2 k2, K3 k3, K4 k4, K5 k5);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 5], (K2) ks[i - 4], (K3) ks[i - 3], (K4) ks[i - 2],
                    (K5) ks[i - 1]);
        }
    }
    public interface Maker6<V, K1, K2, K3, K4, K5, K6> extends Maker<V> {
        public V make(K1 k1, K2 k2, K3 k3, K4 k4, K5 k5, K6 k6);

        @Override
        default V make(Object[] ks, int i) {
            return make((K1) ks[i - 6], (K2) ks[i - 5], (K3) ks[i - 4], (K4) ks[i - 3],
                    (K5) ks[i - 2], (K6) ks[i - 1]);
        }
    }
    static class CacheValue {
        public Object value;
    }
    static class CacheKey {
        public Object[] keys;
        public int count;

        public CacheKey(Object[] keys, int count) {
            this.keys = keys;
            this.count = count;
        }

        public CacheKey(Object[] keys) {
            this(keys, keys.length);
        }

        public CacheKey(CacheKey key) {
            this(key.keys.clone(), key.count);
        }

        @Override
        public int hashCode() {
            return hashCode(keys, count);
        }

        private static int hashCode(Object[] keys, int count) {
            int result = 1;
            for (int i = 0; i < count; i++) {
                Object element = keys[i];
                result = 31 * result + (element == null ? 0 : element.hashCode());
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            CacheKey that = (CacheKey) obj;
            return equals(keys, count, that.keys, that.count);
        }

        private static boolean equals(Object[] keys1, int count1, Object[] keys2, int count2) {
            if (count1 != count2)
                return false;
            for (int i = 0; i < count1; i++) {
                Object k1 = keys1[i];
                Object k2 = keys2[i];
                if (!(k1 == null ? k2 == null : k1.equals(k2)))
                    return false;
            }
            return true;
        }

        public void set(int count, Object k1, Object k2, Object k3, Object k4, Object k5,
                Object k6) {
            this.count = count;
            keys[0] = k1;
            keys[1] = k2;
            keys[2] = k3;
            keys[3] = k4;
            keys[4] = k5;
            keys[5] = k6;
        }
    }
}
