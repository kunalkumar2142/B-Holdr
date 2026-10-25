# B-Holdr Implementation Plan - 9 Days

## Day 1: Project Setup & Wallet Service Foundation (Phase 1 - Part 1)
- [ ] Parent POM + shared-events module
- [ ] wallet-service module setup (Spring Boot, JPA, Flyway, PostgreSQL)
- [ ] Database migrations: wallet, ledger_entry, hold, processed_message tables
- [ ] Entities: Wallet, LedgerEntry, Hold, ProcessedMessage
- [ ] Repositories with custom queries

## Day 2: Wallet Service Core Logic (Phase 1 - Part 2)
- [ ] WalletService: createWallet, getBalance, placeHold, releaseHold, captureHold
- [ ] Double-entry ledger posting (DEBIT/CREDIT in single transaction)
- [ ] Optimistic locking with @Version
- [ ] Idempotency handling via processed_message table
- [ ] Unit tests: money arithmetic, ledger balancing, hold logic

## Day 3: Wallet Service API & Integration Tests (Phase 1 - Complete)
- [ ] REST controllers: POST /wallets, GET /wallets/{id}/balance, GET /wallets/{id}/ledger
- [ ] Global exception handling with error codes (INSUFFICIENT_FUNDS, etc.)
- [ ] Integration tests with Testcontainers (PostgreSQL)
- [ ] Test: create wallet, topup, transfer hold/release, ledger reconciliation

## Day 4: Transfer Service Foundation (Phase 2 - Part 1)
- [ ] transfer-service module setup
- [ ] Database migrations: transfer, saga_instance, outbox_event, processed_message
- [ ] Entities: Transfer, SagaInstance, OutboxEvent
- [ ] State machine enum + validation (only allowed transitions)
- [ ] TransferRepository, SagaInstanceRepository

## Day 5: Transfer Service Orchestration (Phase 2 - Part 2 + Phase 3)
- [ ] SagaOrchestrator: startSaga, handleRiskReply, handleHoldReply, handleCreditReply
- [ ] Transactional outbox pattern (OutboxEventPublisher)
- [ ] Idempotent consumers (processed_message table)
- [ ] Kafka config: topics, producers, consumers
- [ ] Synchronous risk-service placeholder

## Day 6: Risk Service + Kafka Integration (Phase 2 - Complete + Phase 3)
- [ ] risk-service module: velocity limits, fraud rules
- [ ] Kafka messaging between transfer-service ↔ wallet-service ↔ risk-service
- [ ] Correlation ID propagation (MDC + headers)
- [ ] Integration tests: full saga success path with Testcontainers (PostgreSQL + Kafka)

## Day 7: Compensations, Timeouts & Recovery (Phase 4)
- [ ] Compensation actions: releaseHold (idempotent)
- [ ] Exponential backoff retry for compensations
- [ ] STUCK state + alert mechanism
- [ ] Scheduled job: deadline scanner + saga recovery on startup
- [ ] Tests: kill transfer-service mid-saga, restart, verify completion/compensation

## Day 8: Reconciliation, Metrics & Concurrency Tests (Phase 5)
- [ ] Reconciliation job: balance_cached vs ledger sum, DEBIT=CREDIT per transfer
- [ ] Micrometer + Prometheus metrics
- [ ] Concurrency test: parallel transfers from one wallet, verify no negative balance
- [ ] Topup/Withdrawal endpoints via bank-gateway-mock

## Day 9: Documentation & Docker Compose (Phase 6)
- [ ] bank-gateway-mock service (success/failure/delayed/duplicate callbacks)
- [ ] notification-service (idempotent event consumer)
- [ ] README with architecture diagram (Mermaid)
- [ ] Sequence diagrams: success saga, failure saga with compensation
- [ ] docker-compose.yml for full stack
- [ ] Final integration test run