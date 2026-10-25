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
public class ReleaseHoldReply implements Serializable {
    @JsonProperty("transfer_id")
    private UUID transferId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    private boolean success;

    private String reason;

    public static ReleaseHoldReply success(UUID transferId, UUID walletId) {
        return new ReleaseHoldReply(transferId, walletId, true, null);
    }

    public static ReleaseHoldReply failure(UUID transferId, UUID walletId, String reason) {
        return new ReleaseHoldReply(transferId, walletId, false, reason);
    }
}