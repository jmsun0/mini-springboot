package com.sjm.core.util;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


@SuppressWarnings({"unchecked", "rawtypes"})
public class Collections {
    public static <T> Iterator<T> combineIterator(Iterator<Iterator<T>> arr) {
        return new CombineIterator<T>(arr);
    }

    public static <T> Iterable<T> combineIterable(Iterable<Iterable<T>> arr) {
        return new CombineIterable<T>(arr);
    }

    public static <T> Collection<T> combineCollection(Iterable<Collection<T>> arr) {
        return new CombineCollection<T>(arr);
    }

    public static <T> Set<T> combineSet(Iterable<Set<T>> arr) {
        return new CombineSet<T>(arr);
    }

    public static <T> Iterator<T> combine(Iterator<? extends T>... arr) {
        return combineIterator((Iterator) Lists.from(arr).iterator());
    }

    public static <T> Iterable<T> combine(Iterable<? extends T>... arr) {
        return combineIterable((List) Lists.from(arr));
    }

    public static <T> Collection<T> combine(Collection<? extends T>... arr) {
        return combineCollection((List) Lists.from(arr));
    }

    public static <T> Set<T> combine(Set<? extends T>... arr) {
        return combineSet((List) Lists.from(arr));
    }

    public static <T> Iterator<T> combine(T value, Iterator<T> itr) {
        return new CombineValueAndIterator(value, itr);
    }

    public static <T, Q> Iterator<T> convert(Iterator<Q> it,
            Converter<? extends T, ? super Q> conv) {
        return new ConvertIterator<T, Q>(it, conv);
    }

    public static <T> Iterator<T> convert(Enumeration<T> e) {
        return new EnumerationIterator<T>(e);
    }

    public static <T, Q> Iterable<T> convert(Iterable<Q> it,
            Converter<? extends T, ? super Q> conv) {
        return new ConvertIterable<T, Q>(it, conv);
    }

    public static <T, Q> Collection<T> convert(Collection<Q> c,
            Converter<? extends T, ? super Q> conv) {
        return new ConvertCollection<T, Q>(c, conv);
    }

    public static <T, Q> Set<T> convert(Set<Q> c, Converter<? extends T, ? super Q> conv) {
        return new ConvertSet<T, Q>(c, conv);
    }

    public static <T> Iterator<T> filter(Iterator<T> it, Filter<? super T> ft) {
        return new FilterIterator<T>(it, ft);
    }

    public static <T> Iterable<T> filter(Iterable<T> it, Filter<? super T> ft) {
        return new FilterIterable<T>(it, ft);
    }

    public static <T> Collection<T> addAll(Collection<T> c, Iterable<T> itr) {
        for (T value : itr)
            c.add(value);
        return c;
    }

    public static <T, E> Collection<T> addAll(Collection<T> c, Iterable<E> itr,
            Converter<T, E> conv) {
        for (E value : itr)
            c.add(conv.convert(value));
        return c;
    }

    public static <T> void removeAll(Collection<T> c, Iterable<T> itr) {
        for (T value : itr)
            c.remove(value);
    }

    public static Iterable<?> toIterable(Object data) {
        if (data instanceof Iterable)
            return (Iterable<?>) data;
        if (data.getClass().isArray())
            return Lists.from(data);
        if (data instanceof Map)
            return ((Map) data).entrySet();
        return emptySet;
    }

    public static <T> Set<T> toHashSet(Iterable<T> it) {
        HashSet<T> set = new LinkedHashSet<>();
        for (T obj : it)
            set.add(obj);
        return set;
    }

    public static final Iterator emptyIterator = new Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new NoSuchElementException();
        }
    };

    public static <T> Iterator<T> emptyIterator() {
        return emptyIterator;
    }

    public static final Set emptySet = new AbstractSet() {
        @Override
        public Iterator iterator() {
            return emptyIterator;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public boolean containsAll(Collection c) {
            return c.isEmpty();
        }

        @Override
        public Object[] toArray() {
            return Lists.emptyObjectArray;
        }

        @Override
        public Object[] toArray(Object[] a) {
            return a;
        }
    };

    public static <T> Set<T> emptySet() {
        return emptySet;
    }

    public static <T> Iterator<T> readOnly(final Iterator<T> it) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <T> Iterable<T> readOnly(final Iterable<T> itr) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return readOnly(itr.iterator());
            }
        };
    }

    public static <T> Collection<T> readOnly(final Collection<T> col) {
        return new AbstractCollection<T>() {
            @Override
            public Iterator<T> iterator() {
                return readOnly(col.iterator());
            }

            @Override
            public int size() {
                return col.size();
            }

            @Override
            public boolean contains(Object o) {
                return col.contains(o);
            }
        };
    }

    public static <T> Set<T> readOnly(final Set<T> set) {
        return new AbstractSet<T>() {
            @Override
            public Iterator<T> iterator() {
                return readOnly(set.iterator());
            }

            @Override
            public int size() {
                return set.size();
            }

            @Override
            public boolean contains(Object o) {
                return set.contains(o);
            }
        };
    }

    public interface DoubleIterator<T> extends Iterator<T> {
        public T current();

        public boolean hasNext();

        public T next();

        public boolean hasPrev();

        public T prev();
    }

    public static class DiscardQueue<T> extends AbstractList<T> {
        private T[] buffer;
        private int offset;
        private boolean full;

        public DiscardQueue(int size) {
            this.buffer = (T[]) new Object[size];
        }

        @Override
        public T get(int index) {
            if (full) {
                index += offset;
                if (index >= buffer.length)
                    index -= buffer.length;
            }
            return buffer[index];
        }

        @Override
        public int size() {
            return full ? buffer.length : offset;
        }

        public void push(T value) {
            buffer[offset++] = value;
            if (offset >= buffer.length) {
                offset = 0;
                if (!full)
                    full = true;
            }
        }
    }

    // 可回退迭代器
    public static class DiscardIterator<T> extends AbstractIterator<T> {
        private Iterator<T> itr;
        private DiscardQueue<T> queue;
        private int discard;

        public DiscardIterator(Iterator<T> itr, int size) {
            this.itr = itr;
            this.queue = new DiscardQueue<>(size);
        }

        @Override
        public boolean hasNext() {
            return discard > 0 || itr.hasNext();
        }

        @Override
        public T next() {
            if (discard == 0) {
                T value = itr.next();
                queue.push(value);
                return value;
            } else {
                return queue.get(queue.size() - (discard--));
            }
        }

        public int maxDiscard() {
            return queue.size();
        }

        public void discard(int count) {
            discard += count;
            if (discard < 0 || discard >= queue.size())
                throw new IllegalArgumentException("discard overflow");
        }

        public List<T> getQueueData() {
            return queue;
        }
    }


    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>以下均为具体实现>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */
    static class CombineIterator<T> implements Iterator<T> {
        Iterator<Iterator<T>> iit;
        Iterator<T> it;

        CombineIterator(Iterator<Iterator<T>> iit) {
            this.iit = iit;
            if (iit.hasNext())
                it = iit.next();
            else
                it = emptyIterator();
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = it.hasNext();
            if (hasNext == false) {
                if (iit.hasNext()) {
                    it = iit.next();
                    hasNext = it.hasNext();
                }
            }
            return hasNext;
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    static class CombineIterable<T> implements Iterable<T> {
        Iterable<Iterable<T>> its;

        CombineIterable(Iterable<Iterable<T>> its) {
            this.its = its;
        }

        @Override
        public Iterator<T> iterator() {
            return new CombineIterator<T>(convert(its, IterableToIterator).iterator());
        }
    }

    static class CombineCollection<T> extends AbstractCollection<T> {
        Iterable<Collection<T>> cs;

        CombineCollection(Iterable<Collection<T>> cs) {
            this.cs = cs;
        }

        @Override
        public Iterator<T> iterator() {
            return new CombineIterator<T>(convert(cs, IterableToIterator).iterator());
        }

        @Override
        public int size() {
            int size = 0;
            for (Collection<T> c : cs) {
                size += c.size();
            }
            return size;
        }
    }

    static class CombineSet<T> extends AbstractSet<T> {
        Iterable<Set<T>> ss;
        Set<T> set = new HashSet<T>();

        CombineSet(Iterable<Set<T>> ss) {
            this.ss = ss;
        }

        @Override
        public Iterator<T> iterator() {
            return new SetIterator<T>(
                    new CombineIterator<T>(convert(ss, IterableToIterator).iterator()), set);
        }

        @Override
        public int size() {
            return set.size();//
        }
    }

    static class ConvertIterator<T, Q> implements Iterator<T> {
        Iterator<Q> it;
        Converter<? extends T, ? super Q> conv;

        public ConvertIterator(Iterator<Q> it, Converter<? extends T, ? super Q> conv) {
            this.it = it;
            this.conv = conv;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return conv.convert(it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    static class ConvertIterable<T, Q> implements Iterable<T> {
        Iterable<Q> it;
        Converter<? extends T, ? super Q> conv;

        ConvertIterable(Iterable<Q> it, Converter<? extends T, ? super Q> conv) {
            this.it = it;
            this.conv = conv;
        }

        @Override
        public Iterator<T> iterator() {
            return new ConvertIterator<T, Q>(it.iterator(), conv);
        }
    }

    static class EnumerationIterator<T> implements Iterator<T> {
        Enumeration<T> e;

        EnumerationIterator(Enumeration<T> e) {
            this.e = e;
        }

        @Override
        public boolean hasNext() {
            return e.hasMoreElements();
        }

        @Override
        public T next() {
            return e.nextElement();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static class ConvertCollection<T, Q> extends AbstractCollection<T> {
        Collection<Q> c;
        Converter<? extends T, ? super Q> conv;

        ConvertCollection(Collection<Q> c, Converter<? extends T, ? super Q> conv) {
            this.c = c;
            this.conv = conv;
        }

        @Override
        public Iterator<T> iterator() {
            return new ConvertIterator<T, Q>(c.iterator(), conv);
        }

        @Override
        public int size() {
            return c.size();
        }
    }

    static class ConvertSet<T, Q> extends AbstractSet<T> {
        Set<Q> c;
        Converter<? extends T, ? super Q> conv;

        ConvertSet(Set<Q> c, Converter<? extends T, ? super Q> conv) {
            this.c = c;
            this.conv = conv;
        }

        @Override
        public Iterator<T> iterator() {
            return new ConvertIterator<T, Q>(c.iterator(), conv);
        }

        @Override
        public int size() {
            return c.size();
        }
    }

    static class FilterIterator<T> implements Iterator<T> {
        Iterator<T> it;
        Filter<? super T> ft;
        boolean hasNext;
        T next;

        FilterIterator(Iterator<T> it, Filter<? super T> ft) {
            this.it = it;
            this.ft = ft;
            findNext();
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            if (hasNext) {
                T value = next;
                findNext();
                return value;
            }
            return null;
        }

        void findNext() {
            while (it.hasNext()) {
                T value = it.next();
                if (ft.accept(value)) {
                    hasNext = true;
                    next = value;
                    return;
                }
            }
            hasNext = false;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static class FilterIterable<T> implements Iterable<T> {
        Iterable<T> it;
        Filter<? super T> ft;

        FilterIterable(Iterable<T> it, Filter<? super T> ft) {
            this.it = it;
            this.ft = ft;
        }

        @Override
        public Iterator<T> iterator() {
            return new FilterIterator<T>(it.iterator(), ft);
        }
    }

    static class SetIterator<T> extends FilterIterator<T> {
        SetIterator(Iterator<T> it, Set<T> set) {
            super(it, new SetIteratorFilter<T>(set));
        }
    }

    static class SetIteratorFilter<T> implements Filter<T> {
        Set<T> set;

        SetIteratorFilter(Set<T> set) {
            this.set = set;
        }

        @Override
        public boolean accept(T value) {
            return !set.contains(value);
        }
    }

    static Converter IterableToIterator = new Converter<Iterator, Iterable>() {
        @Override
        public Iterator convert(Iterable data) {
            return data.iterator();
        }
    };

    public static abstract class AbstractIterator<T> implements Iterator<T> {
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static class CombineValueAndIterator<T> extends AbstractIterator<T> {
        private T value;
        private Iterator<T> itr;

        public CombineValueAndIterator(T value, Iterator<T> itr) {
            this.value = value;
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            if (value != null)
                return true;
            return itr.hasNext();
        }

        @Override
        public T next() {
            if (value != null) {
                T result = value;
                value = null;
                return result;
            }
            return itr.next();
        }
    }
}
