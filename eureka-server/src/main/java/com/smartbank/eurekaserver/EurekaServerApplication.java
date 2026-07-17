package com.smartbank.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Netflix Eureka service registry.
 *
 * <p>Every business service and the API Gateway register here on startup so that
 * requests can be routed by logical service name ({@code lb://service-name})
 * rather than hard-coded host/port pairs.
 *
 * <p>This is a platform/infrastructure service: it is self-contained, does not
 * register with itself, and does not fetch its own registry.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
