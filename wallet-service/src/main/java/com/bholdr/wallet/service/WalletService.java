package com.bholdr.wallet.service;

import com.bholdr.shared.events.CreditWalletCommand;
import com.bholdr.shared.events.CreditWalletReply;
import com.bholdr.shared.events.PlaceHoldCommand;
import com.bholdr.shared.events.PlaceHoldReply;
import com.bholdr.shared.events.ReleaseHoldCommand;
import com.bholdr.shared.events.ReleaseHoldReply;
import com.bholdr.wallet.entity.Hold;
import com.bholdr.wallet.entity.LedgerEntry;
import com.bholdr.wallet.entity.Wallet;
import com.bholdr.wallet.exception.InsufficientFundsException;
import com.bholdr.wallet.exception.WalletNotFoundException;
import com.bholdr.wallet.repository.HoldRepository;
import com.bholdr.wallet.repository.LedgerEntryRepository;
import com.bholdr.wallet.repository.ProcessedMessageRepository;
import com.bholdr.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final HoldRepository holdRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public Wallet createWallet(UUID userId, String currency) {
        Wallet wallet = new Wallet(UUID.randomUUID(), userId, currency);
        return walletRepository.save(wallet);
    }

    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }

    public Long getBalance(UUID walletId) {
        Wallet wallet = getWallet(walletId);
        return wallet.getBalanceCached();
    }

    public List<LedgerEntry> getLedger(UUID walletId) {
        return ledgerEntryRepository.findByWalletId(walletId);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    @Transactional
    public PlaceHoldReply placeHold(PlaceHoldCommand command) {
        String idempotencyKey = command.getIdempotencyKey();
        if (processedMessageRepository.existsByMessageId(idempotencyKey)) {
            log.info("Duplicate place hold request, idempotencyKey: {}", idempotencyKey);
            Optional<Hold> existingHold = holdRepository.findByTransferIdAndWalletId(command.getTransferId(), command.getWalletId());
            if (existingHold.isPresent()) {
                return PlaceHoldReply.success(command.getTransferId(), command.getWalletId(), existingHold.get().getId());
            }
        }

        Wallet wallet = walletRepository.findByIdForUpdate(command.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + command.getWalletId()));

        Long availableBalance = wallet.getBalanceCached();
        Long holdAmount = command.getAmountMinor();

        if (availableBalance < holdAmount) {
            throw new InsufficientFundsException("Insufficient funds. Available: " + availableBalance + ", Required: " + holdAmount);
        }

        wallet.setBalanceCached(availableBalance - holdAmount);
        walletRepository.save(wallet);

        Hold hold = new Hold();
        hold.setId(UUID.randomUUID());
        hold.setWalletId(command.getWalletId());
        hold.setTransferId(command.getTransferId());
        hold.setAmountMinor(holdAmount);
        hold.setStatus(Hold.Status.HELD);
        holdRepository.save(hold);

        processedMessageRepository.save(new com.bholdr.wallet.entity.ProcessedMessage(idempotencyKey));

        log.info("Hold placed: holdId={}, walletId={}, amountMinor={}", hold.getId(), command.getWalletId(), holdAmount);
        return PlaceHoldReply.success(command.getTransferId(), command.getWalletId(), hold.getId());
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    @Transactional
    public ReleaseHoldReply releaseHold(ReleaseHoldCommand command) {
        String idempotencyKey = command.getIdempotencyKey();
        if (processedMessageRepository.existsByMessageId(idempotencyKey)) {
            log.info("Duplicate release hold request, idempotencyKey: {}", idempotencyKey);
            return ReleaseHoldReply.success(command.getTransferId(), command.getWalletId());
        }

        Hold hold = holdRepository.findByIdForUpdate(command.getHoldId())
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + command.getHoldId()));

        if (hold.getStatus() == Hold.Status.RELEASED) {
            log.info("Hold already released: holdId={}", hold.getId());
            processedMessageRepository.save(new com.bholdr.wallet.entity.ProcessedMessage(idempotencyKey));
            return ReleaseHoldReply.success(command.getTransferId(), command.getWalletId());
        }

        if (hold.getStatus() == Hold.Status.CAPTURED) {
            throw new IllegalStateException("Cannot release captured hold: " + hold.getId());
        }

        Wallet wallet = walletRepository.findByIdForUpdate(hold.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + hold.getWalletId()));

        wallet.setBalanceCached(wallet.getBalanceCached() + hold.getAmountMinor());
        walletRepository.save(wallet);

        hold.setStatus(Hold.Status.RELEASED);
        holdRepository.save(hold);

        processedMessageRepository.save(new com.bholdr.wallet.entity.ProcessedMessage(idempotencyKey));

        log.info("Hold released: holdId={}, walletId={}, amountMinor={}", hold.getId(), hold.getWalletId(), hold.getAmountMinor());
        return ReleaseHoldReply.success(command.getTransferId(), command.getWalletId());
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    @Transactional
    public CreditWalletReply creditWallet(CreditWalletCommand command) {
        String idempotencyKey = command.getIdempotencyKey();
        if (processedMessageRepository.existsByMessageId(idempotencyKey)) {
            log.info("Duplicate credit wallet request, idempotencyKey: {}", idempotencyKey);
            Optional<LedgerEntry> existingEntry = ledgerEntryRepository.findByTransferId(command.getTransferId())
                    .stream()
                    .filter(le -> le.getWalletId().equals(command.getWalletId()) && le.getDirection() == LedgerEntry.Direction.CREDIT)
                    .findFirst();
            if (existingEntry.isPresent()) {
                return CreditWalletReply.success(command.getTransferId(), command.getWalletId(), existingEntry.get().getId());
            }
        }

        UUID holdId = command.getHoldId();
        Hold hold = null;
        if (holdId != null) {
            hold = holdRepository.findByIdForUpdate(holdId)
                    .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));
            if (hold.getStatus() != Hold.Status.HELD) {
                throw new IllegalStateException("Hold not in HELD status: " + holdId);
            }
        }

        Wallet wallet = walletRepository.findByIdForUpdate(command.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + command.getWalletId()));

        wallet.setBalanceCached(wallet.getBalanceCached() + command.getAmountMinor());
        walletRepository.save(wallet);

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setId(UUID.randomUUID());
        creditEntry.setWalletId(command.getWalletId());
        creditEntry.setTransferId(command.getTransferId());
        creditEntry.setDirection(LedgerEntry.Direction.CREDIT);
        creditEntry.setAmountMinor(command.getAmountMinor());
        creditEntry.setCurrency(command.getCurrency());
        ledgerEntryRepository.save(creditEntry);

        if (hold != null) {
            hold.setStatus(Hold.Status.CAPTURED);
            holdRepository.save(hold);
        }

        processedMessageRepository.save(new com.bholdr.wallet.entity.ProcessedMessage(idempotencyKey));

        log.info("Wallet credited: walletId={}, transferId={}, amountMinor={}, ledgerEntryId={}",
                command.getWalletId(), command.getTransferId(), command.getAmountMinor(), creditEntry.getId());
        return CreditWalletReply.success(command.getTransferId(), command.getWalletId(), creditEntry.getId());
    }

    @Transactional
    public void postDebitEntry(UUID walletId, UUID transferId, Long amountMinor, String currency) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        wallet.setBalanceCached(wallet.getBalanceCached() - amountMinor);
        walletRepository.save(wallet);

        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setId(UUID.randomUUID());
        debitEntry.setWalletId(walletId);
        debitEntry.setTransferId(transferId);
        debitEntry.setDirection(LedgerEntry.Direction.DEBIT);
        debitEntry.setAmountMinor(amountMinor);
        debitEntry.setCurrency(currency);
        ledgerEntryRepository.save(debitEntry);

        log.info("Debit entry posted: walletId={}, transferId={}, amountMinor={}", walletId, transferId, amountMinor);
    }
}