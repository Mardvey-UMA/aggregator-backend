-- ============================================================================
-- V4__onboarding_schema.sql
-- Adds onboarding status and category option tables
-- ============================================================================

-- Ensure pgcrypto extension for gen_random_uuid
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- Onboarding status table
-- ============================================================================
CREATE TABLE IF NOT EXISTS onboarding_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Progress tracking
    step VARCHAR(50) NOT NULL DEFAULT 'not_started',
    completed BOOLEAN DEFAULT FALSE,
    skipped BOOLEAN DEFAULT FALSE,

    -- Selected preferences
    selected_categories TEXT[] DEFAULT ARRAY[]::TEXT[],
    selected_content_types TEXT[] DEFAULT ARRAY[]::TEXT[],

    -- Metadata
    onboarding_version VARCHAR(10) DEFAULT 'v1',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_onboarding_user_id ON onboarding_status(user_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_completed ON onboarding_status(completed);
CREATE INDEX IF NOT EXISTS idx_onboarding_step ON onboarding_status(step);

CREATE TRIGGER IF NOT EXISTS update_onboarding_status_updated_at
    BEFORE UPDATE ON onboarding_status
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Category options reference data
-- ============================================================================
CREATE TABLE IF NOT EXISTS category_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_key VARCHAR(50) UNIQUE NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_category_enabled ON category_options(enabled);
CREATE INDEX IF NOT EXISTS idx_category_display_order ON category_options(display_order);

INSERT INTO category_options (category_key, category_name, description, display_order)
VALUES
    ('technology', 'Technology', 'AI, Programming, Gadgets, Startups', 1),
    ('science', 'Science', 'Physics, Biology, Space, Research', 2),
    ('business', 'Business', 'Finance, Marketing, Entrepreneurship', 3),
    ('entertainment', 'Entertainment', 'Movies, Music, Gaming, TV Shows', 4),
    ('sports', 'Sports', 'Football, Basketball, Esports', 5),
    ('lifestyle', 'Lifestyle', 'Travel, Food, Fashion, Health', 6),
    ('education', 'Education', 'Courses, Tutorials, Books, Learning', 7),
    ('news', 'News', 'World News, Politics, Local Events', 8)
ON CONFLICT (category_key) DO NOTHING;
