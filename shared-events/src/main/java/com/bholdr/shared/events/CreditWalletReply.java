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
public class CreditWalletReply implements Serializable {
    @JsonProperty("transfer_id")
    private UUID transferId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    @JsonProperty("ledger_entry_id")
    private UUID ledgerEntryId;

    private boolean success;

    private String reason;

    public static CreditWalletReply success(UUID transferId, UUID walletId, UUID ledgerEntryId) {
        return new CreditWalletReply(transferId, walletId, ledgerEntryId, true, null);
    }

    public static CreditWalletReply failure(UUID transferId, UUID walletId, String reason) {
        return new CreditWalletReply(transferId, walletId, null, false, reason);
    }
}