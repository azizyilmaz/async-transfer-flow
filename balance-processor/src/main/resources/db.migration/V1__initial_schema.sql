-- ============================================================
-- V1__initial_schema.sql
-- Transfer Service + Balance Processor ortak şema
-- ============================================================

-- ── Transfer kayıtları ────────────────────────────────────────────────────
CREATE TABLE transfers (
    id                  UUID        PRIMARY KEY,
    firm_account_id     VARCHAR(64) NOT NULL,
    customer_account_id VARCHAR(64) NOT NULL,
    amount              NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    status              VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    correlation_id      VARCHAR(128) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,

    CONSTRAINT uq_transfers_correlation UNIQUE (correlation_id),
    CONSTRAINT chk_transfers_status CHECK (
        status IN ('INITIATED','PROCESSING','COMPLETED','FAILED')
    )
);

CREATE INDEX index_transfers_firm_created ON transfers (firm_account_id, created_at DESC);
CREATE INDEX index_transfers_status       ON transfers (status) WHERE status != 'COMPLETED';

-- ── Outbox tablosu ────────────────────────────────────────────────────────
CREATE TABLE outbox_events (
    id             UUID        PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(128) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMPTZ,
    retry_count    INT         NOT NULL DEFAULT 0,

    CONSTRAINT chk_outbox_status CHECK (
        status IN ('PENDING','PUBLISHED','FAILED')
    )
);

-- Relay scheduler bu index'i kullanır (PENDING eventleri hızlı bul)
CREATE INDEX index_outbox_status_created ON outbox_events (status, created_at)
    WHERE status = 'PENDING';

-- ── Hesap bakiyeleri ──────────────────────────────────────────────────────
CREATE TABLE accounts (
    account_id  VARCHAR(64)   PRIMARY KEY,
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version     BIGINT        NOT NULL DEFAULT 0   -- optimistic lock
);

-- ── İşlenmiş event kaydı (idempotency) ───────────────────────────────────
CREATE TABLE processed_events (
    transfer_id  UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Örnek firma hesabı ────────────────────────────────────────────────────
INSERT INTO accounts (account_id, balance)
VALUES ('FIRM-001', 1000000.0000);
