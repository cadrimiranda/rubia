-- Add deleted_at column to message_templates table for soft delete functionality
ALTER TABLE message_templates 
ADD COLUMN deleted_at TIMESTAMP NULL;

-- Add index for better performance when filtering non-deleted templates
CREATE INDEX idx_message_templates_deleted_at ON message_templates(deleted_at);

-- Add partial index for active templates only (deleted_at IS NULL)
CREATE INDEX idx_message_templates_active ON message_templates(company_id, deleted_at) WHERE deleted_at IS NULL;