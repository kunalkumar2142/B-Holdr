CREATE TABLE wallet (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance_cached BIGINT NOT NULL DEFAULT 0 CHECK (balance_cached >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_user_id ON wallet(user_id);

CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallet(id),
    transfer_id UUID NOT NULL,
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_wallet_id ON ledger_entry(wallet_id);
CREATE INDEX idx_ledger_transfer_id ON ledger_entry(transfer_id);

CREATE TABLE hold (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallet(id),
    transfer_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    status VARCHAR(10) NOT NULL CHECK (status IN ('HELD', 'RELEASED', 'CAPTURED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (transfer_id, wallet_id)
);

CREATE INDEX idx_hold_wallet_id ON hold(wallet_id);
CREATE INDEX idx_hold_transfer_id ON hold(transfer_id);

CREATE TABLE processed_message (
    message_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);