package com.smartbank.wallet.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

// What the Account Service sends back when a deposit or withdrawal works. If we even get this object, the operation succeeded.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountOperationResponse {
    // The account id (the account service calls this "id").
    private String id;
    // The account balance after the operation.
    private BigDecimal balance;
}
