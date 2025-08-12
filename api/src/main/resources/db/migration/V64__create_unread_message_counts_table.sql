-- Create unread_message_counts table for simple counter-based notifications
CREATE TABLE IF NOT EXISTS unread_message_counts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    unread_count INTEGER NOT NULL DEFAULT 0,
    last_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_unread_user_conversation UNIQUE (user_id, conversation_id)
);

-- Create indexes for optimal performance
CREATE INDEX IF NOT EXISTS idx_unread_user_conversation 
ON unread_message_counts(user_id, conversation_id);

CREATE INDEX IF NOT EXISTS idx_unread_user_company 
ON unread_message_counts(user_id, company_id);

CREATE INDEX IF NOT EXISTS idx_unread_count_filter 
ON unread_message_counts(user_id) WHERE unread_count > 0;

-- Create trigger to automatically update updated_at
CREATE OR REPLACE FUNCTION update_unread_counts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_unread_counts_updated_at
    BEFORE UPDATE ON unread_message_counts
    FOR EACH ROW
    EXECUTE FUNCTION update_unread_counts_updated_at();

-- Add comment to the table
COMMENT ON TABLE unread_message_counts IS 'Simple counter-based unread message tracking per user per conversation';