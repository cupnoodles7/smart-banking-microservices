package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.AccountOperationRequest;
import com.smartbank.wallet.client.dto.AccountOperationResponse;
import com.smartbank.wallet.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Circuit-breaker guarded entry point to the Account Service.
// Wraps the Feign AccountClient so the existing OpenFeign mechanism is kept, while
// repeated failures trip the breaker and fail fast instead of hammering a down service.
@Component
public class AccountGateway {

    private static final Logger log = LoggerFactory.getLogger(AccountGateway.class);

    private final AccountClient accountClient;

    public AccountGateway(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "withdrawFallback")
    public AccountOperationResponse withdraw(AccountOperationRequest request) {
        return accountClient.withdraw(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "depositFallback")
    public AccountOperationResponse deposit(AccountOperationRequest request) {
        return accountClient.deposit(request);
    }

    // Fallbacks. A business rejection (HTTP 400) is re-thrown so the wallet's existing logic can
    // interpret it; anything else means the Account Service is unreachable, so we fail fast.
    private AccountOperationResponse withdrawFallback(AccountOperationRequest request, Throwable t) {
        if (t instanceof FeignException.BadRequest badRequest) {
            throw badRequest;
        }
        log.warn("Account Service unavailable on withdraw (accountId={}): {}",
                request.getAccountId(), t.getMessage());
        throw new ServiceUnavailableException("Account Service is temporarily unavailable. Please try again later.");
    }

    private AccountOperationResponse depositFallback(AccountOperationRequest request, Throwable t) {
        if (t instanceof FeignException.BadRequest badRequest) {
            throw badRequest;
        }
        log.warn("Account Service unavailable on deposit (accountId={}): {}",
                request.getAccountId(), t.getMessage());
        throw new ServiceUnavailableException("Account Service is temporarily unavailable. Please try again later.");
    }
}
