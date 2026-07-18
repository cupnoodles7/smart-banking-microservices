package com.smartbank.user;

import com.smartbank.user.config.InternalApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service - owns the customer profile only (fullName, email, phone, address);
 * no balances, accounts, or wallets (PRD sec 6.3 / 6.4).
 *
 * <p>Registers with Eureka and pulls configuration (port, MongoDB URI) from the
 * Config Server, mirroring the platform conventions used by the other modules.
 * The service has no outbound dependencies (PRD sec 6.3), so {@code client/} stays empty.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(InternalApiProperties.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
