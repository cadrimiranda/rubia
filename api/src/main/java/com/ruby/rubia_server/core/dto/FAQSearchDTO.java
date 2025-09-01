package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQSearchDTO {
    
    private UUID companyId;
    private String searchTerm;
    private Boolean isActive;
    private Integer limit;
    
    @Builder.Default
    private Integer offset = 0;
    
    // For AI search functionality
    private String userMessage;
    private Double minConfidenceScore;
}