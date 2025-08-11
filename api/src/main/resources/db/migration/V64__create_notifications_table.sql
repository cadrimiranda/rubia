-- Create notification enums
CREATE TYPE notification_type AS ENUM ('NEW_MESSAGE', 'MESSAGE_REPLY', 'CONVERSATION_ASSIGNED', 'CONVERSATION_STATUS_CHANGED', 'CAMPAIGN_MESSAGE', 'SYSTEM_ALERT');
CREATE TYPE notification_status AS ENUM ('UNREAD', 'READ', 'DISMISSED');

-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type notification_type NOT NULL,
    status notification_status NOT NULL DEFAULT 'UNREAD',
    title VARCHAR(255) NOT NULL,
    content TEXT,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_conversation_id ON notifications(conversation_id);
CREATE INDEX idx_notifications_message_id ON notifications(message_id);
CREATE INDEX idx_notifications_company_id ON notifications(company_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_read_at ON notifications(read_at);
CREATE INDEX idx_notifications_deleted_at ON notifications(deleted_at);

-- Composite indexes for common queries
CREATE INDEX idx_notifications_user_status_deleted ON notifications(user_id, status, deleted_at);
CREATE INDEX idx_notifications_user_conversation_status ON notifications(user_id, conversation_id, status);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, status) WHERE status = 'UNREAD' AND deleted_at IS NULL;

-- Add unique constraint to prevent duplicate notifications for same message and user
CREATE UNIQUE INDEX idx_notifications_unique_user_message ON notifications(user_id, message_id) WHERE deleted_at IS NULL;

-- Add trigger for updated_at
CREATE TRIGGER trigger_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();