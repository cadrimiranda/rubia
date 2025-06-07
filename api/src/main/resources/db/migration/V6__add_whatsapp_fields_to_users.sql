-- Add WhatsApp fields to users table for multi-client support
ALTER TABLE users 
ADD COLUMN whatsapp_number VARCHAR(20) UNIQUE,
ADD COLUMN is_whatsapp_active BOOLEAN DEFAULT FALSE;