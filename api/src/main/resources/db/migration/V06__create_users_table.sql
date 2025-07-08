CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    department_id UUID,
    company_id UUID NOT NULL,
    role UserRole NOT NULL,
    avatar_url VARCHAR(255),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP WITHOUT TIME ZONE,
    whatsapp_number VARCHAR(255) UNIQUE,
    is_whatsapp_active BOOLEAN DEFAULT FALSE,
    birth_date DATE,
    weight DOUBLE PRECISION,
    height DOUBLE PRECISION,
    address VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_department
        FOREIGN KEY(department_id)
        REFERENCES departments(id)
);
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
