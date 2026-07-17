package com.smartbank.transaction.repository;

import com.smartbank.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

// Data access for the immutable ledger (PRD sec 6.12). Read + append only.
// Ordering (initiatedAt DESC) is supplied by the caller via the Pageable's Sort.
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    // Idempotency check: has this key already been recorded? (PRD sec 6.15)
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Transactions initiated by a given account/wallet id.
    Page<Transaction> findBySenderId(String senderId, Pageable pageable);

    // Transactions received by a given account/wallet id.
    Page<Transaction> findByReceiverId(String receiverId, Pageable pageable);

    // An account/wallet appears on either side, so match sender OR receiver.
    Page<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId, Pageable pageable);

    // All transactions owned by a customer.
    Page<Transaction> findByCustomerId(String customerId, Pageable pageable);
}
