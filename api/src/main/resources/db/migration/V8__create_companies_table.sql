-- Create companies table for multi-tenant support
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    logo_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    plan_type VARCHAR(50) DEFAULT 'BASIC',
    max_users INTEGER DEFAULT 10,
    max_whatsapp_numbers INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX idx_companies_slug ON companies(slug);
CREATE INDEX idx_companies_is_active ON companies(is_active);