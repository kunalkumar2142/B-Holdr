package com.bholdr.wallet.repository;

import com.bholdr.wallet.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByWalletId(UUID walletId);

    List<LedgerEntry> findByTransferId(UUID transferId);

    @Query("SELECT SUM(CASE WHEN le.direction = 'CREDIT' THEN le.amountMinor ELSE -le.amountMinor END) FROM LedgerEntry le WHERE le.walletId = :walletId")
    Long calculateBalanceFromLedger(@Param("walletId") UUID walletId);

    @Query("SELECT SUM(CASE WHEN le.direction = 'DEBIT' THEN le.amountMinor ELSE 0 END) FROM LedgerEntry le WHERE le.transferId = :transferId")
    Long sumDebitsByTransfer(@Param("transferId") UUID transferId);

    @Query("SELECT SUM(CASE WHEN le.direction = 'CREDIT' THEN le.amountMinor ELSE 0 END) FROM LedgerEntry le WHERE le.transferId = :transferId")
    Long sumCreditsByTransfer(@Param("transferId") UUID transferId);
}