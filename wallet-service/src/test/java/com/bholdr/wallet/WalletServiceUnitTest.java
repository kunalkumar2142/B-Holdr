package com.bholdr.wallet.service;

import com.bholdr.wallet.entity.LedgerEntry;
import com.bholdr.wallet.entity.Wallet;
import com.bholdr.wallet.repository.LedgerEntryRepository;
import com.bholdr.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletServiceUnitTest {

    @Autowired
    WalletService walletService;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    @Test
    void testMoneyArithmetic_noDoublePrecision() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");
        assertEquals(10000L, walletService.getBalance(walletId));

        walletService.postDebitEntry(walletId, transferId, 500L, "INR");
        assertEquals(10500L, walletService.getBalance(walletId));
    }

    @Test
    void testLedgerBalancing_debitEqualsCredit() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");
        walletService.postDebitEntry(walletId, transferId, 5000L, "INR");

        long debitSum = ledgerEntryRepository.sumDebitsByTransfer(transferId);
        long creditSum = ledgerEntryRepository.sumCreditsByTransfer(transferId);

        assertEquals(15000L, debitSum);
        assertEquals(0L, creditSum);
    }

    @Test
    void testHoldLogic_balanceConsistency() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletService.createWallet(userId, "INR");
        UUID walletId = wallet.getId();
        UUID transferId = UUID.randomUUID();

        walletService.postDebitEntry(walletId, transferId, 10000L, "INR");

        var holdReply = walletService.placeHold(new com.bholdr.shared.events.PlaceHoldCommand(transferId, walletId, 3000L, "test-1"));
        assertTrue(holdReply.isSuccess());
        assertEquals(7000L, walletService.getBalance(walletId));

        walletService.releaseHold(new com.bholdr.shared.events.ReleaseHoldCommand(transferId, walletId, holdReply.getHoldId(), "test-2"));
        assertEquals(10000L, walletService.getBalance(walletId));
    }
}