package com.smartbank.wallet.repository;

import com.smartbank.wallet.entity.ProcessedRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

// Stores the requests we've already processed, looked up by idempotency key.
@Repository
public interface ProcessedRequestRepository extends MongoRepository<ProcessedRequest, String> {
}
