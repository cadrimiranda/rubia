-- Create WhatsApp Instances table for multi-instance support
CREATE TABLE whatsapp_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    provider VARCHAR(50) NOT NULL DEFAULT 'Z_API',
    instance_id VARCHAR(100),
    access_token TEXT,
    webhook_url VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_CONFIGURED',
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    last_connected_at TIMESTAMP,
    last_status_check TIMESTAMP,
    configuration_data TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_whatsapp_instances_company 
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_whatsapp_instances_phone 
        UNIQUE (phone_number),
    CONSTRAINT uq_whatsapp_instances_instance_id 
        UNIQUE (instance_id),
    CONSTRAINT ck_whatsapp_instances_provider 
        CHECK (provider IN ('Z_API', 'TWILIO', 'WHATSAPP_BUSINESS_API', 'MOCK')),
    CONSTRAINT ck_whatsapp_instances_status 
        CHECK (status IN ('NOT_CONFIGURED', 'CONFIGURING', 'AWAITING_QR_SCAN', 'CONNECTING', 'CONNECTED', 'DISCONNECTED', 'ERROR', 'SUSPENDED'))
);

-- Create indexes for better performance
CREATE INDEX idx_whatsapp_instances_company_id ON whatsapp_instances(company_id);
CREATE INDEX idx_whatsapp_instances_phone_number ON whatsapp_instances(phone_number);
CREATE INDEX idx_whatsapp_instances_instance_id ON whatsapp_instances(instance_id);
CREATE INDEX idx_whatsapp_instances_status ON whatsapp_instances(status);
CREATE INDEX idx_whatsapp_instances_active ON whatsapp_instances(is_active);
CREATE INDEX idx_whatsapp_instances_primary ON whatsapp_instances(company_id, is_primary) WHERE is_primary = true;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_whatsapp_instances_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_whatsapp_instances_updated_at
    BEFORE UPDATE ON whatsapp_instances
    FOR EACH ROW
    EXECUTE FUNCTION update_whatsapp_instances_updated_at();

-- Comments for documentation
COMMENT ON TABLE whatsapp_instances IS 'WhatsApp instances for companies with multi-provider support';
COMMENT ON COLUMN whatsapp_instances.provider IS 'Messaging provider: Z_API, TWILIO, WHATSAPP_BUSINESS_API, MOCK';
COMMENT ON COLUMN whatsapp_instances.status IS 'Instance status: NOT_CONFIGURED, CONFIGURING, AWAITING_QR_SCAN, CONNECTING, CONNECTED, DISCONNECTED, ERROR, SUSPENDED';
COMMENT ON COLUMN whatsapp_instances.is_primary IS 'Indicates if this is the primary instance for the company';
COMMENT ON COLUMN whatsapp_instances.configuration_data IS 'JSON configuration data specific to the provider';