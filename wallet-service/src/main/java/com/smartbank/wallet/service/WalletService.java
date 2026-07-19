package com.smartbank.wallet.service;

import com.smartbank.wallet.dto.request.CreateWalletRequest;
import com.smartbank.wallet.dto.request.PayBillRequest;
import com.smartbank.wallet.dto.request.TopupRequest;
import com.smartbank.wallet.dto.request.WalletTransferRequest;
import com.smartbank.wallet.dto.response.TransactionResult;
import com.smartbank.wallet.dto.response.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Wallet business operations (PRD §6.3, §6.7).
 */
public interface WalletService {

    WalletResponse createWallet(String customerId, CreateWalletRequest request);

    TransactionResult topup(String customerId, TopupRequest request);

    TransactionResult transfer(String customerId, WalletTransferRequest request);

    TransactionResult payBill(String customerId, PayBillRequest request);

    Page<WalletResponse> listByCustomer(String customerId, Pageable pageable);
}
