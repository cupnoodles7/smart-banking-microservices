package com.smartbank.transaction.service;

import com.smartbank.transaction.dto.request.RecordTransactionRequest;
import com.smartbank.transaction.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Business API of the immutable transaction ledger (PRD sec 6.7).
 *
 * <p>The service only records final outcomes (SUCCESS/FAILED) decided by other
 * services and reads them back. It never updates or deletes a record, never
 * touches Account/Wallet data, and never calls another service.
 */
public interface TransactionService {

    // Record an already-decided SUCCESS/FAILED outcome; idempotent on idempotencyKey.
    TransactionResponse record(RecordTransactionRequest request);

    // Single transaction by its id, or 404 if absent.
    TransactionResponse getById(String id);

    // Transactions where the account is sender or receiver, newest first.
    Page<TransactionResponse> getByAccountId(String accountId, Pageable pageable);

    // Transactions where the wallet is sender or receiver, newest first.
    Page<TransactionResponse> getByWalletId(String walletId, Pageable pageable);

    // All of a customer's transactions, newest first.
    Page<TransactionResponse> getByCustomerId(String customerId, Pageable pageable);

    // Transaction previously stored under the given idempotency key, or 404 if absent.
    TransactionResponse getByIdempotencyKey(String idempotencyKey);
}
