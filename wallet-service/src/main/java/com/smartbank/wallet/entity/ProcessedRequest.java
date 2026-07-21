package com.smartbank.wallet.entity;

import com.smartbank.wallet.dto.response.TransactionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Remembers a request we've already handled, keyed by its idempotency key, so a retry
// replays the same result instead of moving money twice.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_requests")
public class ProcessedRequest {

    @Id
    private String idempotencyKey;

    private TransactionResult result;

    private Instant createdAt;
}
