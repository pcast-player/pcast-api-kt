ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE passkey_challenges
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN passkey_user_handle VARCHAR(86);

CREATE INDEX passkey_challenges_email_idx ON passkey_challenges(email);
