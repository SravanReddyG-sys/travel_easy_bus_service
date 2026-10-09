-- ====================================================================
-- TravelEasy Bus Microservice - Initial Schema Migration (V1)
-- ====================================================================

-- Enable UUID extension if supported
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schema baseline marker table for Phase 1 verification
CREATE TABLE IF NOT EXISTS service_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO service_metadata (service_name, version) 
VALUES ('traveleasy-bus-service', '1.0.0-SNAPSHOT')
ON CONFLICT DO NOTHING;
