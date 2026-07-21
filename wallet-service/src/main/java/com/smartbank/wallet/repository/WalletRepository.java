package com.smartbank.wallet.repository;

import com.smartbank.wallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Reads and writes wallets in the database - only this service touches wallet_db.
@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

    Optional<Wallet> findByLinkedAccountId(String linkedAccountId);

    Page<Wallet> findByCustomerId(String customerId, Pageable pageable);

    boolean existsByLinkedAccountId(String linkedAccountId);
}
