CREATE TABLE conversation_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    customer_id UUID,
    user_id UUID,
    ai_agent_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_conversation
        FOREIGN KEY(conversation_id)
        REFERENCES conversations(id),
    CONSTRAINT fk_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id),
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id),
    CONSTRAINT fk_ai_agent
        FOREIGN KEY(ai_agent_id)
        REFERENCES ai_agents(id),
    UNIQUE (conversation_id, customer_id),
    UNIQUE (conversation_id, user_id),
    UNIQUE (conversation_id, ai_agent_id),
    CHECK (
        (CASE WHEN customer_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN user_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN ai_agent_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);
