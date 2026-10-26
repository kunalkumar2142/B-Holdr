package com.bholdr.transfer.service;

import com.bholdr.shared.events.*;
import com.bholdr.transfer.entity.Transfer;
import com.bholdr.transfer.entity.SagaInstance;
import com.bholdr.transfer.repository.TransferRepository;
import com.bholdr.transfer.repository.SagaInstanceRepository;
import com.bholdr.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public String startSaga(StartTransferCommand command) {
        String idempotencyKey = command.getIdempotencyKey();

        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate transfer request, returning original transfer ID: {}", existing.get().getId());
            return existing.get().getId().toString();
        }

        UUID transferId = UUID.randomUUID();
        UUID fromWalletId = command.getFromWalletId();
        UUID toWalletId = command.getToWalletId();
        Long amount = command.getAmountMinor();

        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setFromWallet(fromWalletId);
        transfer.setToWallet(toWalletId);
        transfer.setAmountMinor(amount);
        transfer.setCurrency(command.getCurrency());
        transfer.setStatus(Transfer.Status.PENDING);

        transferRepository.save(transfer);

        SagaInstance saga = new SagaInstance();
        saga.setId(UUID.randomUUID());
        saga.setTransferId(transferId);
        saga.setCurrentStep("PENDING");
        saga.setStatus(SagaInstance.Status.RUNNING);
        saga.setDeadlineAt(Instant.now().plus(Duration.ofHours(24)));
        sagaInstanceRepository.save(saga);

        log.info("Transfer saga started: transferId={}, from={}, to={}, amountMinor={}", transferId, fromWalletId, toWalletId, amount);

        sendRiskCheck(transferId, fromWalletId, toWalletId, amount, command.getCurrency(), idempotencyKey);

        return transferId.toString();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(millis = 100))
    @Transactional
    public void handleRiskReply(RiskCheckReply reply) {
        String transferIdStr = reply.getTransferId();
        UUID transferId = UUID.fromString(transferIdStr);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        SagaInstance saga = sagaInstanceRepository.findByTransferId(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + transferId));

        if (saga.getCurrentStep() != null && saga.getCurrentStep().equals("RISK_APPROVED")) {
            log.info("Risk already approved for transfer {}, skipping", transferId);
            return;
        }

        if (reply.isApproved()) {
            log.info("Risk approved for transfer {}", transferId);
            transfer.setStatus(Transfer.Status.RISK_APPROVED);
            transferRepository.save(transfer);

            saga.setCurrentStep("RISK_APPROVED");
            saga.setStatus(SagaInstance.Status.RUNNING);
            sagaInstanceRepository.save(saga);

            placeHold(transferId);
        } else {
            log.info("Risk rejected for transfer {}, starting compensation", transferId);
            transfer.setStatus(Transfer.Status.FAILED);
            transfer.setFailureReason(reply.getReason());
            transferRepository.save(transfer);

            saga.setCurrentStep("COMPENSATING");
            saga.setStatus(SagaInstance.Status.COMPENSATING);
            sagaInstanceRepository.save(saga);

            compensateFail(transferId);
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(millis = 100))
    @Transactional
    public void handleHoldReply(PlaceHoldReply reply) {
        String transferIdStr = reply.getTransferId();
        UUID transferId = UUID.fromString(transferIdStr);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        SagaInstance saga = sagaInstanceRepository.findByTransferId(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + transferId));

        if (reply.isSuccess()) {
            log.info("Hold placed for transfer {}", transferId);
            transfer.setStatus(Transfer.Status.FUNDS_HELD);
            transferRepository.save(transfer);

            saga.setCurrentStep("FUNDS_HELD");
            saga.setStatus(SagaInstance.Status.RUNNING);
            sagaInstanceRepository.save(saga);

            riskApprove(transferId);
        } else {
            log.info("Hold failed for transfer {}, starting compensation", transferId);
            transfer.setStatus(Transfer.Status.FAILED);
            transfer.setFailureReason(reply.getReason());
            transferRepository.save(transfer);

            saga.setCurrentStep("COMPENSATING");
            saga.setStatus(SagaInstance.Status.COMPENSATING);
            sagaInstanceRepository.save(saga);

            compensateFail(transferId);
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(millis = 100))
    @Transactional
    public void handleCreditReply(CreditWalletReply reply) {
        String transferIdStr = reply.getTransferId();
        UUID transferId = UUID.fromString(transferIdStr);

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        SagaInstance saga = sagaInstanceRepository.findByTransferId(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + transferId));

        if (reply.isSuccess()) {
            log.info("Credit completed for transfer {}", transferId);
            transfer.setStatus(Transfer.Status.CREDITED);
            transferRepository.save(transfer);

            saga.setCurrentStep("CREDITED");
            saga.setStatus(SagaInstance.Status.RUNNING);
            sagaInstanceRepository.save(saga);

            completeTransfer(transferId);
        } else {
            log.info("Credit failed for transfer {}, starting compensation", transferId);
            transfer.setStatus(Transfer.Status.FAILED);
            transfer.setFailureReason(reply.getReason());
            transferRepository.save(transfer);

            saga.setCurrentStep("COMPENSATING");
            saga.setStatus(SagaInstance.Status.COMPENSATING);
            sagaInstanceRepository.save(saga);

            compensateFail(transferId);
        }
    }

    private void placeHold(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow();

        UUID fromWalletId = transfer.getFromWallet();
        Long amount = transfer.getAmountMinor();

        PlaceHoldCommand command = new PlaceHoldCommand(transferId, fromWalletId, amount, "hold-" + transferId);
        kafkaTemplate.send("place-hold-topic", idempotencyKey -> idempotencyKey, command);
    }

    private void riskApprove(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow();

        UUID fromWalletId = transfer.getFromWallet();
        UUID toWalletId = transfer.getToWallet();

        RiskCheckCommand command = new RiskCheckCommand(transferId, fromWalletId, toWalletId, transfer.getAmountMinor(), transfer.getCurrency(), "risk-" + transferId);
        kafkaTemplate.send("risk-check-topic", idempotencyKey -> idempotencyKey, command);
    }

    private void completeTransfer(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow();

        transfer.setStatus(Transfer.Status.COMPLETED);
        transferRepository.save(transfer);

        sagaInstanceRepository.findByTransferId(transferId)
                .ifPresent(saga -> {
                    saga.setCurrentStep("COMPLETED");
                    saga.setStatus(SagaInstance.Status.COMPLETED);
                    sagaInstanceRepository.save(saga);
                });

        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(transferId);
        event.setType("TransferCompleted");
        event.setPayload("{\"transferId\":\"" + transferId + "\", \"status\":\"COMPLETED\"}");
        transfer.getOutboxEvents().add(event);
        // Note: in real implementation, would use entity manager to persist

        log.info("Transfer completed: {}", transferId);
    }

    private void compensateFail(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow();

        ReleaseHoldCommand releaseCmd = new ReleaseHoldCommand(transferId, transfer.getFromWallet(), null, "comp-" + transferId);
        kafkaTemplate.send("release-hold-topic", idempotencyKey -> idempotencyKey, releaseCmd);

        log.info("Compensation initiated for transfer {}", transferId);
    }
}