package com.ruby.rubia_server.core.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event emitted when a campaign contact needs to be retried
 */
public class CampaignRetryEvent extends ApplicationEvent {

    private final UUID campaignId;
    private final UUID contactId;
    private final String companyId;

    public CampaignRetryEvent(Object source, UUID campaignId, UUID contactId, String companyId) {
        super(source);
        this.campaignId = campaignId;
        this.contactId = contactId;
        this.companyId = companyId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public String getCompanyId() {
        return companyId;
    }
}