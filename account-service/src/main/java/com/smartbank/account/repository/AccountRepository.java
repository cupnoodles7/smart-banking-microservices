package com.smartbank.account.repository;

import com.smartbank.account.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

// Data access for accounts (PRD sec 6.12).
public interface AccountRepository extends MongoRepository<Account, String> {

    // GET /accounts/customer/{customerId} - a customer can hold multiple accounts.
    List<Account> findByCustomerId(String customerId);
}
