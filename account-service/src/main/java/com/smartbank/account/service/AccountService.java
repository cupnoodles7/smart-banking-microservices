package com.smartbank.account.service;

import com.smartbank.account.dto.request.CreateAccountRequest;
import com.smartbank.account.dto.request.DepositRequest;
import com.smartbank.account.dto.request.TransferRequest;
import com.smartbank.account.dto.request.WithdrawRequest;
import com.smartbank.account.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    // Create a new SAVINGS or CURRENT account for the calling customer.
    AccountResponse createAccount(String callerCustomerId, CreateAccountRequest request);

    // All accounts owned by a customer. Caller may only list their own accounts.
    List<AccountResponse> getByCustomerId(String customerId, String callerCustomerId);

    // Credit an account. Fails on invalid amount or if it would exceed the account's
    // maximum balance.
    AccountResponse deposit(String callerCustomerId, DepositRequest request);

    // Debit an account. Fails on invalid amount, insufficient balance, or daily limits.
    AccountResponse withdraw(String callerCustomerId, WithdrawRequest request);

    // Move money from one account to another. Fails on self-transfer, invalid
    // counterparty account, insufficient balance, or daily limits on the source account.
    AccountResponse transfer(String callerCustomerId, TransferRequest request);
}
