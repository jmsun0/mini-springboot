package com.sjm.core.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeReference<T> {
    private Type type;

    protected TypeReference() {
        type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
