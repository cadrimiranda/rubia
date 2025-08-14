-- Fix message_drafts column types and add AI_CONTEXTUAL source type
-- This migration aligns the database schema with the entity changes

-- 1. Change confidence column from DECIMAL to REAL (float4) 
ALTER TABLE message_drafts ALTER COLUMN confidence TYPE REAL;

-- 2. Drop the old check constraint for source_type
ALTER TABLE message_drafts DROP CONSTRAINT IF EXISTS chk_message_drafts_source_type;

-- 3. Add new check constraint that includes AI_CONTEXTUAL
ALTER TABLE message_drafts ADD CONSTRAINT chk_message_drafts_source_type 
    CHECK (source_type IN ('FAQ', 'TEMPLATE', 'AI_GENERATED', 'AI_CONTEXTUAL', NULL));

-- 4. Update the comment to reflect the new source type
COMMENT ON COLUMN message_drafts.source_type IS 'Source type: FAQ, TEMPLATE, AI_GENERATED, or AI_CONTEXTUAL';