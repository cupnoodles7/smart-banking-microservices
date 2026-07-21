package com.smartbank.wallet.service;

import com.smartbank.wallet.dto.request.CreateWalletRequest;
import com.smartbank.wallet.dto.request.PayBillRequest;
import com.smartbank.wallet.dto.request.TopupRequest;
import com.smartbank.wallet.dto.request.WalletTransferRequest;
import com.smartbank.wallet.dto.response.TransactionResult;
import com.smartbank.wallet.dto.response.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// The things a wallet can do: create one, top it up, transfer, pay a bill, and list a customer's wallets.
public interface WalletService {

    WalletResponse createWallet(String customerId, CreateWalletRequest request);

    TransactionResult topup(String customerId, TopupRequest request);

    TransactionResult transfer(String customerId, WalletTransferRequest request);

    TransactionResult payBill(String customerId, PayBillRequest request);

    Page<WalletResponse> listByCustomer(String callerCustomerId, String targetCustomerId, Pageable pageable);
}
