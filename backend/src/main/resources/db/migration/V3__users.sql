CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    login_fail_count  INT          NOT NULL DEFAULT 0,
    locked_until      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_fail_count CHECK (login_fail_count >= 0)
);
