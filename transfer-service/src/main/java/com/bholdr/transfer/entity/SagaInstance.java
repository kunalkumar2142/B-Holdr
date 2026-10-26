package com.bholdr.transfer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transfer_id", nullable = false, unique = true)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 30, nullable = false)
    private String currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status {
        RUNNING, COMPENSATING, COMPLETED, FAILED, STUCK
    }
}