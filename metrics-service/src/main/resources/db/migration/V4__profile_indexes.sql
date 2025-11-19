CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles (user_id);

CREATE INDEX IF NOT EXISTS idx_user_events_user_id_event_type
    ON user_events (user_id, event_type);

CREATE INDEX IF NOT EXISTS idx_user_events_timestamp
    ON user_events (event_timestamp);

