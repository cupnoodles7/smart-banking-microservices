package com.smartbank.wallet.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Account Service deposit/withdraw result. Fields are read leniently
 * ({@code status}/{@code failureReason} as Strings) so the wallet service is not
 * coupled to the account service's enum packages across the wire.
 *
 * <p>Per PRD §7.3 a business failure (e.g. insufficient balance) comes back as
 * HTTP 200 with {@code status = "FAILED"}, not an HTTP error.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountOperationResponse {
    private String status;
    private String failureReason;
    private BigDecimal balance;
    private String accountId;
    private String message;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
