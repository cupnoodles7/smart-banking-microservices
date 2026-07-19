package com.smartbank.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Wallet Service entry point.
 *
 * <p>Owns {@code wallet_db}: stored-value wallets (Paytm-like) that hold real money,
 * linked one-to-one to an Account. Exposes create / top-up / wallet-transfer / pay-bill
 * and enforces the ₹50,000 balance cap, ₹20,000 daily transfer limit and 20 txns/day
 * limit (PRD §6.6). Registers with Eureka and pulls config from the Config Server.
 *
 * <p>{@code @EnableMongoAuditing} drives {@code createdAt}/{@code updatedAt}; optimistic
 * locking uses the {@code @Version} field on {@link com.smartbank.wallet.entity.Wallet}.
 */
@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
