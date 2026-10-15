-- ====================================================================
-- TravelEasy Bus Microservice - Email Verifications & Password Reset (V6)
-- Powered by Resend Transactional Email API
-- ====================================================================

CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL, -- 'REGISTRATION' or 'PASSWORD_RESET'
    data_payload TEXT,            -- Serialized registration details
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_verif_lookup ON email_verifications(LOWER(email), purpose, is_used);

-- Add email verified column to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT TRUE;
