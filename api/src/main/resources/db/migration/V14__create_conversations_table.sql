CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    channel Channel NOT NULL,
    status ConversationStatus NOT NULL,
    priority INT,
    assigned_user_id UUID,
    owner_user_id UUID,
    campaign_id UUID,
    conversation_type ConversationType NOT NULL DEFAULT 'ONE_TO_ONE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_assigned_user
        FOREIGN KEY(assigned_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_owner_user
        FOREIGN KEY(owner_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_campaign
        FOREIGN KEY(campaign_id)
        REFERENCES campaigns(id)
);
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
