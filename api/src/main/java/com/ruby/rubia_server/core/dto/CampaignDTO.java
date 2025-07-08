package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private String name;
    private String description;
    private CampaignStatus status;
    private UUID createdByUserId;
    private String createdByUserName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String targetAudienceDescription;
    private UUID initialMessageTemplateId;
    private String initialMessageTemplateName;
    private Integer totalContacts;
    private Integer contactsReached;
    private String sourceSystemName;
    private String sourceSystemId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}