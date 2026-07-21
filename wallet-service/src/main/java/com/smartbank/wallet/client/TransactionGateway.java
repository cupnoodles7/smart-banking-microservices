package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.RecordTransactionRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Circuit-breaker guarded entry point to the Transaction Service.
// Wraps the Feign TransactionClient. Ledger recording is best-effort: if the Transaction Service
// is unavailable the fallback just logs, because the money has already moved and must not be undone.
@Component
public class TransactionGateway {

    private static final Logger log = LoggerFactory.getLogger(TransactionGateway.class);

    private final TransactionClient transactionClient;

    public TransactionGateway(TransactionClient transactionClient) {
        this.transactionClient = transactionClient;
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "recordFallback")
    public void record(RecordTransactionRequest request) {
        transactionClient.record(request);
    }

    // Best-effort: swallow the failure so the caller's flow is unaffected, but make it visible.
    private void recordFallback(RecordTransactionRequest request, Throwable t) {
        log.error("Transaction Service temporarily unavailable - ledger record skipped for idempotencyKey={}: {}",
                request.getIdempotencyKey(), t.getMessage());
    }
}
