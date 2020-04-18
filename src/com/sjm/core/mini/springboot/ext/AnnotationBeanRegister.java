package com.sjm.core.mini.springboot.ext;

import java.lang.annotation.Annotation;

public interface AnnotationBeanRegister<A extends Annotation> {
    public AnnotationBeanDefinition register(A ann, Class<?> clazz);
}
