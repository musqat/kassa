ALTER TABLE users ADD COLUMN login_id VARCHAR(20);
UPDATE users SET login_id = 'user' || id;
ALTER TABLE users ALTER COLUMN login_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);

ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN token_valid_after TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN deleted_at        TIMESTAMPTZ;

CREATE TABLE email_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    purpose     VARCHAR(20)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_email_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_email_token_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD', 'FIND_LOGIN_ID'))
);

CREATE INDEX ix_email_token_user_purpose ON email_token (user_id, purpose);
