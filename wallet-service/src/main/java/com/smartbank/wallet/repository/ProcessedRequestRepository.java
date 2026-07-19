package com.smartbank.wallet.repository;

import com.smartbank.wallet.entity.ProcessedRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Idempotency-key store (PRD §6.15). Keyed by the client-supplied idempotencyKey.
 */
@Repository
public interface ProcessedRequestRepository extends MongoRepository<ProcessedRequest, String> {
}
