package com.ruby.rubia_server.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessCampaignDTO {
    
    @NotNull(message = "Company ID is required")
    private UUID companyId;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Campaign name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    @Size(max = 100, message = "Source system name must not exceed 100 characters")
    private String sourceSystem;
    
    @NotEmpty(message = "At least one template must be selected")
    private List<UUID> templateIds;
}