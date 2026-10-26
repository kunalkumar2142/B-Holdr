CREATE TABLE transfer (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    from_wallet UUID NOT NULL,
    to_wallet UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'FUNDS_HELD', 'RISK_APPROVED', 'CREDITED', 'COMPLETED', 'COMPENSATING', 'FAILED', 'STUCK')),
    failure_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transfer_from_wallet ON transfer(from_wallet);
CREATE INDEX idx_transfer_to_wallet ON transfer(to_wallet);
CREATE INDEX idx_transfer_status ON transfer(status);

CREATE TABLE saga_instance (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES transfer(id),
    current_step VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPENSATING', 'COMPLETED', 'FAILED', 'STUCK')),
    deadline_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_transfer_id ON saga_instance(transfer_id);
CREATE INDEX idx_saga_status_deadline ON saga_instance(status, deadline_at);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_published ON outbox_event(published_at) WHERE published_at IS NULL;

CREATE TABLE processed_message (
    message_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);