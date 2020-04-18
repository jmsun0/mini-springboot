package com.sjm.core.util;

public interface Filter<T> {
    public boolean accept(T data);
}
