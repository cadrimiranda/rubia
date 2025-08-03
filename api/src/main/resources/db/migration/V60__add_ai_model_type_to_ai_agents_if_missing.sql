-- Add ai_model_type column to ai_agents table if it doesn't exist
-- This is a safety migration in case the column was missed during initial creation

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'ai_agents' 
        AND column_name = 'ai_model_type'
    ) THEN
        ALTER TABLE ai_agents ADD COLUMN ai_model_type VARCHAR(255) NOT NULL DEFAULT 'GPT-4';
    END IF;
END $$;