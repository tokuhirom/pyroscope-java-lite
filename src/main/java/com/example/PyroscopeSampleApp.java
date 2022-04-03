package com.example;

import java.time.Duration;

// This is sample.
public class PyroscopeSampleApp {
    public static void main(String[] args) throws InterruptedException {
        var agent = new PyroscopeAgent(
                "http://localhost:4040",
                "lite",
                Duration.ofMillis(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        agent.start();

        Thread.sleep(200 * 1000);
    }
}
