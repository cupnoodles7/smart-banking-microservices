package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.RecordTransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Reports every finished wallet transaction - whether it worked or not - to the Transaction Service so it's on the ledger.
@FeignClient(name = "transaction-service", path = "/transactions")
public interface TransactionClient {

    @PostMapping("/internal")
    void record(@RequestBody RecordTransactionRequest request);
}
