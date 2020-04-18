package com.sjm.core.mini.springboot.ext;

import com.sjm.core.mini.springboot.api.FactoryBean;

public class AnnotationBeanDefinition {
    public String name;
    public Class<? extends FactoryBean<?>> factoryClass;
    public Object[] constructorArgs;

    public AnnotationBeanDefinition(String name, Class<? extends FactoryBean<?>> factoryClass,
            Object... constructorArgs) {
        this.factoryClass = factoryClass;
        this.constructorArgs = constructorArgs;
    }

    public AnnotationBeanDefinition() {}
}
