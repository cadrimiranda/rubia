-- Add default UUID generation to conversation_media table
ALTER TABLE conversation_media ALTER COLUMN id SET DEFAULT gen_random_uuid();