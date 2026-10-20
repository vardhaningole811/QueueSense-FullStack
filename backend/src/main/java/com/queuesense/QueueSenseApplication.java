package com.queuesense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueueSenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueSenseApplication.class, args);
    }
}
