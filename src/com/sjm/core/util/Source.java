package com.sjm.core.util;

public abstract class Source<K> {
    public abstract K next();

    public abstract CharSequence getValue();

    public static <K> Source<K> filter(final Source<K> src, final Filter<K> ft) {
        return new Source<K>() {
            @Override
            public K next() {
                K key;
                while (!ft.accept((key = src.next())));
                return key;
            }

            @Override
            public CharSequence getValue() {
                return src.getValue();
            }
        };
    }

    public static <K> void print(Source<K> src, K eof) {
        for (K key; (key = src.next()) != eof;)
            System.out.println(new MyStringBuilder().append(key).append('(')
                    .appendEscape(src.getValue(), null, -1, -1).append(')'));
    }
}
