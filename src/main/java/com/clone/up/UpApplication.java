package com.clone.up;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UpApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpApplication.class, args);
    }

}
