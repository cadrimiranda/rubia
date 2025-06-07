-- Add company_id to all tenant-specific tables for multi-tenant isolation

-- Add company_id to departments
ALTER TABLE departments 
ADD COLUMN company_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD CONSTRAINT fk_departments_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to users  
ALTER TABLE users
ADD COLUMN company_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD CONSTRAINT fk_users_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to customers
ALTER TABLE customers
ADD COLUMN company_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD CONSTRAINT fk_customers_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to conversations
ALTER TABLE conversations
ADD COLUMN company_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD CONSTRAINT fk_conversations_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to messages
ALTER TABLE messages
ADD COLUMN company_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD CONSTRAINT fk_messages_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX idx_departments_company_id ON departments(company_id);
CREATE INDEX idx_users_company_id ON users(company_id);
CREATE INDEX idx_customers_company_id ON customers(company_id);
CREATE INDEX idx_conversations_company_id ON conversations(company_id);
CREATE INDEX idx_messages_company_id ON messages(company_id);

-- Update unique constraints to be company-scoped
ALTER TABLE customers DROP CONSTRAINT customers_phone_key;
ALTER TABLE customers ADD CONSTRAINT customers_phone_company_unique UNIQUE (phone, company_id);

ALTER TABLE users DROP CONSTRAINT users_whatsapp_number_key;
ALTER TABLE users ADD CONSTRAINT users_whatsapp_number_company_unique UNIQUE (whatsapp_number, company_id);