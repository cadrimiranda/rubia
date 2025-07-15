package com.ruby.rubia_server.dto.campaign;

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
    
    private String name;
    
    private String description;
    
    private CampaignStatus status;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private Integer totalContacts;
    
    private Integer contactsReached;
    
    private String sourceSystemName;
    
    private String sourceSystemId;
    
    private UUID companyId;
    
    private String createdBy;
    
    private UUID initialMessageTemplateId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}