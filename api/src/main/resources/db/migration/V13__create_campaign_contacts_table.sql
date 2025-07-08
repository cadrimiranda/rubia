CREATE TABLE campaign_contacts (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    contact_status CampaignContactStatus NOT NULL DEFAULT 'PENDING',
    message_sent_at TIMESTAMP WITHOUT TIME ZONE,
    response_received_at TIMESTAMP WITHOUT TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_campaign
        FOREIGN KEY(campaign_id)
        REFERENCES campaigns(id),
    CONSTRAINT fk_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id)
);
CREATE TRIGGER update_campaign_contacts_updated_at BEFORE UPDATE ON campaign_contacts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
