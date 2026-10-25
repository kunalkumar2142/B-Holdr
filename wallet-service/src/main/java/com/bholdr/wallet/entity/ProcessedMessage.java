package com.bholdr.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {
    @Id
    @Column(name = "message_id", length = 255, nullable = false)
    private String messageId;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedMessage(String messageId) {
        this.messageId = messageId;
    }
}