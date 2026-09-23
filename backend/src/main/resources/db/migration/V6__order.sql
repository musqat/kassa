CREATE TABLE address (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    receiver    VARCHAR(50)  NOT NULL,
    phone_enc   VARCHAR(255) NOT NULL,
    zipcode     VARCHAR(10)  NOT NULL,
    addr1       VARCHAR(200) NOT NULL,
    addr2       VARCHAR(200),
    is_default  BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_address_user ON address (user_id);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_no      VARCHAR(20)  NOT NULL,
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    item_amount   BIGINT       NOT NULL,
    shipping_fee  BIGINT       NOT NULL,
    total_amount  BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,

    receiver      VARCHAR(50)  NOT NULL,
    phone_enc     VARCHAR(255) NOT NULL,
    zipcode       VARCHAR(10)  NOT NULL,
    addr1         VARCHAR(200) NOT NULL,
    addr2         VARCHAR(200),

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    paid_at       TIMESTAMPTZ,
    closed_at     TIMESTAMPTZ,

    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'PAID', 'SHIPPED', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_orders_amount CHECK (item_amount >= 0 AND shipping_fee >= 0 AND total_amount >= 0)
);

CREATE INDEX ix_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status_created ON orders (status, created_at);

CREATE TABLE order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT       NOT NULL REFERENCES orders (id),
    product_id    BIGINT       NOT NULL REFERENCES product (id),
    product_name  VARCHAR(100) NOT NULL,
    price         BIGINT       NOT NULL,
    quantity      INT          NOT NULL,

    CONSTRAINT ck_order_item_quantity CHECK (quantity BETWEEN 1 AND 99)
);

CREATE INDEX ix_order_item_order ON order_item (order_id);
