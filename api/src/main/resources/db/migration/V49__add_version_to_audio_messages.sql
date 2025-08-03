-- Add version column for optimistic locking to audio_messages table
ALTER TABLE audio_messages ADD COLUMN version BIGINT DEFAULT 0;

-- Create index for version column (optional, for performance)
CREATE INDEX idx_audio_messages_version ON audio_messages(version);