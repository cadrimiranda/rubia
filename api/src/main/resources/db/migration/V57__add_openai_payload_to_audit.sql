-- Migration to add OpenAI payload tracking fields to message enhancement audit table

-- Add new columns for OpenAI payload tracking
ALTER TABLE message_enhancement_audit
ADD COLUMN openai_system_message TEXT,
ADD COLUMN openai_user_message TEXT,
ADD COLUMN openai_full_payload TEXT;

-- Add comments for the new fields
COMMENT ON COLUMN message_enhancement_audit.openai_system_message IS 'System message sent to OpenAI API';
COMMENT ON COLUMN message_enhancement_audit.openai_user_message IS 'User message (prompt) sent to OpenAI API';
COMMENT ON COLUMN message_enhancement_audit.openai_full_payload IS 'Complete JSON payload sent to OpenAI for debugging purposes';

-- Add index for searching by payload content (useful for debugging similar prompts)
CREATE INDEX idx_message_enhancement_audit_system_message ON message_enhancement_audit 
USING gin(to_tsvector('portuguese', openai_system_message));

CREATE INDEX idx_message_enhancement_audit_user_message ON message_enhancement_audit 
USING gin(to_tsvector('portuguese', openai_user_message));