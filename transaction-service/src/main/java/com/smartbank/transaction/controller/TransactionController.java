package com.smartbank.transaction.controller;

import com.smartbank.transaction.constants.TransactionConstants;
import com.smartbank.transaction.dto.request.RecordTransactionRequest;
import com.smartbank.transaction.dto.response.TransactionResponse;
import com.smartbank.transaction.security.InternalApiKeyGuard;
import com.smartbank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Immutable ledger of SUCCESS/FAILED transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final InternalApiKeyGuard internalApiKeyGuard;

    public TransactionController(TransactionService transactionService,
                                InternalApiKeyGuard internalApiKeyGuard) {
        this.transactionService = transactionService;
        this.internalApiKeyGuard = internalApiKeyGuard;
    }

    @PostMapping("/internal")
    @Operation(summary = "Record a completed transaction",
            description = "Stores an already-decided SUCCESS or FAILED outcome. Requires the "
                    + "X-Internal-Api-Key header. Idempotent on idempotencyKey.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction recorded (or existing one returned)"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid internal API key"),
            @ApiResponse(responseCode = "409", description = "Conflicting duplicate write")
    })
    public ResponseEntity<TransactionResponse> record(
            @RequestHeader(name = TransactionConstants.INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @Valid @RequestBody RecordTransactionRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.record(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by id",
            description = "Retrieves a single transaction by its transaction id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "List transactions for an account",
            description = "Retrieves transactions where the account appears as sender or receiver, "
                    + "newest first.")
    @ApiResponse(responseCode = "200", description = "Page of transactions")
    public ResponseEntity<Page<TransactionResponse>> getByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getByAccountId(accountId, pageable(page, size)));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "List transactions for a wallet",
            description = "Retrieves transactions where the wallet appears as sender or receiver, "
                    + "newest first.")
    @ApiResponse(responseCode = "200", description = "Page of transactions")
    public ResponseEntity<Page<TransactionResponse>> getByWallet(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getByWalletId(walletId, pageable(page, size)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List transactions for a customer",
            description = "Retrieves all transactions owned by the customer, newest first.")
    @ApiResponse(responseCode = "200", description = "Page of transactions")
    public ResponseEntity<Page<TransactionResponse>> getByCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getByCustomerId(customerId, pageable(page, size)));
    }

    // Pagination with the ledger's fixed ordering: initiatedAt descending (PRD sec 6.7).
    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, TransactionConstants.SORT_FIELD));
    }
}
