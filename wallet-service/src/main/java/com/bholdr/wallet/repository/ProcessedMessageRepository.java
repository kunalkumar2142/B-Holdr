package com.bholdr.wallet.repository;

import com.bholdr.wallet.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
    Optional<ProcessedMessage> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);
}