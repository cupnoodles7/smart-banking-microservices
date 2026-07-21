package com.smartbank.account.service;

import com.smartbank.account.dto.request.CreateAccountRequest;
import com.smartbank.account.dto.request.DepositRequest;
import com.smartbank.account.dto.request.TransferRequest;
import com.smartbank.account.dto.request.WithdrawRequest;
import com.smartbank.account.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(String callerCustomerId, CreateAccountRequest request);

    List<AccountResponse> getByCustomerId(String customerId, String callerCustomerId);

    AccountResponse deposit(String callerCustomerId, DepositRequest request);

    AccountResponse withdraw(String callerCustomerId, WithdrawRequest request);

    AccountResponse transfer(String callerCustomerId, TransferRequest request);
}
