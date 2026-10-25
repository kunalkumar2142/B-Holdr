package com.bholdr.wallet.repository;

import com.bholdr.wallet.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface HoldRepository extends JpaRepository<Hold, UUID> {
    Optional<Hold> findByTransferIdAndWalletId(UUID transferId, UUID walletId);

    Optional<Hold> findByTransferId(UUID transferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hold h WHERE h.id = :id")
    Optional<Hold> findByIdForUpdate(@Param("id") UUID id);
}