-- ============================================================================
-- V1__initial_schema.sql
-- Initial database schema for Authentication Service
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Users table
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    roles VARCHAR(255) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);

COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON COLUMN users.id IS 'Unique identifier for the user';
COMMENT ON COLUMN users.email IS 'User email address, used for login';
COMMENT ON COLUMN users.username IS 'Unique username for display';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password, null for OAuth-only users';
COMMENT ON COLUMN users.email_verified IS 'Whether user has verified their email';
COMMENT ON COLUMN users.enabled IS 'Whether user account is active';
COMMENT ON COLUMN users.roles IS 'Comma-separated list of user roles';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user was last updated';
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of last successful login';

-- Indexes for users table
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_last_login_at ON users (last_login_at);
CREATE INDEX idx_users_enabled ON users (enabled) WHERE enabled = TRUE;

-- ============================================================================
-- OAuth accounts table
-- ============================================================================
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_username VARCHAR(255),
    provider_email VARCHAR(255),
    access_token VARCHAR(1000),
    refresh_token VARCHAR(1000),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_oauth_accounts_provider_provider_id UNIQUE (provider, provider_id)
);

COMMENT ON TABLE oauth_accounts IS 'OAuth2 provider accounts linked to users';
COMMENT ON COLUMN oauth_accounts.id IS 'Unique identifier for the OAuth account';
COMMENT ON COLUMN oauth_accounts.user_id IS 'Reference to the user';
COMMENT ON COLUMN oauth_accounts.provider IS 'OAuth provider name (VK, GOOGLE, etc.)';
COMMENT ON COLUMN oauth_accounts.provider_id IS 'User ID from the OAuth provider';
COMMENT ON COLUMN oauth_accounts.provider_username IS 'Username from the OAuth provider';
COMMENT ON COLUMN oauth_accounts.provider_email IS 'Email from the OAuth provider';
COMMENT ON COLUMN oauth_accounts.access_token IS 'OAuth access token (encrypted)';
COMMENT ON COLUMN oauth_accounts.refresh_token IS 'OAuth refresh token (encrypted)';
COMMENT ON COLUMN oauth_accounts.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN oauth_accounts.created_at IS 'Timestamp when account was linked';
COMMENT ON COLUMN oauth_accounts.updated_at IS 'Timestamp when account was last updated';

-- Indexes for oauth_accounts table
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts (user_id);
CREATE INDEX idx_oauth_accounts_provider ON oauth_accounts (provider);
CREATE INDEX idx_oauth_accounts_provider_id ON oauth_accounts (provider, provider_id);

-- ============================================================================
-- Refresh tokens table
-- ============================================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token)
);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for token refresh flow';
COMMENT ON COLUMN refresh_tokens.id IS 'Unique identifier for the token';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to the user';
COMMENT ON COLUMN refresh_tokens.token IS 'The refresh token value';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Whether token has been revoked';
COMMENT ON COLUMN refresh_tokens.device_info IS 'User agent or device information';
COMMENT ON COLUMN refresh_tokens.ip_address IS 'IP address where token was issued';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Timestamp when token was created';
COMMENT ON COLUMN refresh_tokens.used_at IS 'Timestamp when token was last used';

-- Indexes for refresh_tokens table
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_valid ON refresh_tokens (user_id, revoked, expires_at)
    WHERE revoked = FALSE;

-- ============================================================================
-- Trigger function for auto-updating updated_at column
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Triggers for updated_at auto-update
-- ============================================================================
CREATE TRIGGER tr_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER tr_oauth_accounts_updated_at
    BEFORE UPDATE ON oauth_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Initial data (optional - admin user for testing)
-- ============================================================================
-- Uncomment if you want to create an initial admin user
-- INSERT INTO users (email, username, password_hash, email_verified, enabled, roles)
-- VALUES (
--     'admin@example.com',
--     'admin',
--     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.NTtE4jJzQBJlXi', -- password: admin123
--     TRUE,
--     TRUE,
--     'USER,ADMIN'
-- );
