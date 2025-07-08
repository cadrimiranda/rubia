CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    content TEXT,
    sender_type SenderType NOT NULL,
    sender_id UUID,
    status MessageStatus NOT NULL DEFAULT 'SENT',
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    read_at TIMESTAMP WITHOUT TIME ZONE,
    external_message_id VARCHAR(255),
    is_ai_generated BOOLEAN,
    ai_confidence DOUBLE PRECISION,
    ai_agent_id UUID,
    message_template_id UUID,
    conversation_media_id UUID UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation
        FOREIGN KEY(conversation_id)
        REFERENCES conversations(id),
    CONSTRAINT fk_ai_agent
        FOREIGN KEY(ai_agent_id)
        REFERENCES ai_agents(id),
    CONSTRAINT fk_message_template
        FOREIGN KEY(message_template_id)
        REFERENCES message_templates(id),
    CONSTRAINT fk_conversation_media
        FOREIGN KEY(conversation_media_id)
        REFERENCES conversation_media(id)
);
