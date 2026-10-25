package com.bholdr.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseHoldCommand implements Serializable {
    @NotNull
    @JsonProperty("transfer_id")
    private UUID transferId;

    @NotNull
    @JsonProperty("wallet_id")
    private UUID walletId;

    @NotNull
    @JsonProperty("hold_id")
    private UUID holdId;

    @NotNull
    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}