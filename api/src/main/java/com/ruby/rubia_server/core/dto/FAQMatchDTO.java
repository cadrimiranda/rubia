package com.ruby.rubia_server.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQMatchDTO {
    
    private FAQDTO faq;
    private Double confidenceScore;
    private String matchReason; // "exact_keyword", "partial_match", "trigger_match", etc.
    private String matchedText; // The specific text that caused the match
}