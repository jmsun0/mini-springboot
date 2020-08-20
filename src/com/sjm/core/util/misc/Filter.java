package com.sjm.core.util.misc;

public interface Filter<T> {
    public boolean accept(T data);
}
