package com.bholdr.transfer.repository;

import com.bholdr.transfer.entity.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {
    Optional<SagaInstance> findByTransferId(UUID transferId);

    @Query("SELECT s FROM SagaInstance s WHERE s.status = :status AND s.deadlineAt < :now")
    java.util.List<SagaInstance> findExpiredSagas(@Param("status") SagaInstance.Status status, @Param("now") Instant now);

    @Query("SELECT s FROM SagaInstance s WHERE s.currentStep = :step")
    java.util.List<SagaInstance> findByCurrentStep(@Param("step") String step);
}