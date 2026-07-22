package com.smartbank.account.entity;

import com.smartbank.account.dto.response.AccountResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Remembers a deposit/withdraw we've already applied, keyed by its idempotency key, so a retry
// (e.g. the Wallet Service resending a refund after a timeout) replays the stored result instead
// of moving money a second time. The @Id gives us a unique constraint for free.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_operations")
public class ProcessedOperation {

    @Id
    private String idempotencyKey;

    private String accountId;

    private AccountResponse result;

    private Instant createdAt;
}
