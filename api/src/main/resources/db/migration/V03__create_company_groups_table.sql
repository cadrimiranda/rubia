CREATE TABLE company_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE TRIGGER update_company_groups_updated_at BEFORE UPDATE ON company_groups FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
