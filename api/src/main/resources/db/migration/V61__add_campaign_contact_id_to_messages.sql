-- Migration: Add campaign_contact_id column to messages table
-- Description: Adds a foreign key relationship between messages and campaign_contacts
-- for optimized campaign message tracking and synchronization

-- Add the campaign_contact_id column as nullable initially
ALTER TABLE messages 
ADD COLUMN campaign_contact_id UUID;

-- Add foreign key constraint referencing campaign_contacts table
ALTER TABLE messages 
ADD CONSTRAINT fk_messages_campaign_contact_id 
FOREIGN KEY (campaign_contact_id) 
REFERENCES campaign_contacts(id) 
ON DELETE SET NULL;

-- Add index for performance optimization on campaign contact queries
CREATE INDEX idx_messages_campaign_contact_id_status 
ON messages (campaign_contact_id, status);

-- Add comment for documentation
COMMENT ON COLUMN messages.campaign_contact_id IS 'Optional reference to campaign_contacts table for campaign-related messages. Used for direct tracking and synchronization of campaign message status.';