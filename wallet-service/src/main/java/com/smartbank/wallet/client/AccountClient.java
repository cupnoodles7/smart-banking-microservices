package com.smartbank.wallet.client;

import com.smartbank.wallet.client.dto.AccountOperationRequest;
import com.smartbank.wallet.client.dto.AccountOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Talks to the Account Service over the documented REST contract (PRD §6.7).
 * Used by top-up: debit the linked account ({@link #withdraw}), and on partial
 * failure credit it back ({@link #deposit}) — the compensating reversal of §6.16.
 *
 * <p>Load-balanced by service id via Eureka. The caller's JWT + identity headers are
 * forwarded by {@link com.smartbank.wallet.config.FeignClientConfig}.
 *
 * <p>NOTE: the Account Service is not implemented yet; this client targets its PRD
 * contract so top-up works end-to-end once that service exists.
 */
@FeignClient(name = "account-service", path = "/accounts")
public interface AccountClient {

    /** Debit the linked account (source of a top-up). */
    @PostMapping("/withdraw")
    AccountOperationResponse withdraw(@RequestBody AccountOperationRequest request);

    /** Credit the linked account (compensating reversal of a failed top-up). */
    @PostMapping("/deposit")
    AccountOperationResponse deposit(@RequestBody AccountOperationRequest request);
}
