package com.smartbank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway - the single entry point for the Smart Banking System.
 *
 * <p>Responsibilities (PRD sec 6.2 / 6.8):
 * <ul>
 *   <li>Route every external request to the correct downstream service, resolved
 *       through Eureka by logical name ({@code lb://...}).</li>
 *   <li>Validate the JWT before forwarding, except on the open auth endpoints
 *       ({@code /auth/register}, {@code /auth/login}, {@code /auth/refresh}).</li>
 *   <li>Reject structural failures (missing/invalid token) with the standard
 *       error shape (PRD sec 6.9); business-rule failures are handled downstream.</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
