package com.sjm.core.util;

public interface Converter<D, S> {
    public D convert(S data);
}
