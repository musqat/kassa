CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT       NOT NULL REFERENCES category (id),
    name            VARCHAR(200) NOT NULL,
    price           BIGINT       NOT NULL,
    stock           INT          NOT NULL DEFAULT 0,
    reserved_stock  INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL,
    thumbnail_url   VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_product_price             CHECK (price >= 0),
    CONSTRAINT ck_product_stock             CHECK (stock >= 0),
    CONSTRAINT ck_product_reserved_stock    CHECK (reserved_stock >= 0),
    CONSTRAINT ck_product_reserved_le_stock CHECK (reserved_stock <= stock)
);

CREATE INDEX idx_product_category_status ON product (category_id, status);
