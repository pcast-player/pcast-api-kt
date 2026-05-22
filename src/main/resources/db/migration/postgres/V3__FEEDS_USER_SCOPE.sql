-- Drop existing feeds data (no production data; adding mandatory user ownership)
DELETE FROM feeds;

-- Add user ownership column
ALTER TABLE feeds
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE;

-- Replace old nano_id unique index with per-user uniqueness
DROP INDEX IF EXISTS nanoid_idx;
CREATE UNIQUE INDEX feeds_user_nanoid_idx ON feeds (user_id, nano_id);
