package com.smartbank.account.controller;

import com.smartbank.account.dto.request.CreateAccountRequest;
import com.smartbank.account.dto.request.DepositRequest;
import com.smartbank.account.dto.request.TransferRequest;
import com.smartbank.account.dto.request.WithdrawRequest;
import com.smartbank.account.dto.response.AccountResponse;
import com.smartbank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts", description = "Savings and current accounts - open, deposit, withdraw, transfer (PRD sec 6.10)")
public class AccountController {

    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Create a SAVINGS or CURRENT account for the calling customer (PRD sec 6.10). */
    @PostMapping
    @Operation(summary = "Open a new account",
            description = "Creates a SAVINGS or CURRENT account for the calling customer. The owner "
                    + "is taken from the gateway-injected X-Customer-Id header, and the customer is "
                    + "verified against the User Service before the account is created.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or account type"),
            @ApiResponse(responseCode = "403", description = "Missing caller identity"),
            @ApiResponse(responseCode = "404", description = "Customer not found in User Service"),
            @ApiResponse(responseCode = "503", description = "User Service unavailable")
    })
public ResponseEntity<AccountResponse> createAccount(
        @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
        @Valid @RequestBody CreateAccountRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(accountService.createAccount(customerId, request));
}

    /** Credit an account owned by the caller (PRD sec 6.10). */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit into an account",
            description = "Credits an account owned by the caller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit applied"),
            @ApiResponse(responseCode = "400", description = "Invalid amount"),
            @ApiResponse(responseCode = "403", description = "Caller does not own this account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountResponse> deposit(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(accountService.deposit(customerId, request));
    }

    /** Debit an account owned by the caller (PRD sec 6.10). */
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from an account",
            description = "Debits an account owned by the caller, subject to balance and daily limits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal applied"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, insufficient balance, or daily limit exceeded"),
            @ApiResponse(responseCode = "403", description = "Caller does not own this account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountResponse> withdraw(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(accountService.withdraw(customerId, request));
    }

    /** Move money from an account owned by the caller to another account (PRD sec 6.10). */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer between accounts",
            description = "Moves money from an account owned by the caller to another account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer applied"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, insufficient balance, self-transfer, or daily limit exceeded"),
            @ApiResponse(responseCode = "403", description = "Caller does not own the source account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountResponse> transfer(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String customerId,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(customerId, request));
    }

    /** List all accounts owned by a customer; a caller may only list their own (PRD sec 6.10). */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List a customer's accounts",
            description = "Lists all accounts owned by a customer. A caller may only list their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts returned"),
            @ApiResponse(responseCode = "403", description = "Caller may only list their own accounts")
    })
    public ResponseEntity<List<AccountResponse>> getByCustomer(
            @RequestHeader(value = CUSTOMER_ID_HEADER, required = false) String callerCustomerId,
            @PathVariable String customerId) {
        return ResponseEntity.ok(accountService.getByCustomerId(customerId, callerCustomerId));
    }
}
