package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;

// This is sample.
@SpringBootApplication
public class PyroscopeSampleApp {
    public static void main(String[] args) throws InterruptedException {
        var agent = new PyroscopeAgent(
                "http://localhost:4040",
                "lite3",
                Duration.ofMillis(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        agent.start();

        SpringApplication.run(PyroscopeSampleApp.class, args);
    }
}
