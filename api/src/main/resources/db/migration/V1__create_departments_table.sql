-- Create departments table
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    auto_assign BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create trigger to automatically update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create index for performance
CREATE INDEX idx_departments_name ON departments(name);
CREATE INDEX idx_departments_auto_assign ON departments(auto_assign);

-- Insert default departments
INSERT INTO departments (name, description, auto_assign) VALUES
    ('Comercial', 'Departamento de vendas e atendimento comercial', true),
    ('Suporte', 'Departamento de suporte técnico', true),
    ('Vendas', 'Departamento especializado em vendas', true);