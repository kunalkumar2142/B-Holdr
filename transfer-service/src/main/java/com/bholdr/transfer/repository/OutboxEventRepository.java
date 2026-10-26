package com.bholdr.transfer.repository;

import com.bholdr.transfer.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.published_at IS NULL")
    java.util.List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT o FROM OutboxEvent o WHERE o.aggregate_id = :id AND o.published_at IS NULL")
    Optional<OutboxEvent> findByAggregateId(@Param("id") UUID id);

    @Query("UPDATE OutboxEvent o SET o.published_at = :now WHERE o.id = :id")
    void markPublished(@Param("id") UUID id, @Param("now") Instant now);
}