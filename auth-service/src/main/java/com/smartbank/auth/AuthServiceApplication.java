package com.smartbank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Auth Service (PRD sec 6.3): registration, login, JWT issue/refresh, BCrypt.
 * Owns auth_db. Tokens are verified locally at the API Gateway using the shared secret.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
