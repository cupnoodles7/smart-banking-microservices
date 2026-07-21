package com.smartbank.wallet.controller;

import com.smartbank.wallet.constants.WalletConstants;
import com.smartbank.wallet.dto.request.CreateWalletRequest;
import com.smartbank.wallet.dto.request.PayBillRequest;
import com.smartbank.wallet.dto.request.TopupRequest;
import com.smartbank.wallet.dto.request.WalletTransferRequest;
import com.smartbank.wallet.dto.response.TransactionResult;
import com.smartbank.wallet.dto.response.WalletResponse;
import com.smartbank.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The wallet's HTTP endpoints. Money-moving calls return 200 with a result - a rejected
// business rule shows up as a FAILED result, not as an error.
@Tag(name = "Wallets", description = "Create wallets and move money - top up, transfer, and pay bills.")
@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Open a new wallet", description = "Creates a wallet linked to a bank account. One wallet per account.")
    @PostMapping
    public ResponseEntity<WalletResponse> create(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Top up a wallet", description = "Pulls money from the linked bank account into the wallet.")
    @PostMapping("/topup")
    public ResponseEntity<TransactionResult> topup(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody TopupRequest request) {
        return ResponseEntity.ok(walletService.topup(customerId, request));
    }

    @Operation(summary = "Transfer between wallets", description = "Moves money from your wallet to another wallet.")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResult> transfer(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody WalletTransferRequest request) {
        return ResponseEntity.ok(walletService.transfer(customerId, request));
    }

    @Operation(summary = "Pay a bill", description = "Pays a bill from the wallet to a merchant.")
    @PostMapping("/pay-bill")
    public ResponseEntity<TransactionResult> payBill(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody PayBillRequest request) {
        return ResponseEntity.ok(walletService.payBill(customerId, request));
    }

    @Operation(summary = "List a customer's wallets", description = "Lists wallets for a customer. You can only list your own.")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<WalletResponse>> listByCustomer(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String callerCustomerId,
            @PathVariable String customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.listByCustomer(callerCustomerId, customerId, pageable));
    }
}
