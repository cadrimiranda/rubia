-- Add version column for optimistic locking to audio_messages table
ALTER TABLE audio_messages ADD COLUMN version BIGINT DEFAULT 0;