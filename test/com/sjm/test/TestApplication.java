package com.sjm.test;

import com.sjm.core.springboot.SpringApplication;
import com.sjm.core.springboot.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(TestApplication.class);
        app.run(args);
    }
}
