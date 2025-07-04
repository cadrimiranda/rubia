CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    campaign_status CampaignStatus NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID,
    start_date TIMESTAMP WITHOUT TIME ZONE,
    end_date TIMESTAMP WITHOUT TIME ZONE,
    target_audience_description TEXT,
    message_template_id UUID,
    total_contacts INT DEFAULT 0,
    contacts_reached INT DEFAULT 0,
    source_system_name VARCHAR(255),
    source_system_id VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_company
        FOREIGN KEY(company_id)
        REFERENCES companies(id),
    CONSTRAINT fk_created_by_user
        FOREIGN KEY(created_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_message_template
        FOREIGN KEY(message_template_id)
        REFERENCES message_templates(id)
);
CREATE TRIGGER update_campaigns_updated_at BEFORE UPDATE ON campaigns FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
