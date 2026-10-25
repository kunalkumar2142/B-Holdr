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
public class RiskCheckCommand implements Serializable {
    @NotNull
    @JsonProperty("transfer_id")
    private UUID transferId;

    @NotNull
    @JsonProperty("from_wallet_id")
    private UUID fromWalletId;

    @NotNull
    @JsonProperty("to_wallet_id")
    private UUID toWalletId;

    @Positive
    @JsonProperty("amount_minor")
    private Long amountMinor;

    @NotNull
    @JsonProperty("currency")
    private String currency;

    @NotNull
    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}