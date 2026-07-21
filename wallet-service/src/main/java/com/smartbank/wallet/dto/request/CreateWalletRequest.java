package com.smartbank.wallet.dto.request;

import com.smartbank.wallet.entity.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Create a wallet linked to an account (PRD §6.7 POST /wallets).
 * The owning {@code customerId} is taken from the trusted gateway header, not the body.
 */
@Data
public class CreateWalletRequest {

    @NotBlank(message = "linkedAccountId is required")
    private String linkedAccountId;

    @NotNull(message = "walletType is required")
    private WalletType walletType;
}
