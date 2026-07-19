package com.smartbank.wallet.controller;

import com.smartbank.wallet.constants.WalletConstants;
import com.smartbank.wallet.dto.request.CreateWalletRequest;
import com.smartbank.wallet.dto.request.PayBillRequest;
import com.smartbank.wallet.dto.request.TopupRequest;
import com.smartbank.wallet.dto.request.WalletTransferRequest;
import com.smartbank.wallet.dto.response.TransactionResult;
import com.smartbank.wallet.dto.response.WalletResponse;
import com.smartbank.wallet.service.WalletService;
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

/**
 * Wallet REST API (PRD §6.7). All routes require a valid JWT — the API Gateway
 * validates it and forwards the authenticated {@code X-Customer-Id} header.
 *
 * <p>Money-moving endpoints return HTTP 200 with a {@link TransactionResult}; a
 * business-rule violation is a {@code FAILED} result, not an HTTP error (PRD §7.3).
 */
@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<TransactionResult> topup(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody TopupRequest request) {
        return ResponseEntity.ok(walletService.topup(customerId, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResult> transfer(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody WalletTransferRequest request) {
        return ResponseEntity.ok(walletService.transfer(customerId, request));
    }

    @PostMapping("/pay-bill")
    public ResponseEntity<TransactionResult> payBill(
            @RequestHeader(WalletConstants.HEADER_CUSTOMER_ID) String customerId,
            @Valid @RequestBody PayBillRequest request) {
        return ResponseEntity.ok(walletService.payBill(customerId, request));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<WalletResponse>> listByCustomer(
            @PathVariable String customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.listByCustomer(customerId, pageable));
    }
}
