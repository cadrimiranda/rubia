-- Create customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255),
    whatsapp_id VARCHAR(255) UNIQUE,
    profile_url TEXT,
    is_blocked BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create trigger for updated_at
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_whatsapp_id ON customers(whatsapp_id);
CREATE INDEX idx_customers_is_blocked ON customers(is_blocked);
CREATE INDEX idx_customers_name ON customers(name);

-- Insert some test customers
INSERT INTO customers (phone, name, whatsapp_id, is_blocked) VALUES
    ('+5511999999001', 'Lilia - AT Santa Casa', 'wa_001', false),
    ('+5511999999002', 'Kelly Rocha - BIOCOR', 'wa_002', false),
    ('+5511999999003', 'Evandro Thiesen - Clínica LeVitá', 'wa_003', false),
    ('+5511999999004', 'Maria Luiza - BIOCOR', 'wa_004', false),
    ('+5511999999005', 'Allan Bruno', 'wa_005', false);