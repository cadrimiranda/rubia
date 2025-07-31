-- Add chat_lid column to conversations table for Z-API integration
ALTER TABLE conversations ADD COLUMN chat_lid VARCHAR(255);

-- Create unique index on chat_lid for faster lookups
CREATE UNIQUE INDEX idx_conversations_chat_lid ON conversations(chat_lid) WHERE chat_lid IS NOT NULL;

-- Add comment explaining the purpose
COMMENT ON COLUMN conversations.chat_lid IS 'WhatsApp chat LID from Z-API webhook for conversation linking';