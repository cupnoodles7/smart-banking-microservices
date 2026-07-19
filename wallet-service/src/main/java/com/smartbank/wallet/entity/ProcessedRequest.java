package com.smartbank.wallet.entity;

import com.smartbank.wallet.dto.response.TransactionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Idempotency record — {@code wallet_db.processed_requests} (PRD §6.15).
 *
 * <p>The owning service stores each client-supplied {@code idempotencyKey} with the
 * result it produced. A repeated key returns the stored {@link TransactionResult}
 * instead of moving money a second time. The key is the document {@code _id}, so a
 * concurrent duplicate fails on a unique-key clash rather than double-processing.
 */
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
