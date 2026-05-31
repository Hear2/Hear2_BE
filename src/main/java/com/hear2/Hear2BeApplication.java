package com.hear2;

import com.hear2.calendar.service.GoogleCalendarProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(GoogleCalendarProperties.class)
public class Hear2BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(Hear2BeApplication.class, args);
    }

}
