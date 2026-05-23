ALTER TABLE users ADD COLUMN passkey_user_handle VARCHAR(86);

UPDATE users
SET passkey_user_handle = replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '')
WHERE passkey_user_handle IS NULL;

ALTER TABLE users ALTER COLUMN passkey_user_handle SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_passkey_user_handle_unique UNIQUE (passkey_user_handle);

CREATE TABLE passkey_credentials (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credential_id VARCHAR(1024) NOT NULL UNIQUE,
    public_key_cose TEXT NOT NULL,
    signature_count BIGINT NOT NULL DEFAULT 0,
    transports VARCHAR(255),
    nickname VARCHAR(255),
    backup_eligible BOOLEAN,
    backed_up BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP
);

CREATE INDEX passkey_credentials_user_idx ON passkey_credentials(user_id);
CREATE INDEX passkey_credentials_credential_idx ON passkey_credentials(credential_id);

CREATE TABLE passkey_challenges (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    challenge VARCHAR(512) NOT NULL UNIQUE,
    request_json TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX passkey_challenges_challenge_idx ON passkey_challenges(challenge);
CREATE INDEX passkey_challenges_expires_idx ON passkey_challenges(expires_at);
CREATE INDEX passkey_challenges_consumed_idx ON passkey_challenges(consumed_at);
