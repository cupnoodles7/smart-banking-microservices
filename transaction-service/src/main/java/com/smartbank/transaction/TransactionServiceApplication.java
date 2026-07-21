package com.smartbank.transaction;

import com.smartbank.transaction.config.InternalApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// Transaction Service - immutable ledger of SUCCESS/FAILED transactions.

@SpringBootApplication
@EnableDiscoveryClient // register with Eureka so it is reachable by logical name
@EnableConfigurationProperties(InternalApiProperties.class)
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
