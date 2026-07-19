package com.smartbank.account.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.smartbank.account.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull(message = "accountType must not be null")
    private AccountType accountType;

    @JsonCreator
    public CreateAccountRequest(AccountType accountType) {
        this.accountType = accountType;
    }

    public CreateAccountRequest() {
    }
}