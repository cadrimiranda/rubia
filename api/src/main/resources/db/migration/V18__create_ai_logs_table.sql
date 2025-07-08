CREATE TABLE ai_logs (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    ai_agent_id UUID NOT NULL,
    user_id UUID,
    conversation_id UUID,
    message_id UUID,
    message_template_id UUID,
    request_prompt TEXT NOT NULL,
    raw_response TEXT,
    processed_response TEXT,
    tokens_used_input INT,
    tokens_used_output INT,
    estimated_cost NUMERIC(10, 8),
    status AILogStatus NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_ai_agent
        FOREIGN KEY(ai_agent_id)
        REFERENCES ai_agents(id),
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id),
    CONSTRAINT fk_conversation
        FOREIGN KEY(conversation_id)
        REFERENCES conversations(id),
    CONSTRAINT fk_message
        FOREIGN KEY(message_id)
        REFERENCES messages(id),
    CONSTRAINT fk_message_template
        FOREIGN KEY(message_template_id)
        REFERENCES message_templates(id)
);
