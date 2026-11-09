-- Email verification is orthogonal to the admin-managed status: a user is ACTIVE or DISABLED, and
-- separately has or has not proven ownership of the mailbox. Login requires both ACTIVE and verified.
ALTER TABLE users ADD COLUMN email_verified_at timestamptz;

-- Accounts created before verification existed keep working: treat them as verified at creation.
UPDATE users SET email_verified_at = created_at;

ALTER TABLE users ADD CONSTRAINT ck_users_email_verified_at
    CHECK (email_verified_at IS NULL OR email_verified_at >= created_at);

-- Single-use secrets delivered by email. Only the SHA-256 hash is stored; the raw token lives in the link.
-- purpose prevents a verification token from being replayed as a password reset and vice versa.
CREATE TABLE one_time_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    purpose varchar(32) NOT NULL,
    token_hash varchar(64) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,

    CONSTRAINT ck_one_time_tokens_uuid_v7 CHECK ((uuid_extract_version(id) = 7) IS TRUE),
    CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_one_time_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_one_time_tokens_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_one_time_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT ck_one_time_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_one_time_tokens_consumption CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);

-- Invalidating older tokens of the same purpose and the per-account send cooldown both look up by user and purpose.
CREATE INDEX idx_one_time_tokens_user_purpose ON one_time_tokens (user_id, purpose, created_at DESC);
