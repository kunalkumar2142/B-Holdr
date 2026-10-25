package com.bholdr.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceHoldReply implements Serializable {
    @JsonProperty("transfer_id")
    private UUID transferId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    @JsonProperty("hold_id")
    private UUID holdId;

    private boolean success;

    private String reason;

    public static PlaceHoldReply success(UUID transferId, UUID walletId, UUID holdId) {
        return new PlaceHoldReply(transferId, walletId, holdId, true, null);
    }

    public static PlaceHoldReply failure(UUID transferId, UUID walletId, String reason) {
        return new PlaceHoldReply(transferId, walletId, null, false, reason);
    }
}