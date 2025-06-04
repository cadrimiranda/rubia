-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    assigned_user_id UUID REFERENCES users(id),
    department_id UUID REFERENCES departments(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ENTRADA' 
        CHECK (status IN ('ENTRADA', 'ESPERANDO', 'FINALIZADOS')),
    channel VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP'
        CHECK (channel IN ('WHATSAPP', 'WEB', 'TELEGRAM', 'EMAIL')),
    priority INTEGER DEFAULT 0,
    is_pinned BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    closed_at TIMESTAMP WITH TIME ZONE
);

-- Create trigger for updated_at
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_conversations_customer_id ON conversations(customer_id);
CREATE INDEX idx_conversations_assigned_user_id ON conversations(assigned_user_id);
CREATE INDEX idx_conversations_department_id ON conversations(department_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_channel ON conversations(channel);
CREATE INDEX idx_conversations_status_updated ON conversations(status, updated_at DESC);
CREATE INDEX idx_conversations_is_pinned ON conversations(is_pinned);
CREATE INDEX idx_conversations_priority ON conversations(priority);

-- Insert test conversations
INSERT INTO conversations (customer_id, assigned_user_id, department_id, status, channel, priority, is_pinned) 
SELECT 
    c.id,
    (SELECT id FROM users WHERE email = 'admin@rubia.com'),
    (SELECT id FROM departments WHERE name = 'Comercial'),
    CASE 
        WHEN RANDOM() < 0.4 THEN 'ENTRADA'
        WHEN RANDOM() < 0.7 THEN 'ESPERANDO'
        ELSE 'FINALIZADOS'
    END,
    'WHATSAPP',
    FLOOR(RANDOM() * 3) - 1, -- -1, 0, 1
    RANDOM() < 0.2 -- 20% chance of being pinned
FROM customers c
LIMIT 5;