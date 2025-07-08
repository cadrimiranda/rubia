CREATE TABLE user_ai_agents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ai_agent_id UUID NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_default BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id),
    CONSTRAINT fk_ai_agent
        FOREIGN KEY(ai_agent_id)
        REFERENCES ai_agents(id),
    UNIQUE (user_id, ai_agent_id)
);
