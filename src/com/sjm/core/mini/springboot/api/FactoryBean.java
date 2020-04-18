package com.sjm.core.mini.springboot.api;

public interface FactoryBean<T> {
    public T getObject() throws Exception;

    public Class<?> getObjectType();

    public boolean isSingleton();
}
