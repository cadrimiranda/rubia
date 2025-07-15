-- Increase media_type column length to avoid potential constraint issues
ALTER TABLE conversation_media ALTER COLUMN media_type TYPE VARCHAR(50);