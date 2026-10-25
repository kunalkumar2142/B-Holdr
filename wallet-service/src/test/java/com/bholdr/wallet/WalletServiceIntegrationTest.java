package com.bholdr.wallet;

import com.bholdr.wallet.entity.Hold;
import com.bholdr.wallet.entity.LedgerEntry;
import com.bholdr.wallet.entity.Wallet;
import com.bholdr.wallet.repository.HoldRepository;
import com.bholdr.wallet.repository.LedgerEntryRepository;
import com.bholdr.wallet.repository.ProcessedMessageRepository;
import com.bholdr.wallet.repository.WalletRepository;
import com.bholdr.wallet.service.WalletService;
import com.bholdr.wallet.exception.InsufficientFundsException;
import com.bholdr.wallet.exception.WalletNotFoundException;
import com.bholdr.shared.events.CreditWalletCommand;
import com.bholdr.shared.events.PlaceHoldCommand;
import com.bholdr.shared.events.ReleaseHoldCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class WalletServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WalletService walletService;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    HoldRepository holdRepository;

    @Autowired
    ProcessedMessageRepository processedMessageRepository;

    @Test
    void createWallet_andGetBalance() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");

        assertNotNull(wallet.getId());
        assertEquals(userId, wallet.getUserId());
        assertEquals("INR", wallet.getCurrency());
        assertEquals(0L, wallet.getBalanceCached());

        Long balance = walletService.getBalance(wallet.getId());
        assertEquals(0L, balance);
    }

    @Test
    void placeHold_andReleaseHold() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");

        Long balanceBefore = walletService.getBalance(walletId);
        assertEquals(10000L, balanceBefore);

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId, walletId, 3000L, "idem-key-1");
        var holdReply = walletService.placeHold(holdCommand);

        assertTrue(holdReply.isSuccess());
        assertNotNull(holdReply.getHoldId());

        Long balanceAfterHold = walletService.getBalance(walletId);
        assertEquals(7000L, balanceAfterHold);

        Hold hold = holdRepository.findById(holdReply.getHoldId()).orElseThrow();
        assertEquals(Hold.Status.HELD, hold.getStatus());

        ReleaseHoldCommand releaseCommand = new ReleaseHoldCommand(transferId, walletId, holdReply.getHoldId(), "idem-key-2");
        var releaseReply = walletService.releaseHold(releaseCommand);

        assertTrue(releaseReply.isSuccess());

        Long balanceAfterRelease = walletService.getBalance(walletId);
        assertEquals(10000L, balanceAfterRelease);

        Hold releasedHold = holdRepository.findById(holdReply.getHoldId()).orElseThrow();
        assertEquals(Hold.Status.RELEASED, releasedHold.getStatus());
    }

    @Test
    void placeHold_insufficientFunds_throwsException() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId, walletId, 5000L, "idem-key-3");

        assertThrows(InsufficientFundsException.class, () -> walletService.placeHold(holdCommand));
    }

    @Test
    void placeHold_idempotent() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId, walletId, 3000L, "idem-key-4");
        var reply1 = walletService.placeHold(holdCommand);
        var reply2 = walletService.placeHold(holdCommand);

        assertEquals(reply1.getHoldId(), reply2.getHoldId());
        assertEquals(7000L, walletService.getBalance(walletId));
    }

    @Test
    void creditWallet_capturesHold() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId, walletId, 3000L, "idem-key-5");
        var holdReply = walletService.placeHold(holdCommand);

        CreditWalletCommand creditCommand = new CreditWalletCommand(transferId, walletId, 3000L, "INR", "idem-key-6", holdReply.getHoldId());
        var creditReply = walletService.creditWallet(creditCommand);

        assertTrue(creditReply.isSuccess());
        assertNotNull(creditReply.getLedgerEntryId());

        Long balance = walletService.getBalance(walletId);
        assertEquals(10000L, balance);

        Hold hold = holdRepository.findById(holdReply.getHoldId()).orElseThrow();
        assertEquals(Hold.Status.CAPTURED, hold.getStatus());

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferId(transferId);
        assertEquals(2, entries.size());
        long creditSum = entries.stream().filter(e -> e.getDirection() == LedgerEntry.Direction.CREDIT).mapToLong(LedgerEntry::getAmountMinor).sum();
        long debitSum = entries.stream().filter(e -> e.getDirection() == LedgerEntry.Direction.DEBIT).mapToLong(LedgerEntry::getAmountMinor).sum();
        assertEquals(3000L, creditSum);
        assertEquals(10000L, debitSum);
    }

    @Test
    void releaseHold_idempotent() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId, walletId, 3000L, "idem-key-7");
        var holdReply = walletService.placeHold(holdCommand);

        ReleaseHoldCommand releaseCommand = new ReleaseHoldCommand(transferId, walletId, holdReply.getHoldId(), "idem-key-8");
        walletService.releaseHold(releaseCommand);
        walletService.releaseHold(releaseCommand);

        Long balance = walletService.getBalance(walletId);
        assertEquals(10000L, balance);
    }

    @Test
    void ledgerReconciliation_balanceMatchesSum() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId1 = UUID.randomUUID();
        UUID transferId2 = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId1, 10000L, "INR");
        walletService.postDebitEntry(walletId, transferId2, 5000L, "INR");

        PlaceHoldCommand holdCommand = new PlaceHoldCommand(transferId1, walletId, 3000L, "idem-key-9");
        var holdReply = walletService.placeHold(holdCommand);

        CreditWalletCommand creditCommand = new CreditWalletCommand(transferId1, walletId, 3000L, "INR", "idem-key-10", holdReply.getHoldId());
        walletService.creditWallet(creditCommand);

        Long cachedBalance = walletService.getBalance(walletId);
        Long ledgerBalance = ledgerEntryRepository.calculateBalanceFromLedger(walletId);

        assertEquals(cachedBalance, ledgerBalance);
    }
}