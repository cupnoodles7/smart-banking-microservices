package com.smartbank.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server.
 *
 * <p>Serves centralized, Git-backed configuration to every other service in the
 * Smart Banking System. Services fetch their configuration on startup via
 * {@code spring.config.import=configserver:http://localhost:8888}.
 *
 * <p>This is a platform/infrastructure service and is deliberately self-contained:
 * it does not itself pull configuration from another config server, so it can be the
 * first process to boot.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
