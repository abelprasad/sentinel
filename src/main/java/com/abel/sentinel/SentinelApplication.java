package com.abel.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SentinelApplication {

    public static void main(String[] args) {
        System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        SpringApplication.run(SentinelApplication.class, args);
    }

}