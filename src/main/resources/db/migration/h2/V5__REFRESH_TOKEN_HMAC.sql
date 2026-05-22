-- Invalidate all existing refresh tokens (no production data).
-- Tokens stored with the old unsalted SHA-256 scheme are incompatible
-- with the new HMAC-SHA256 scheme introduced in this migration.
TRUNCATE TABLE refresh_tokens;

-- Widen token_hash to 88 chars to accommodate base64url HMAC-SHA256 output.
ALTER TABLE refresh_tokens ALTER COLUMN token_hash VARCHAR(88);
