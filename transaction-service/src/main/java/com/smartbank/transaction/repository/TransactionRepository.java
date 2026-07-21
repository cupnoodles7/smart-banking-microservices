package com.smartbank.transaction.repository;

import com.smartbank.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySenderId(String senderId, Pageable pageable);

    Page<Transaction> findByReceiverId(String receiverId, Pageable pageable);

    Page<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId, Pageable pageable);

    Page<Transaction> findByCustomerId(String customerId, Pageable pageable);
}
