package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.AccountOperationRequest;
import com.smartbank.wallet.client.dto.AccountOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Talks to the Account Service: pulls money out of the linked bank account on top-up, and puts it back if the top-up can't be finished.
@FeignClient(name = "account-service", path = "/accounts")
public interface AccountClient {

    // Take money out of the linked account (where a top-up comes from).
    @PostMapping("/withdraw")
    AccountOperationResponse withdraw(@RequestBody AccountOperationRequest request);

    // Put money back into the linked account when a top-up couldn't be finished.
    @PostMapping("/deposit")
    AccountOperationResponse deposit(@RequestBody AccountOperationRequest request);
}
