package com.rentflow.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.rentflow")
public class RentFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentFlowApplication.class, args);
    }
}
