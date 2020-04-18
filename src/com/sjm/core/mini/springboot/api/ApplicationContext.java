package com.sjm.core.mini.springboot.api;

import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;

public class ApplicationContext {
    static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);

    private SpringApplication app;

    public ApplicationContext(SpringApplication app) {
        this.app = app;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        return (T) app.getBean(requiredType);
    }

    public Object getBean(String name) {
        return app.getBean(name);
    }
}
