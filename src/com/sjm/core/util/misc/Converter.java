package com.sjm.core.util.misc;

public interface Converter<D, S> {
    public D convert(S data);
}
