package com.smartbank.wallet.dto.request;

import com.smartbank.wallet.entity.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// What the caller sends to open a new wallet. The owner comes from the trusted login header, not this body.
@Data
public class CreateWalletRequest {

    @NotBlank(message = "linkedAccountId is required")
    private String linkedAccountId;

    @NotNull(message = "walletType is required")
    private WalletType walletType;
}
