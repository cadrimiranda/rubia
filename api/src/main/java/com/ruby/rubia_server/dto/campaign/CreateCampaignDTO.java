package com.ruby.rubia_server.dto.campaign;

import com.ruby.rubia_server.core.enums.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignDTO {
    
    @NotNull(message = "Company ID is required")
    private UUID companyId;
    
    @NotNull(message = "User ID is required")
    private UUID createdByUserId;
    
    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    @Size(max = 100, message = "Source system name must not exceed 100 characters")
    private String sourceSystemName;
    
    @Size(max = 100, message = "Source system ID must not exceed 100 characters")
    private String sourceSystemId;
    
    private UUID initialMessageTemplateId;
    
    private String targetAudienceDescription;
    
    @Builder.Default
    private Integer totalContacts = 0;
}