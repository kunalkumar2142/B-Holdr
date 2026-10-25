package com.bholdr.shared.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void placeHoldCommand_serializesAndDeserializes() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        String idempotencyKey = "test-key-1";

        PlaceHoldCommand command = new PlaceHoldCommand(transferId, walletId, 1000L, idempotencyKey);

        String json = mapper.writeValueAsString(command);
        PlaceHoldCommand deserialized = mapper.readValue(json, PlaceHoldCommand.class);

        assertEquals(transferId, deserialized.getTransferId());
        assertEquals(walletId, deserialized.getWalletId());
        assertEquals(1000L, deserialized.getAmountMinor());
        assertEquals(idempotencyKey, deserialized.getIdempotencyKey());
    }

    @Test
    void placeHoldReply_serializesAndDeserializes() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();

        PlaceHoldReply reply = PlaceHoldReply.success(transferId, walletId, holdId);

        String json = mapper.writeValueAsString(reply);
        PlaceHoldReply deserialized = mapper.readValue(json, PlaceHoldReply.class);

        assertTrue(deserialized.isSuccess());
        assertEquals(transferId, deserialized.getTransferId());
        assertEquals(walletId, deserialized.getWalletId());
        assertEquals(holdId, deserialized.getHoldId());
        assertNull(deserialized.getReason());
    }

    @Test
    void creditWalletCommand_serializesAndDeserializes() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        String idempotencyKey = "test-key-2";

        CreditWalletCommand command = new CreditWalletCommand(transferId, walletId, 2000L, "INR", idempotencyKey, holdId);

        String json = mapper.writeValueAsString(command);
        CreditWalletCommand deserialized = mapper.readValue(json, CreditWalletCommand.class);

        assertEquals(transferId, deserialized.getTransferId());
        assertEquals(walletId, deserialized.getWalletId());
        assertEquals(2000L, deserialized.getAmountMinor());
        assertEquals("INR", deserialized.getCurrency());
        assertEquals(idempotencyKey, deserialized.getIdempotencyKey());
        assertEquals(holdId, deserialized.getHoldId());
    }

    @Test
    void releaseHoldCommand_serializesAndDeserializes() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        String idempotencyKey = "test-key-3";

        ReleaseHoldCommand command = new ReleaseHoldCommand(transferId, walletId, holdId, idempotencyKey);

        String json = mapper.writeValueAsString(command);
        ReleaseHoldCommand deserialized = mapper.readValue(json, ReleaseHoldCommand.class);

        assertEquals(transferId, deserialized.getTransferId());
        assertEquals(walletId, deserialized.getWalletId());
        assertEquals(holdId, deserialized.getHoldId());
        assertEquals(idempotencyKey, deserialized.getIdempotencyKey());
    }

    @Test
    void riskCheckCommand_serializesAndDeserializes() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String idempotencyKey = "test-key-4";

        RiskCheckCommand command = new RiskCheckCommand(transferId, fromWalletId, toWalletId, 5000L, "INR", idempotencyKey);

        String json = mapper.writeValueAsString(command);
        RiskCheckCommand deserialized = mapper.readValue(json, RiskCheckCommand.class);

        assertEquals(transferId, deserialized.getTransferId());
        assertEquals(fromWalletId, deserialized.getFromWalletId());
        assertEquals(toWalletId, deserialized.getToWalletId());
        assertEquals(5000L, deserialized.getAmountMinor());
        assertEquals("INR", deserialized.getCurrency());
        assertEquals(idempotencyKey, deserialized.getIdempotencyKey());
    }
}