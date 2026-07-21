package com.smartbank.transaction.service;

import com.smartbank.transaction.dto.request.RecordTransactionRequest;
import com.smartbank.transaction.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse record(RecordTransactionRequest request);

    TransactionResponse getById(String id);

    Page<TransactionResponse> getByAccountId(String accountId, Pageable pageable);

    Page<TransactionResponse> getByWalletId(String walletId, Pageable pageable);

    Page<TransactionResponse> getByCustomerId(String customerId, Pageable pageable);

    TransactionResponse getByIdempotencyKey(String idempotencyKey);
}
