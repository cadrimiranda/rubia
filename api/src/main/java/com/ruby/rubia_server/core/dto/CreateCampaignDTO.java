package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateCampaignDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Status is required")
    private CampaignStatus status;

    private UUID createdByUserId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Size(max = 1000, message = "Target audience description must not exceed 1000 characters")
    private String targetAudienceDescription;

    private UUID initialMessageTemplateId;

    private Integer totalContacts;

    @Size(max = 255, message = "Source system name must not exceed 255 characters")
    private String sourceSystemName;

    @Size(max = 255, message = "Source system ID must not exceed 255 characters")
    private String sourceSystemId;
}