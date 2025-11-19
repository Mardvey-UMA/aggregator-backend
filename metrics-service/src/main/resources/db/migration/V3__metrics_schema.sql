-- Metrics service schema
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    total_views INTEGER NOT NULL DEFAULT 0,
    total_clicks INTEGER NOT NULL DEFAULT 0,
    total_likes INTEGER NOT NULL DEFAULT 0,
    total_dislikes INTEGER NOT NULL DEFAULT 0,
    total_bookmarks INTEGER NOT NULL DEFAULT 0,
    total_sessions INTEGER NOT NULL DEFAULT 0,

    category_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    entity_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    content_type_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    style_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,

    avg_dwell_time_seconds REAL NOT NULL DEFAULT 0,
    avg_session_length_seconds INTEGER NOT NULL DEFAULT 0,
    active_hours INTEGER[] NOT NULL DEFAULT '{}'::INTEGER[],
    preferred_content_length VARCHAR(20),

    clickbait_tolerance REAL NOT NULL DEFAULT 0.5,
    exploration_vs_exploitation REAL NOT NULL DEFAULT 0.5,

    liked_posts_last_30d TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
    disliked_posts_last_30d TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
    viewed_posts_last_7d TEXT[] NOT NULL DEFAULT '{}'::TEXT[],

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE user_profiles IS 'Aggregated user preference profiles derived from behavioral signals';

CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles (user_id);

CREATE TABLE IF NOT EXISTS user_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    session_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    platform VARCHAR(20),
    app_version VARCHAR(20),
    request_id UUID,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE user_events IS 'Immutable log of all interaction events ingested by the metrics service';

CREATE INDEX IF NOT EXISTS idx_user_events_user_timestamp ON user_events (user_id, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_user_events_type_timestamp ON user_events (event_type, event_timestamp);

CREATE TABLE IF NOT EXISTS event_batches (
    id UUID PRIMARY KEY,
    batch_id UUID UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    event_count INTEGER NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE event_batches IS 'Tracks ingestion batches for deduplication and replay';

CREATE INDEX IF NOT EXISTS idx_event_batches_user_id ON event_batches (user_id);

CREATE OR REPLACE FUNCTION set_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_profiles_updated
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_timestamp();

CREATE TRIGGER trg_user_events_updated
    BEFORE UPDATE ON user_events
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_timestamp();

CREATE TRIGGER trg_event_batches_updated
    BEFORE UPDATE ON event_batches
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_timestamp();

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('user_events', 'event_timestamp', if_not_exists => TRUE);
    ELSE
        RAISE NOTICE 'timescaledb extension not installed; continuing without hypertable';
    END IF;
END;
$$;

