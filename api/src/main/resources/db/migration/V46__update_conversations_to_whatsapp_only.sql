-- Migration to convert all conversations to WhatsApp channel only
-- This migration updates existing conversations from other channels to WhatsApp
-- and removes support for multiple channels since this is a WhatsApp-only system

-- Update all non-WhatsApp conversations to WhatsApp (channel = 0)
UPDATE conversations 
SET channel = 0 
WHERE channel != 0;

-- Remove old constraint that allowed multiple channel types
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS chk_channel;

-- Add new constraint that only allows WhatsApp (channel = 0)
ALTER TABLE conversations ADD CONSTRAINT chk_channel CHECK (channel = 0);

-- Add comment explaining the change
COMMENT ON COLUMN conversations.channel IS 'Communication channel - WhatsApp only (0=WHATSAPP)';