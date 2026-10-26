package com.bholdr.transfer.repository;

import com.bholdr.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transfer t WHERE t.fromWallet = :from AND t.toWallet = :to")
    Optional<Transfer> findByWallets(@Param("from") UUID from, @Param("to") UUID to);

    @Query("SELECT t FROM Transfer t WHERE t.status IN :statuses")
    java.util.List<Transfer> findByStatusIn(@Param("statuses") java.util.List<Status> statuses);
}