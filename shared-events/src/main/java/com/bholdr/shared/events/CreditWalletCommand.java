package com.bholdr.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditWalletCommand implements Serializable {
    @NotNull
    @JsonProperty("transfer_id")
    private UUID transferId;

    @NotNull
    @JsonProperty("wallet_id")
    private UUID walletId;

    @Positive
    @JsonProperty("amount_minor")
    private Long amountMinor;

    @NotNull
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("hold_id")
    private UUID holdId;
}