package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.RecordTransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Writes completed ledger entries to the Transaction Service (PRD §6.7
 * POST /transactions/internal). Both SUCCESS and FAILED transactions are recorded
 * and never updated afterwards (PRD §6.5).
 *
 * <p>Ledger writes are best-effort (PRD §6.16): if this fails after money already
 * moved, the caller logs at ERROR and still reports success to the client — it must
 * not roll back money that has moved. See {@code WalletServiceImpl#recordLedger}.
 *
 * <p>NOTE: the Transaction Service is not implemented yet; this client targets its
 * PRD contract.
 */
@FeignClient(name = "transaction-service", path = "/transactions")
public interface TransactionClient {

    @PostMapping("/internal")
    void record(@RequestBody RecordTransactionRequest request);
}
