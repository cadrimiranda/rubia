CREATE TABLE message_templates (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID,
    ai_agent_id UUID,
    tone VARCHAR(255),
    last_edited_by_user_id UUID,
    edit_count INT DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_created_by_user
        FOREIGN KEY(created_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_last_edited_by_user
        FOREIGN KEY(last_edited_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_ai_agent
        FOREIGN KEY(ai_agent_id)
        REFERENCES ai_agents(id)
);
CREATE TRIGGER update_message_templates_updated_at BEFORE UPDATE ON message_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
