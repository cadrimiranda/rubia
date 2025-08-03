-- Migration to fix temperature_used column type for Hibernate compatibility

-- Change temperature_used column type from DECIMAL(3,2) to DOUBLE PRECISION
-- This ensures compatibility with Java Double type and Hibernate expectations
ALTER TABLE message_enhancement_audit 
ALTER COLUMN temperature_used TYPE DOUBLE PRECISION;

-- Add comment explaining the type choice
COMMENT ON COLUMN message_enhancement_audit.temperature_used IS 'Temperature parameter used in AI model (0.0 to 2.0) - stored as double precision for Hibernate compatibility';