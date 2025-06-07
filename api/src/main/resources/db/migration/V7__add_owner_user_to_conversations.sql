-- Add owner_user_id to conversations table for multi-client support
ALTER TABLE conversations 
ADD COLUMN owner_user_id UUID,
ADD CONSTRAINT fk_conversations_owner_user 
    FOREIGN KEY (owner_user_id) REFERENCES users(id);