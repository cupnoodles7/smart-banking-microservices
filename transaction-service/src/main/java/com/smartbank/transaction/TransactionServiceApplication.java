package com.smartbank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// Transaction Service - immutable ledger of SUCCESS/FAILED transactions (PRD sec 6.3).
// It records outcomes decided by other services; it never calls them for validation.
@SpringBootApplication
@EnableDiscoveryClient // register with Eureka so it is reachable by logical name
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
