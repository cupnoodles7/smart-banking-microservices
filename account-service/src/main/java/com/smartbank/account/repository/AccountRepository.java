package com.smartbank.account.repository;

import com.smartbank.account.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {

    List<Account> findByCustomerId(String customerId);
}
