CREATE TABLE customers (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    whatsapp_id VARCHAR(255),
    profile_url VARCHAR(255),
    is_blocked BOOLEAN DEFAULT FALSE,
    source_system_name VARCHAR(255),
    source_system_id VARCHAR(255),
    imported_at TIMESTAMP WITHOUT TIME ZONE,
    birth_date DATE,
    last_donation_date DATE,
    next_eligible_donation_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id)
);
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
