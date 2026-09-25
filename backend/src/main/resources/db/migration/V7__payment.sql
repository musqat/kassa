CREATE TABLE payment (
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT       NOT NULL REFERENCES orders (id),
    transaction_id         VARCHAR(100),
    amount                 BIGINT       NOT NULL,
    method                 VARCHAR(30),
    status                 VARCHAR(20)  NOT NULL,
    cancel_idempotency_key VARCHAR(100),
    fail_reason            VARCHAR(255),

    requested_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    approved_at            TIMESTAMPTZ,
    canceled_at            TIMESTAMPTZ,

    CONSTRAINT ck_payment_status CHECK (status IN ('REQUESTED', 'PAID', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
);

CREATE INDEX ix_payment_order ON payment (order_id, requested_at DESC);

-- 한 주문에 승인된 결제는 하나다. 실패 뒤 다른 카드로 다시 결제하는 흐름은 막지 않는다
CREATE UNIQUE INDEX uk_payment_paid_order ON payment (order_id) WHERE status = 'PAID';

CREATE TABLE payment_log (
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT      NOT NULL REFERENCES payment (id),
    event_type  VARCHAR(40) NOT NULL,
    raw_payload TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_payment_log_payment ON payment_log (payment_id, created_at);

CREATE TABLE saga_instance (
    id           BIGSERIAL PRIMARY KEY,
    saga_type    VARCHAR(40) NOT NULL,
    order_no     VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    current_step VARCHAR(40),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_saga_instance_order_no UNIQUE (order_no),
    CONSTRAINT ck_saga_instance_status CHECK (
        status IN ('RUNNING', 'COMPLETED', 'COMPENSATING', 'FAILED', 'NEEDS_ATTENTION')
    )
);

CREATE INDEX ix_saga_instance_status_updated ON saga_instance (status, updated_at);

CREATE TABLE saga_step (
    id               BIGSERIAL PRIMARY KEY,
    saga_instance_id BIGINT      NOT NULL REFERENCES saga_instance (id),
    step_name        VARCHAR(40) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    attempt_count    INT         NOT NULL DEFAULT 0,
    idempotency_key  VARCHAR(100),
    payload          TEXT,
    error            VARCHAR(255),
    executed_at      TIMESTAMPTZ,

    CONSTRAINT ck_saga_step_status CHECK (status IN ('PENDING', 'DONE', 'FAILED', 'COMPENSATED'))
);

CREATE INDEX ix_saga_step_instance ON saga_step (saga_instance_id, id);

CREATE TABLE webhook_inbox (
    id            BIGSERIAL PRIMARY KEY,
    event_id      VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    headers       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    error         VARCHAR(255),
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,

    CONSTRAINT uk_webhook_inbox_event UNIQUE (event_id),
    CONSTRAINT ck_webhook_inbox_status CHECK (status IN ('RECEIVED', 'DONE', 'FAILED'))
);

CREATE INDEX ix_webhook_inbox_status_received ON webhook_inbox (status, received_at);

CREATE TABLE reconcile_diff (
    id           BIGSERIAL PRIMARY KEY,
    target_date  DATE        NOT NULL,
    order_no     VARCHAR(20),
    kind         VARCHAR(30) NOT NULL,
    gateway_side TEXT,
    local_side   TEXT,
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_reconcile_diff_kind CHECK (
        kind IN ('MISSING_LOCAL', 'MISSING_GATEWAY', 'AMOUNT_MISMATCH', 'STATUS_MISMATCH')
    )
);

CREATE INDEX ix_reconcile_diff_date ON reconcile_diff (target_date, kind);

-- 같은 날짜를 다시 대사해도 같은 건이 쌓이지 않는다
CREATE UNIQUE INDEX uk_reconcile_diff_target ON reconcile_diff (target_date, order_no, kind)
    WHERE order_no IS NOT NULL;
