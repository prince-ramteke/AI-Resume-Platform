-- V8: Add email verification and OAuth fields to users table.
-- email_verified defaults FALSE; the UPDATE below grandfathers all existing
-- rows so existing LOCAL accounts are not locked out when the flag goes live.
-- password_hash becomes nullable to support future Google-only accounts.

ALTER TABLE users
    ADD COLUMN first_name       VARCHAR(100),
    ADD COLUMN last_name        VARCHAR(100),
    ADD COLUMN email_verified   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN auth_provider    VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_subject VARCHAR(255);

ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Grandfather all pre-existing accounts as verified (they already authenticated
-- via the old flow and never needed OTP verification).
UPDATE users SET email_verified = TRUE;

-- Partial unique index: only one identity per provider_subject, NULL excluded.
CREATE UNIQUE INDEX idx_users_provider_subject
    ON users (provider_subject)
    WHERE provider_subject IS NOT NULL;

-- OTP records for email verification workflow.
CREATE TABLE email_verifications (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash       VARCHAR(64)  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    attempt_count  INT          NOT NULL DEFAULT 0,
    last_resent_at TIMESTAMPTZ,
    used_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_verifications_user ON email_verifications (user_id);

-- One-time exchange codes for the OAuth callback token handoff.
CREATE TABLE oauth_exchange_codes (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_oauth_exchange_codes_user ON oauth_exchange_codes (user_id);
CREATE INDEX idx_oauth_exchange_codes_hash ON oauth_exchange_codes (code_hash);
