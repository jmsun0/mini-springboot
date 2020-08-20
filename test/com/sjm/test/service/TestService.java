package com.sjm.test.service;

import com.sjm.core.springboot.Component;
import com.sjm.core.springboot.PostConstruct;

@Component
public class TestService {
    @PostConstruct
    private void init() {
        System.out.println(123456);
    }
}
