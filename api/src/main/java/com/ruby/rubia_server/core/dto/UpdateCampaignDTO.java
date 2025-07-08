package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CampaignStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignDTO {

    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private CampaignStatus status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Size(max = 1000, message = "Target audience description must not exceed 1000 characters")
    private String targetAudienceDescription;

    private Integer totalContacts;

    private Integer contactsReached;

    @Size(max = 255, message = "Source system name must not exceed 255 characters")
    private String sourceSystemName;

    @Size(max = 255, message = "Source system ID must not exceed 255 characters")
    private String sourceSystemId;
}