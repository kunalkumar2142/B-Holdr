package com.bholdr.wallet.controller;

import com.bholdr.wallet.entity.LedgerEntry;
import com.bholdr.wallet.entity.Wallet;
import com.bholdr.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request.userId(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateWalletResponse(wallet.getId(), wallet.getUserId(), wallet.getCurrency(), wallet.getBalanceCached()));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable @NotNull UUID id) {
        Long balance = walletService.getBalance(id);
        return ResponseEntity.ok(new BalanceResponse(id, balance));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable @NotNull UUID id) {
        List<LedgerEntry> entries = walletService.getLedger(id);
        List<LedgerEntryResponse> response = entries.stream()
                .map(e -> new LedgerEntryResponse(e.getId(), e.getWalletId(), e.getTransferId(),
                        e.getDirection().name(), e.getAmountMinor(), e.getCurrency(), e.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record CreateWalletRequest(UUID userId, String currency) {}
    public record CreateWalletResponse(UUID id, UUID userId, String currency, Long balanceCached) {}
    public record BalanceResponse(UUID walletId, Long balance) {}
    public record LedgerEntryResponse(UUID id, UUID walletId, UUID transferId, String direction, Long amountMinor, String currency, java.time.Instant createdAt) {}
}