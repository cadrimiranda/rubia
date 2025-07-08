package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.CompanyPlanType;
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
public class CompanyDTO {
    
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String logoUrl;
    private Boolean isActive;
    private CompanyPlanType planType;
    private Integer maxUsers;
    private Integer maxWhatsappNumbers;
    private UUID companyGroupId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}