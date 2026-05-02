package com.rentflow.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.rentflow")
@EnableJpaRepositories(basePackages = "com.rentflow")
@EntityScan(basePackages = "com.rentflow")
public class RentFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentFlowApplication.class, args);
    }
}
