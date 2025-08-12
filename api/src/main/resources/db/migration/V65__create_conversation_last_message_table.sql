-- Create conversation_last_message table for CQRS optimized queries
CREATE TABLE conversation_last_message (
    conversation_id UUID NOT NULL PRIMARY KEY,
    last_message_date TIMESTAMP NOT NULL,
    last_message_id UUID,
    last_message_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_conversation_last_message_conversation 
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) 
        ON DELETE CASCADE
);

-- Create index for efficient ordering by last_message_date
CREATE INDEX idx_conversation_last_message_date ON conversation_last_message(last_message_date DESC);

-- Add trigger to update updated_at timestamp
CREATE TRIGGER trigger_conversation_last_message_updated_at
    BEFORE UPDATE ON conversation_last_message
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();