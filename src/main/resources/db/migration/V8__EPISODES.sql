CREATE TABLE episodes (
    id UUID PRIMARY KEY,
    feed_id UUID NOT NULL REFERENCES feeds(id) ON DELETE CASCADE,
    guid VARCHAR(1024) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    media_url TEXT NOT NULL,
    media_type VARCHAR(255),
    duration_seconds BIGINT,
    published_at TIMESTAMP,
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (feed_id, guid)
);

CREATE INDEX episodes_feed_published_idx ON episodes(feed_id, published_at DESC);
CREATE INDEX episodes_feed_created_idx ON episodes(feed_id, created_at DESC);

CREATE TABLE episode_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    position_seconds BIGINT NOT NULL,
    duration_seconds BIGINT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, episode_id)
);

CREATE INDEX episode_progress_user_updated_idx ON episode_progress(user_id, updated_at DESC);
