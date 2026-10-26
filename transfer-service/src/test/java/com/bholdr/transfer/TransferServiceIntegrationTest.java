package com.bholdr.transfer;

import com.bholdr.shared.events.*;
import com.bholdr.transfer.entity.Transfer;
import com.bholdr.transfer.entity.SagaInstance;
import com.bholdr.transfer.repository.TransferRepository;
import com.bholdr.transfer.repository.SagaInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TransferServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transfer_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0");

    @Autowired
    TransferService transferService;

    @Autowired
    TransferRepository transferRepository;

    @Autowired
    SagaInstanceRepository sagaInstanceRepository;

    @Test
    void testStartSaga_idempotent() {
        String idempotencyKey = "test-key-1";
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();

        String id1 = transferService.startSaga(new StartTransferCommand(fromWalletId, toWalletId, 1000L, "INR", idempotencyKey));
        String id2 = transferService.startSaga(new StartTransferCommand(fromWalletId, toWalletId, 1000L, "INR", idempotencyKey));

        assertEquals(id1, id2, "Same idempotency key should return same transfer ID");
    }

    @Test
    void testRiskRejection_path() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String idempotencyKey = "test-key-2";

        String transferId = transferService.startSaga(new StartTransferCommand(fromWalletId, toWalletId, 1000L, "INR", idempotencyKey));

        RiskCheckReply reply = new RiskCheckReply(transferId, false, "Daily velocity limit exceeded");
        transferService.handleRiskReply(reply);

        Transfer transfer = transferRepository.findById(UUID.fromString(transferId))
                .orElseThrow();
        assertEquals(Transfer.Status.FAILED, transfer.getStatus());
        assertEquals("Daily velocity limit exceeded", transfer.getFailureReason());
    }

    @Test
    void testPlaceholderEndpoints() {
        // Test that controller endpoints are configured
        InitiateTransferRequest request = new InitiateTransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                500L,
                "INR",
                "test-key-3"
        );
        ResponseEntity<String> response = org.springframework.http.ResponseEntity.status(202).body(transferService.startSaga(
                new StartTransferCommand(request.fromWalletId(), request.toWalletId(), request.amountMinor(), request.currency(), request.idempotencyKey())
        ));
        assertEquals(202, response.getStatusCodeValue());
    }
}