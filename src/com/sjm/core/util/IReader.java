package com.sjm.core.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class IReader<T> implements Closeable {
    public abstract T read() throws IOException;

    @Override
    public void close() throws IOException {}

    @Override
    protected void finalize() throws Throwable {
        Misc.close(this);
    }

    public static <T> Iterator<T> convert(final IReader<T> it) {
        return new Iterator<T>() {
            Boolean hasNext;
            T value;

            @Override
            public boolean hasNext() {
                try {
                    if (hasNext != null)
                        return hasNext;
                    hasNext = (value = it.read()) != null;
                    return hasNext;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public T next() {
                if (hasNext()) {
                    hasNext = null;
                    return value;
                }
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <T> IReader<T> convert(final Iterator<T> it) {
        return new IReader<T>() {
            @Override
            public T read() {
                return it.hasNext() ? it.next() : null;
            }
        };
    }

    public static <D, S> IReader<D> convert(final IReader<S> it,
            final Converter<? extends D, ? super S> conv) {
        return new IReader<D>() {
            @Override
            public D read() throws IOException {
                return conv.convert(it.read());
            }
        };
    }

    public static IReader<String> convert(final BufferedReader br) {
        return new IReader<String>() {
            @Override
            public String read() throws IOException {
                return br.readLine();
            }

            @Override
            public void close() throws IOException {
                br.close();
            }
        };
    }

    public static <T> IReader<T> filter(final IReader<T> it, final Filter<? super T> ft) {
        return new IReader<T>() {
            @Override
            public T read() throws IOException {
                T value;
                while ((value = it.read()) != null)
                    if (ft.accept(value))
                        return value;
                return null;
            }
        };
    }

    public static <T> IReader<T> recursion(T root, final Converter<IReader<T>, T> conv) {
        final Stack<IReader<T>> stack = new Stack<IReader<T>>();
        IReader<T> r = conv.convert(root);
        if (r == null)
            throw new IllegalArgumentException();
        stack.push(r);
        return new IReader<T>() {
            @Override
            public T read() throws IOException {
                T next;
                while ((next = stack.peek().read()) == null) {
                    stack.pop();
                    if (stack.isEmpty())
                        return null;
                }
                IReader<T> it = conv.convert(next);
                if (it != null)
                    stack.push(it);
                return next;
            }
        };
    }

    public static <T> IReader<T> combine(final List<IReader<T>> list) {
        return new IReader<T>() {
            int index;

            @Override
            public T read() throws IOException {
                for (int len = list.size(); index < len; index++) {
                    T result = list.get(index).read();
                    if (result != null)
                        return result;
                }
                return null;
            }

            @Override
            public void close() throws IOException {
                IOException ex = null;
                for (IReader<T> r : list) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        ex = e;
                    }
                }
                if (ex != null)
                    throw ex;
            }
        };
    }

    public static final IReader emptyIReader = new IReader() {
        @Override
        public Object read() throws IOException {
            return null;
        }
    };

    public static <T> IReader<T> emptyIReader() {
        return emptyIReader;
    }

    public static abstract class DoubleIReader<T> extends IReader<T> {
        public abstract T current() throws IOException;

        public abstract T prev() throws IOException;
    }

    public static <D, S> DoubleIReader<D> convert(DoubleIReader<S> it,
            Converter<? extends D, ? super S> conv) {
        return new ConvertDoubleIReader<D, S>(it, conv);
    }

    public static <T> DoubleIReader<T> filter(DoubleIReader<T> it, Filter<? super T> ft) {
        return new FilterDoubleIReader<T>(it, ft);
    }

    public static <T, A> DoubleIReader<T> array(A arr, ArrayController<T, A> ctr, int index) {
        return new ArrayDoubleIReader<T, A>(arr, ctr, index);
    }

    public static abstract class StageDoubleIReader<T> extends DoubleIReader<T> {
        public StageDoubleIReader(int stage) {
            setStage(stage);
        }

        protected DoubleIReader<T> currentStage;

        @Override
        public T current() throws IOException {
            return currentStage.current();
        }

        @Override
        public T read() throws IOException {
            return currentStage.read();
        }

        @Override
        public T prev() throws IOException {
            return currentStage.prev();
        }

        public void setStage(int state) {
            switch (state) {
                case 0:
                    currentStage = firstStage;
                    break;
                case 1:
                    currentStage = midStage;
                    break;
                case 2:
                    currentStage = lastStage;
                    break;
                default:
                    throw new IllegalStateException("state:" + state);
            }
        }

        protected abstract T firstGetNext() throws IOException;

        protected abstract T midGetNext() throws IOException;

        protected abstract T midGetCurrent() throws IOException;

        protected abstract T midGetPrev() throws IOException;

        protected abstract T lastGetPrev() throws IOException;

        public static class DoubleIteratorAdapter<T> extends DoubleIReader<T> {
            @Override
            public T read() throws IOException {
                return null;
            }

            @Override
            public T current() throws IOException {
                return null;
            }

            @Override
            public T prev() throws IOException {
                return null;
            }
        }

        protected DoubleIReader<T> firstStage = new DoubleIteratorAdapter<T>() {
            @Override
            public T read() throws IOException {
                T value = firstGetNext();
                if (value != null)
                    setStage(1);
                return value;
            }
        };
        protected DoubleIReader<T> midStage = new DoubleIReader<T>() {
            @Override
            public T read() throws IOException {
                T value = midGetNext();
                if (value == null)
                    setStage(2);
                return value;
            }

            @Override
            public T current() throws IOException {
                return midGetCurrent();
            }

            @Override
            public T prev() throws IOException {
                T value = midGetPrev();
                if (value == null)
                    setStage(0);
                return value;
            }
        };
        protected DoubleIReader<T> lastStage = new DoubleIteratorAdapter<T>() {
            @Override
            public T prev() throws IOException {
                T value = lastGetPrev();
                if (value != null)
                    setStage(1);
                return value;
            };
        };
    }

    static class ArrayDoubleIReader<T, A> extends StageDoubleIReader<T> {
        private A arr;
        private ArrayController<T, A> ctr;
        private int i;

        public ArrayDoubleIReader(A arr, ArrayController<T, A> ctr, int index) {
            super(index < 0 ? 0 : (index >= ctr.getLength(arr) ? 2 : 1));
            this.arr = arr;
            this.ctr = ctr;
            this.i = index;
        }

        @Override
        protected T firstGetNext() {
            return ctr.getLength(arr) == 0 ? null : ctr.get(arr, i = 0);
        }

        @Override
        protected T midGetNext() {
            return i == ctr.getLength(arr) - 1 ? null : ctr.get(arr, ++i);
        }

        @Override
        protected T midGetCurrent() {
            return ctr.get(arr, i);
        }

        @Override
        protected T midGetPrev() {
            return i == 0 ? null : ctr.get(arr, --i);
        }

        @Override
        protected T lastGetPrev() {
            int size = ctr.getLength(arr);
            return size == 0 ? null : ctr.get(arr, i = size - 1);
        }
    }

    static class FilterDoubleIReader<T> extends DoubleIReader<T> {
        DoubleIReader<T> it;
        Filter<? super T> ft;

        FilterDoubleIReader(DoubleIReader<T> it, Filter<? super T> ft) {
            this.it = it;
            this.ft = ft;
        }

        @Override
        public T read() throws IOException {
            T value;
            while ((value = it.read()) != null)
                if (ft.accept(value))
                    return value;
            return null;
        }

        @Override
        public T current() throws IOException {
            return it.current();
        }

        @Override
        public T prev() throws IOException {
            T value;
            while ((value = it.prev()) != null)
                if (ft.accept(value))
                    return value;
            return null;
        }
    }

    static class ConvertDoubleIReader<D, S> extends DoubleIReader<D> {
        DoubleIReader<S> it;
        Converter<? extends D, ? super S> conv;

        ConvertDoubleIReader(DoubleIReader<S> it, Converter<? extends D, ? super S> conv) {
            this.it = it;
            this.conv = conv;
        }

        @Override
        public D read() throws IOException {
            return conv.convert(it.read());
        }

        @Override
        public D current() throws IOException {
            return conv.convert(it.current());
        }

        @Override
        public D prev() throws IOException {
            return conv.convert(it.prev());
        }
    }
}
