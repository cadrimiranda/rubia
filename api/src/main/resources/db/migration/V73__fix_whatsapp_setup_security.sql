-- Configure security and performance for WhatsApp setup (Fixed version)
-- Add unique index to prevent multiple primary instances per company (PostgreSQL partial index)
DROP INDEX IF EXISTS idx_whatsapp_instances_company_primary_unique;
CREATE UNIQUE INDEX idx_whatsapp_instances_company_primary_unique 
    ON whatsapp_instances(company_id) 
    WHERE is_primary = true AND is_active = true;

-- Improve phone number constraint to allow reuse of deactivated numbers (PostgreSQL partial index)
ALTER TABLE whatsapp_instances 
    DROP CONSTRAINT IF EXISTS uq_whatsapp_instances_phone;

DROP INDEX IF EXISTS idx_whatsapp_instances_phone_active_unique;
CREATE UNIQUE INDEX idx_whatsapp_instances_phone_active_unique 
    ON whatsapp_instances(phone_number) 
    WHERE is_active = true;

-- Add performance index for filtering active instances by status
CREATE INDEX IF NOT EXISTS idx_whatsapp_instances_company_status_active 
    ON whatsapp_instances(company_id, status) 
    WHERE is_active = true;

-- Create function to automatically set first instance as primary
CREATE OR REPLACE FUNCTION ensure_primary_instance()
RETURNS TRIGGER AS $$
BEGIN
    -- If this is the first active instance for the company, make it primary
    IF NEW.is_active = true AND NOT EXISTS (
        SELECT 1 FROM whatsapp_instances 
        WHERE company_id = NEW.company_id 
        AND is_active = true 
        AND is_primary = true 
        AND id != NEW.id
    ) THEN
        NEW.is_primary = true;
    END IF;
    
    -- If setting as primary, ensure no other instance is primary for this company
    IF NEW.is_primary = true AND NEW.is_active = true THEN
        UPDATE whatsapp_instances 
        SET is_primary = false, updated_at = CURRENT_TIMESTAMP
        WHERE company_id = NEW.company_id 
        AND is_active = true 
        AND is_primary = true 
        AND id != NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to ensure primary instance logic
DROP TRIGGER IF EXISTS trigger_ensure_primary_instance ON whatsapp_instances;
CREATE TRIGGER trigger_ensure_primary_instance
    BEFORE INSERT OR UPDATE ON whatsapp_instances
    FOR EACH ROW
    EXECUTE FUNCTION ensure_primary_instance();

-- Add sample data validation
-- Ensure we have valid enum values (only if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'ck_whatsapp_instances_provider_updated' 
        AND table_name = 'whatsapp_instances'
    ) THEN
        ALTER TABLE whatsapp_instances 
            ADD CONSTRAINT ck_whatsapp_instances_provider_updated 
            CHECK (provider IN ('Z_API', 'TWILIO', 'WHATSAPP_BUSINESS_API', 'MOCK'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'ck_whatsapp_instances_status_updated' 
        AND table_name = 'whatsapp_instances'
    ) THEN
        ALTER TABLE whatsapp_instances 
            ADD CONSTRAINT ck_whatsapp_instances_status_updated 
            CHECK (status IN ('NOT_CONFIGURED', 'CONFIGURING', 'AWAITING_QR_SCAN', 'CONNECTING', 'CONNECTED', 'DISCONNECTED', 'ERROR', 'SUSPENDED'));
    END IF;
END $$;

-- Add constraint to ensure phone numbers are properly formatted (only if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'ck_whatsapp_instances_phone_format' 
        AND table_name = 'whatsapp_instances'
    ) THEN
        ALTER TABLE whatsapp_instances 
            ADD CONSTRAINT ck_whatsapp_instances_phone_format 
            CHECK (phone_number ~ '^\+?[1-9]\d{10,14}$');
    END IF;
END $$;

-- Create index for webhook lookups by instance_id
CREATE INDEX IF NOT EXISTS idx_whatsapp_instances_instance_id_active 
    ON whatsapp_instances(instance_id) 
    WHERE instance_id IS NOT NULL AND is_active = true;

-- Add useful views for monitoring
CREATE OR REPLACE VIEW company_whatsapp_status AS
SELECT 
    c.id as company_id,
    c.name as company_name,
    c.slug as company_slug,
    COUNT(wi.id) as total_instances,
    COUNT(CASE WHEN wi.status = 'CONNECTED' THEN 1 END) as connected_instances,
    COUNT(CASE WHEN wi.status = 'ERROR' THEN 1 END) as error_instances,
    MAX(CASE WHEN wi.is_primary = true THEN wi.phone_number END) as primary_phone,
    MAX(CASE WHEN wi.is_primary = true THEN wi.status END) as primary_status,
    BOOL_OR(wi.status = 'CONNECTED') as has_connected_instance,
    BOOL_OR(wi.status != 'NOT_CONFIGURED') as has_configured_instance
FROM companies c
LEFT JOIN whatsapp_instances wi ON c.id = wi.company_id AND wi.is_active = true
WHERE c.is_active = true
GROUP BY c.id, c.name, c.slug
ORDER BY c.name;

-- Grant permissions
GRANT SELECT ON company_whatsapp_status TO PUBLIC;

-- Add comment for monitoring
COMMENT ON VIEW company_whatsapp_status IS 'View for monitoring WhatsApp setup status across all companies';