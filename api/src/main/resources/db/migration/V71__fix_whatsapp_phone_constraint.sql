-- Fix constraint that failed in V45 due to view dependency
-- Drop view temporarily to allow constraint addition
DROP VIEW IF EXISTS company_whatsapp_status;

-- Add the phone format constraint that failed in V45
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_whatsapp_instances_phone_format'
    ) THEN
        ALTER TABLE whatsapp_instances 
            ADD CONSTRAINT ck_whatsapp_instances_phone_format 
            CHECK (phone_number ~ '^\+?[1-9]\d{10,14}$');
    END IF;
END $$;

-- Recreate the view
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

-- Add comment
COMMENT ON VIEW company_whatsapp_status IS 'View for monitoring WhatsApp setup status across all companies';