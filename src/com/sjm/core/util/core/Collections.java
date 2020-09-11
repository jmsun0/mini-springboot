package com.sjm.core.util.core;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 集合工具类
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Collections {
    public static <T> Iterator<T> concatIterator(Iterator<Iterator<T>> arr) {
        return new ConcatIterator<>(arr);
    }

    public static <T> Iterable<T> concatIterable(Iterable<Iterable<T>> arr) {
        return new ConcatIterable<>(arr);
    }

    public static <T> Collection<T> concatCollection(Iterable<Collection<T>> arr) {
        return new ConcatCollection<>(arr);
    }

    public static <T> Set<T> concatSet(Iterable<Set<T>> arr) {
        return new ConcatSet<>(arr);
    }

    public static <T> Iterator<T> concat(Iterator<? extends T>... arr) {
        return concatIterator((Iterator) Arrays.asList(arr).iterator());
    }

    public static <T> Iterable<T> concat(Iterable<? extends T>... arr) {
        return concatIterable((Iterable) Arrays.asList(arr));
    }

    public static <T> Collection<T> concat(Collection<? extends T>... arr) {
        return concatCollection((Iterable) Arrays.asList(arr));
    }

    public static <T> Set<T> concat(Set<? extends T>... arr) {
        return concatSet((Iterable) Arrays.asList(arr));
    }

    public static <T> Iterator<T> concat(T firstValue, Iterator<T> itr) {
        return new ConcatValueAndIterator(firstValue, itr);
    }

    public static <T, Q> Iterator<T> convert(Iterator<Q> it,
            Function<? super Q, ? extends T> func) {
        return new ConvertIterator<>(it, func);
    }

    public static <T> Iterator<T> convert(Enumeration<T> e) {
        return new EnumerationIterator<>(e);
    }

    public static <T, Q> Iterable<T> convert(Iterable<Q> it,
            Function<? super Q, ? extends T> func) {
        return new ConvertIterable<>(it, func);
    }

    public static <T, Q> Collection<T> convert(Collection<Q> c,
            Function<? super Q, ? extends T> func) {
        return new ConvertCollection<>(c, func);
    }

    public static <T, Q> Set<T> convert(Set<Q> c, Function<? super Q, ? extends T> func) {
        return new ConvertSet<>(c, func);
    }

    public static <T> Iterator<T> filter(Iterator<T> it, Predicate<? super T> p) {
        return new FilterIterator<>(it, p);
    }

    public static <T> Iterable<T> filter(Iterable<T> it, Predicate<? super T> p) {
        return new FilterIterable<>(it, p);
    }

    public static <T> Collection<T> addAll(Collection<T> c, Iterable<T> itr) {
        for (T value : itr)
            c.add(value);
        return c;
    }

    public static <T, E> Collection<T> addAll(Collection<T> c, Iterable<E> itr,
            Function<? super E, ? extends T> func) {
        for (E value : itr)
            c.add(func.apply(value));
        return c;
    }

    public static <T> void removeAll(Collection<T> c, Iterable<T> itr) {
        for (T value : itr)
            c.remove(value);
    }

    public static <T> Iterator<T> emptyIterator() {
        return emptyIterator;
    }

    public static <T> Set<T> emptySet() {
        return emptySet;
    }

    public static <T> Iterator<T> readOnly(final Iterator<T> it) {
        return new ReadOnlyIterator<>(it);
    }

    public static <T> Iterable<T> readOnly(final Iterable<T> itr) {
        return new ReadOnlyIterable<>(itr);
    }

    public static <T> Collection<T> readOnly(final Collection<T> col) {
        return new ReadOnlyCollection<>(col);
    }

    public static <T> Set<T> readOnly(final Set<T> set) {
        return new ReadOnlySet<>(set);
    }

    public interface ObjectStream<T> {
        public T read();
    }

    public static <T> Iterator<T> convert(ObjectStream<T> input) {
        return new ObjectStreamIterator<>(input);
    }

    public static <T> ObjectStream<T> convert(Iterator<T> it) {
        return new IteratorObjectStream<>(it);
    }

    public static <D, S> ObjectStream<D> convert(ObjectStream<S> input,
            Function<? super S, ? extends D> func) {
        return new ConvertObjectStream<>(input, func);
    }

    public static <T> ObjectStream<T> filter(ObjectStream<T> input, Predicate<? super T> p) {
        return new FilterObjectStream<>(input, p);
    }

    public static <T> ObjectStream<T> recursion(T root, Function<T, ObjectStream<T>> func) {
        return new RecursionObjectStream<>(root, func);
    }

    public static <T> ObjectStream<T> concat(List<ObjectStream<T>> list) {
        return new ConcatObjectStream<>(list);
    }

    public static <T> ObjectStream<T> emptyObjectStream() {
        return emptyObjectStream;
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

        public DiscardIterator(Iterator<T> itr, int maxDiscard) {
            this.itr = itr;
            this.queue = new DiscardQueue<>(maxDiscard);
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

    public static abstract class AbstractIterator<T> implements Iterator<T> {
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Private>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    static class ConcatIterator<T> implements Iterator<T> {
        Iterator<Iterator<T>> iit;
        Iterator<T> it;

        ConcatIterator(Iterator<Iterator<T>> iit) {
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

    static class ConcatIterable<T> implements Iterable<T> {
        Iterable<Iterable<T>> its;

        ConcatIterable(Iterable<Iterable<T>> its) {
            this.its = its;
        }

        @Override
        public Iterator<T> iterator() {
            return concatIterator(convert(its, Iterable::iterator).iterator());
        }
    }

    static class ConcatCollection<T> extends AbstractCollection<T> {
        Iterable<Collection<T>> cs;

        ConcatCollection(Iterable<Collection<T>> cs) {
            this.cs = cs;
        }

        @Override
        public Iterator<T> iterator() {
            return concatIterator(convert(cs, Iterable::iterator).iterator());
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

    static class ConcatSet<T> extends AbstractSet<T> {
        Iterable<Set<T>> ss;
        Set<T> set = new HashSet<T>();

        ConcatSet(Iterable<Set<T>> ss) {
            this.ss = ss;
        }

        @Override
        public Iterator<T> iterator() {
            return filter(concatIterator(convert(ss, Iterable::iterator).iterator()),
                    value -> !set.contains(value));
        }

        @Override
        public int size() {
            return set.size();//
        }
    }

    static class ConvertIterator<T, Q> implements Iterator<T> {
        Iterator<Q> it;
        Function<? super Q, ? extends T> func;

        public ConvertIterator(Iterator<Q> it, Function<? super Q, ? extends T> func) {
            this.it = it;
            this.func = func;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return func.apply(it.next());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    static class ConvertIterable<T, Q> implements Iterable<T> {
        Iterable<Q> it;
        Function<? super Q, ? extends T> func;

        ConvertIterable(Iterable<Q> it, Function<? super Q, ? extends T> func) {
            this.it = it;
            this.func = func;
        }

        @Override
        public Iterator<T> iterator() {
            return convert(it.iterator(), func);
        }
    }

    static class EnumerationIterator<T> extends AbstractIterator<T> {
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
    }

    static class ConvertCollection<T, Q> extends AbstractCollection<T> {
        Collection<Q> c;
        Function<? super Q, ? extends T> func;

        ConvertCollection(Collection<Q> c, Function<? super Q, ? extends T> func) {
            this.c = c;
            this.func = func;
        }

        @Override
        public Iterator<T> iterator() {
            return convert(c.iterator(), func);
        }

        @Override
        public int size() {
            return c.size();
        }
    }

    static class ConvertSet<T, Q> extends AbstractSet<T> {
        Set<Q> c;
        Function<? super Q, ? extends T> func;

        ConvertSet(Set<Q> c, Function<? super Q, ? extends T> func) {
            this.c = c;
            this.func = func;
        }

        @Override
        public Iterator<T> iterator() {
            return convert(c.iterator(), func);
        }

        @Override
        public int size() {
            return c.size();
        }
    }

    static class FilterIterator<T> extends AbstractIterator<T> {
        Iterator<T> it;
        Predicate<? super T> p;
        boolean hasNext;
        T next;

        FilterIterator(Iterator<T> it, Predicate<? super T> p) {
            this.it = it;
            this.p = p;
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
                if (p.test(value)) {
                    hasNext = true;
                    next = value;
                    return;
                }
            }
            hasNext = false;
        }
    }

    static class FilterIterable<T> implements Iterable<T> {
        Iterable<T> it;
        Predicate<? super T> p;

        FilterIterable(Iterable<T> it, Predicate<? super T> p) {
            this.it = it;
            this.p = p;
        }

        @Override
        public Iterator<T> iterator() {
            return filter(it.iterator(), p);
        }
    }
    static class ConcatValueAndIterator<T> extends AbstractIterator<T> {
        private T firstValue;
        private Iterator<T> itr;

        public ConcatValueAndIterator(T firstValue, Iterator<T> itr) {
            this.firstValue = firstValue;
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            if (firstValue != null)
                return true;
            return itr.hasNext();
        }

        @Override
        public T next() {
            if (firstValue != null) {
                T result = firstValue;
                firstValue = null;
                return result;
            }
            return itr.next();
        }
    }

    private static final Iterator emptyIterator = new Iterator() {
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
    private static final Set emptySet = new AbstractSet() {
        private Object[] emptyObjectArray = {};

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
            return emptyObjectArray;
        }

        @Override
        public Object[] toArray(Object[] a) {
            return a;
        }
    };

    static class ReadOnlyIterator<T> implements Iterator<T> {
        private Iterator<T> it;

        public ReadOnlyIterator(Iterator<T> it) {
            this.it = it;
        }

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
    }
    static class ReadOnlyIterable<T> implements Iterable<T> {
        private Iterable<T> itr;

        public ReadOnlyIterable(Iterable<T> itr) {
            this.itr = itr;
        }

        @Override
        public Iterator<T> iterator() {
            return readOnly(itr.iterator());
        }
    }
    static class ReadOnlyCollection<T> extends AbstractCollection<T> {
        private Collection<T> col;

        public ReadOnlyCollection(Collection<T> col) {
            this.col = col;
        }

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
    }
    static class ReadOnlySet<T> extends AbstractSet<T> {
        private Set<T> set;

        public ReadOnlySet(Set<T> set) {
            this.set = set;
        }

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
    }

    private static final ObjectStream emptyObjectStream = new ObjectStream() {
        @Override
        public Object read() {
            return null;
        }
    };

    static class ObjectStreamIterator<T> extends AbstractIterator<T> {
        private Boolean hasNext;
        private T value;
        private ObjectStream<T> input;

        public ObjectStreamIterator(ObjectStream<T> input) {
            this.input = input;
        }

        @Override
        public boolean hasNext() {
            if (hasNext != null)
                return hasNext;
            hasNext = (value = input.read()) != null;
            return hasNext;
        }

        @Override
        public T next() {
            if (hasNext()) {
                hasNext = null;
                return value;
            }
            return null;
        }
    }
    static class IteratorObjectStream<T> implements ObjectStream<T> {
        private Iterator<T> it;

        public IteratorObjectStream(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public T read() {
            return it.hasNext() ? it.next() : null;
        }
    }
    static class ConvertObjectStream<D, S> implements ObjectStream<D> {
        private ObjectStream<S> input;
        private Function<? super S, ? extends D> func;

        public ConvertObjectStream(ObjectStream<S> input, Function<? super S, ? extends D> func) {
            this.input = input;
            this.func = func;
        }

        @Override
        public D read() {
            return func.apply(input.read());
        }
    }
    static class FilterObjectStream<T> implements ObjectStream<T> {
        private ObjectStream<T> input;
        private Predicate<? super T> p;

        public FilterObjectStream(ObjectStream<T> input, Predicate<? super T> p) {
            this.input = input;
            this.p = p;
        }

        @Override
        public T read() {
            T value;
            while ((value = input.read()) != null)
                if (p.test(value))
                    return value;
            return null;
        }
    }
    static class RecursionObjectStream<T> implements ObjectStream<T> {
        private Stack<ObjectStream<T>> stack = new Stack<>();
        private Function<T, ObjectStream<T>> func;

        public RecursionObjectStream(T root, Function<T, ObjectStream<T>> func) {
            this.func = func;
            ObjectStream<T> r = func.apply(root);
            if (r == null)
                throw new IllegalArgumentException();
            stack.push(r);
        }

        @Override
        public T read() {
            T next;
            while ((next = stack.peek().read()) == null) {
                stack.pop();
                if (stack.isEmpty())
                    return null;
            }
            ObjectStream<T> it = func.apply(next);
            if (it != null)
                stack.push(it);
            return next;
        }
    }
    static class ConcatObjectStream<T> implements ObjectStream<T> {
        private List<ObjectStream<T>> list;
        private int index;

        public ConcatObjectStream(List<ObjectStream<T>> list) {
            super();
            this.list = list;
        }

        @Override
        public T read() {
            for (int len = list.size(); index < len; index++) {
                T result = list.get(index).read();
                if (result != null)
                    return result;
            }
            return null;
        }
    }
}
