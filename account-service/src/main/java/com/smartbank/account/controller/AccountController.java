package com.smartbank.account.controller;

import com.smartbank.account.dto.request.CreateAccountRequest;
import com.smartbank.account.dto.request.DepositRequest;
import com.smartbank.account.dto.request.TransferRequest;
import com.smartbank.account.dto.request.WithdrawRequest;
import com.smartbank.account.dto.response.AccountResponse;
import com.smartbank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Create a SAVINGS or CURRENT account for the calling customer (PRD sec 6.10). */
    @PostMapping
public ResponseEntity<AccountResponse> createAccount(
        @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
        @Valid @RequestBody CreateAccountRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(accountService.createAccount(customerId, request));
}

    /** Credit an account owned by the caller (PRD sec 6.10). */
    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(accountService.deposit(customerId, request));
    }

    /** Debit an account owned by the caller (PRD sec 6.10). */
    @PostMapping("/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(accountService.withdraw(customerId, request));
    }

    /** Move money from an account owned by the caller to another account (PRD sec 6.10). */
    @PostMapping("/transfer")
    public ResponseEntity<AccountResponse> transfer(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(customerId, request));
    }

    /** List all accounts owned by a customer; a caller may only list their own (PRD sec 6.10). */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getByCustomer(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String callerCustomerId,
            @PathVariable String customerId) {
        return ResponseEntity.ok(accountService.getByCustomerId(customerId, callerCustomerId));
    }
}
