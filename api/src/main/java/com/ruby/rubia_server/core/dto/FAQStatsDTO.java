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
public class FAQStatsDTO {
    
    private UUID companyId;
    private Long totalFAQs;
    private Long activeFAQs;
    private Long inactiveFAQs;
    private Double averageSuccessRate;
    private Long totalUsageCount;
    private FAQDTO mostUsedFAQ;
    private FAQDTO topPerformingFAQ;
}