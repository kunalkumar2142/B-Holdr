package com.bholdr.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "INR";

    @Column(name = "balance_cached", nullable = false)
    private Long balanceCached = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Wallet(UUID id, UUID userId, String currency) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balanceCached = 0L;
    }
}