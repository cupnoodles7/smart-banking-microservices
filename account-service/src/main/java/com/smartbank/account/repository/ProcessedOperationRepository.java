package com.smartbank.account.repository;

import com.smartbank.account.entity.ProcessedOperation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

// Stores the deposit/withdraw operations we've already applied, looked up by idempotency key.
@Repository
public interface ProcessedOperationRepository extends MongoRepository<ProcessedOperation, String> {
}
