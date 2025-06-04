-- Create messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    sender_type VARCHAR(20) NOT NULL CHECK (sender_type IN ('CUSTOMER', 'AGENT', 'AI', 'SYSTEM')),
    sender_id UUID, -- user_id when sender_type = 'AGENT'
    message_type VARCHAR(20) DEFAULT 'TEXT' 
        CHECK (message_type IN ('TEXT', 'IMAGE', 'AUDIO', 'FILE', 'LOCATION', 'CONTACT')),
    media_url TEXT,
    external_message_id VARCHAR(255), -- ID from WhatsApp/provider
    is_ai_generated BOOLEAN DEFAULT false,
    ai_confidence DECIMAL(3,2), -- Score 0.00-1.00
    status VARCHAR(20) DEFAULT 'SENT' 
        CHECK (status IN ('SENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for performance
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_sender_type ON messages(sender_type);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_message_type ON messages(message_type);
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_external_id ON messages(external_message_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_is_ai_generated ON messages(is_ai_generated);

-- Create full-text search index for content (Portuguese)
CREATE INDEX idx_messages_content_gin ON messages USING gin(to_tsvector('portuguese', content));

-- Insert test messages for existing conversations
INSERT INTO messages (conversation_id, content, sender_type, sender_id, message_type, status, created_at)
SELECT 
    c.id,
    CASE 
        WHEN RANDOM() < 0.5 THEN 'Olá! Gostaria de mais informações sobre os serviços.'
        WHEN RANDOM() < 0.7 THEN 'Obrigado pelo atendimento!'
        ELSE 'Pode me ajudar com uma dúvida?'
    END,
    'CUSTOMER',
    NULL,
    'TEXT',
    'DELIVERED',
    c.created_at + INTERVAL '1 minute'
FROM conversations c;

-- Insert agent responses
INSERT INTO messages (conversation_id, content, sender_type, sender_id, message_type, status, created_at)
SELECT 
    c.id,
    CASE 
        WHEN RANDOM() < 0.5 THEN 'Olá! Claro, posso te ajudar. Sobre qual serviço você gostaria de saber?'
        WHEN RANDOM() < 0.7 THEN 'De nada! Estou aqui para ajudar.'
        ELSE 'Com certeza! Me conte qual é sua dúvida.'
    END,
    'AGENT',
    c.assigned_user_id,
    'TEXT',
    'READ',
    c.created_at + INTERVAL '2 minutes'
FROM conversations c
WHERE c.assigned_user_id IS NOT NULL;