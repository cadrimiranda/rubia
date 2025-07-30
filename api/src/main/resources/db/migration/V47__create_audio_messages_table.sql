-- Create message_direction enum
CREATE TYPE message_direction AS ENUM ('INCOMING', 'OUTGOING');

-- Create processing_status enum  
CREATE TYPE processing_status AS ENUM ('RECEIVED', 'DOWNLOADING', 'PROCESSING', 'COMPLETED', 'FAILED');

-- Create audio_messages table
CREATE TABLE audio_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id VARCHAR(255) UNIQUE NOT NULL,
    from_number VARCHAR(20) NOT NULL,
    to_number VARCHAR(20),
    direction message_direction NOT NULL,
    audio_url VARCHAR(500),
    file_path VARCHAR(500),
    mime_type VARCHAR(100),
    duration_seconds INTEGER,
    file_size_bytes BIGINT,
    status processing_status NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    conversation_id UUID REFERENCES conversations(id) ON DELETE SET NULL
);

-- Create indexes for better performance
CREATE INDEX idx_audio_messages_message_id ON audio_messages(message_id);
CREATE INDEX idx_audio_messages_conversation_id ON audio_messages(conversation_id);
CREATE INDEX idx_audio_messages_status ON audio_messages(status);
CREATE INDEX idx_audio_messages_created_at ON audio_messages(created_at);