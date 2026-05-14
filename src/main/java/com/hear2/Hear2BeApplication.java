package com.hear2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Hear2BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(Hear2BeApplication.class, args);
    }

}
