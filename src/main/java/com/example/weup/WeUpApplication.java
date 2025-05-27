package com.example.weup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeUpApplication.class, args);
    }

}
