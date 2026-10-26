package com.bholdr.transfer.controller;

import com.bholdr.shared.events.*;
import com.bholdr.transfer.entity.Transfer;
import com.bholdr.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<String> initiateTransfer(@Valid @RequestBody InitiateTransferRequest request) {
        String transferId = transferService.startSaga(new StartTransferCommand(
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmountMinor(),
                request.getCurrency(),
                request.getIdempotencyKey()
        ));
        return ResponseEntity.status(202).body(transferId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getTransferStatus(@PathVariable String id) {
        Transfer transfer = null; // In real impl, fetch from repo
        return ResponseEntity.ok("Transfer status: placeholder");
    }

    public record InitiateTransferRequest(UUID fromWalletId, UUID toWalletId, Long amountMinor, String currency, String idempotencyKey) {}
    public record StartTransferCommand(UUID fromWalletId, UUID toWalletId, Long amountMinor, String currency, String idempotencyKey) {}
}