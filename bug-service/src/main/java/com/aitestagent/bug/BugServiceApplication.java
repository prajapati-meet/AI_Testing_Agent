package com.aitestagent.bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BugServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BugServiceApplication.class, args);
    }
}
