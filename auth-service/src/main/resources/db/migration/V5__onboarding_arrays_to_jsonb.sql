-- Convert array columns to JSONB for easier Hibernate mapping
ALTER TABLE onboarding_status
    ALTER COLUMN selected_categories DROP DEFAULT,
    ALTER COLUMN selected_content_types DROP DEFAULT;

ALTER TABLE onboarding_status
    ALTER COLUMN selected_categories TYPE jsonb
        USING COALESCE(to_jsonb(selected_categories), '[]'::jsonb),
    ALTER COLUMN selected_content_types TYPE jsonb
        USING COALESCE(to_jsonb(selected_content_types), '[]'::jsonb);

ALTER TABLE onboarding_status
    ALTER COLUMN selected_categories SET DEFAULT '[]'::jsonb,
    ALTER COLUMN selected_content_types SET DEFAULT '[]'::jsonb;

