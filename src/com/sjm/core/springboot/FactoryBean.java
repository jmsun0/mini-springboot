package com.sjm.core.springboot;

public interface FactoryBean<T> {
    public T getObject() throws Exception;

    public Class<?> getObjectType();

    public boolean isSingleton();
}
