CREATE TABLE ai_agents (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar_url VARCHAR(255),
    ai_model_type VARCHAR(255) NOT NULL,
    temperament VARCHAR(255) NOT NULL,
    max_response_length INT DEFAULT 500,
    temperature NUMERIC(3, 2) DEFAULT 0.7,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id)
);
CREATE TRIGGER update_ai_agents_updated_at BEFORE UPDATE ON ai_agents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
