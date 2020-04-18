package com.sjm.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public class Filters {
    public static <T> Filter<T> and(final Filter<? super T> f1, final Filter<? super T> f2) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) && f2.accept(value);
            }
        };
    }

    public static <T> Filter<T> and(final Filter<? super T> f1, final Filter<? super T> f2,
            final Filter<? super T> f3) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) && f2.accept(value) && f3.accept(value);
            }
        };
    }

    public static <T> Filter<T> and(final Filter<? super T> f1, final Filter<? super T> f2,
            final Filter<? super T> f3, final Filter<? super T> f4) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) && f2.accept(value) && f3.accept(value) && f4.accept(value);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Filter<T> and(final Filter<? super T>... fs) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                for (Filter<? super T> f : fs) {
                    if (!f.accept(value))
                        return false;
                }
                return true;
            }
        };
    }

    public static <T> Filter<T> or(final Filter<? super T> f1, final Filter<? super T> f2) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) || f2.accept(value);
            }
        };
    }

    public static <T> Filter<T> or(final Filter<? super T> f1, final Filter<? super T> f2,
            final Filter<? super T> f3) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) || f2.accept(value) || f3.accept(value);
            }
        };
    }

    public static <T> Filter<T> or(final Filter<? super T> f1, final Filter<? super T> f2,
            final Filter<? super T> f3, final Filter<? super T> f4) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return f1.accept(value) || f2.accept(value) || f3.accept(value) || f4.accept(value);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Filter<T> or(final Filter<? super T>... fs) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                for (Filter<? super T> f : fs) {
                    if (f.accept(value))
                        return true;
                }
                return false;
            }
        };
    }

    public static <T> Filter<T> not(final Filter<T> f) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return !f.accept(value);
            }
        };
    }

    public static <T, Q> Filter<Q> convert(final Filter<T> f, final Converter<T, ? super Q> conv) {
        return new Filter<Q>() {
            @Override
            public boolean accept(Q value) {
                return f.accept(conv.convert(value));
            }
        };
    }

    public static <T> Filter<T> equal(final T obj) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return obj.equals(value);
            }
        };
    }

    public static Filter<Integer> equal(final int v) {
        return new Filter<Integer>() {
            @Override
            public boolean accept(Integer value) {
                return value == v;
            }
        };
    }

    public static Filter<Character> equal(final char v) {
        return new Filter<Character>() {
            @Override
            public boolean accept(Character value) {
                return value == v;
            }
        };
    }

    public static Filter<Character> equalIgnoreCase(final char v) {
        return new Filter<Character>() {
            char ch = Strings.toLowCase(v);

            @Override
            public boolean accept(Character value) {
                return Strings.toLowCase(value) == ch;
            }
        };
    }

    public static Filter<Character> equals(final char... cs) {
        return new Filter<Character>() {
            @Override
            public boolean accept(Character value) {
                return Strings.equals(value, cs);
            }
        };
    }

    public static Filter<Character> equalsIgnoreCase(final char... cs) {
        return new Filter<Character>() {
            @Override
            public boolean accept(Character value) {
                return Strings.equalsIgnoreCase(value, cs);
            }
        };
    }

    public static <T> Filter<T> identity(final T obj) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return value == obj;
            }
        };
    }

    public static Filter<Integer> range(final int min, final int max) {
        return new Filter<Integer>() {
            @Override
            public boolean accept(Integer value) {
                return value >= min && value <= max;
            }
        };
    }

    public static Filter<Character> range(final char min, final char max) {
        return new Filter<Character>() {
            @Override
            public boolean accept(Character value) {
                return value >= min && value <= max;
            }
        };
    }

    public static <T> Filter<T> contains(final Collection<T> col) {
        return new Filter<T>() {
            @Override
            public boolean accept(T value) {
                return col.contains(value);
            }
        };
    }

    public static Filter<String> contains(final Filter<Character> f) {
        return new Filter<String>() {
            @Override
            public boolean accept(String data) {
                int len = data.length();
                for (int i = 0; i < len; i++) {
                    if (f.accept(data.charAt(i)))
                        return true;
                }
                return false;
            }
        };
    }

    public static Filter<String> contains(final String str) {
        return new Filter<String>() {
            @Override
            public boolean accept(String data) {
                return data.contains(str);
            }
        };
    }

    public static <K, V> Filter<K> containsKey(final Map<K, V> map) {
        return new Filter<K>() {
            @Override
            public boolean accept(K value) {
                return map.containsKey(value);
            }
        };
    }

    public static <T> Filter<Collection<T>> collection(final Filter<T> f) {
        return new Filter<Collection<T>>() {
            @Override
            public boolean accept(Collection<T> value) {
                for (T v : value)
                    if (!f.accept(v))
                        return false;
                return true;
            }
        };
    }

    public static <T> Filter<Collection<T>> collectionSize(final int size) {
        return new Filter<Collection<T>>() {
            @Override
            public boolean accept(Collection<T> value) {
                return value.size() == size;
            }
        };
    }

    public static Filter<Character> ascii(final String asc) {
        return new Filter<Character>() {
            boolean[] cs = new boolean[128];

            {
                int len = asc.length();
                for (int i = 0; i < len; i++) {
                    char c = asc.charAt(i);
                    if (c < 128) {
                        cs[c] = true;
                    } else
                        throw new IllegalArgumentException("非法字符:" + c);
                }
            }

            @Override
            public boolean accept(Character c) {
                return c < 128 ? cs[c] : false;
            }
        };
    }

    public static Filter<String> indexCharFilter(final Filter<Character> f, final int index) {
        return new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return f.accept(s.charAt(index));
            }
        };
    }

    public static Filter<String> lastIndexCharFilter(final Filter<Character> f, final int index) {
        return new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return f.accept(s.charAt(s.length() - 1 - index));
            }
        };
    }

    public static Filter<String> indexString(final String str, final int index) {
        return new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return s.startsWith(str, index);
            }
        };
    }

    public static Filter<String> lastIndexString(final String str, final int index) {
        return new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return s.startsWith(str, s.length() - str.length() - index);
            }
        };
    }

    public static Filter<String> regex(final String regex) {
        return new Filter<String>() {
            Pattern patten;
            {
                patten = Pattern.compile(regex);
            }

            @Override
            public boolean accept(String value) {
                return patten.matcher(value).find();
            }
        };
    }

    public static final Filter<Object> IsNull = new Filter<Object>() {
        @Override
        public boolean accept(Object value) {
            return value == null;
        }
    };
    public static final Filter<Object> NotNull = new Filter<Object>() {
        @Override
        public boolean accept(Object value) {
            return value != null;
        }
    };
    public static final Filter<Object> True = new Filter<Object>() {
        @Override
        public boolean accept(Object value) {
            return true;
        }
    };
    public static final Filter<Object> False = new Filter<Object>() {
        @Override
        public boolean accept(Object value) {
            return false;
        }
    };

    public static final Filter<Character> IsNumber = range('0', '9');
    public static final Filter<Character> IsLowCase = range('a', 'z');
    public static final Filter<Character> IsUpCase = range('A', 'Z');
    public static final Filter<Character> IsChar = Filters.or(IsLowCase, IsUpCase);
    public static final Filter<Character> IsLine = equal('_');
    public static final Filter<Character> IsNumberCharLine = Filters.or(IsNumber, IsChar, IsLine);
    public static final Filter<Character> IsVisible = ascii(Strings.VisibleChars);
    public static final Filter<Character> IsSpecial = ascii(Strings.SpecialChars);
    public static final Filter<Character> IsBlank = ascii(Strings.BlankChars);
    public static final Filter<Character> IsNamed = ascii(Strings.NamedChars);
    public static final Filter<Character> IsChinese = range('\u4e00', '\u9fbb');

    public static final Filter<String> IsEmptyString = new Filter<String>() {
        @Override
        public boolean accept(String value) {
            return value.isEmpty();
        }
    };
    public static final Filter<String> NotEmptyString = Filters.not(IsEmptyString);
}
