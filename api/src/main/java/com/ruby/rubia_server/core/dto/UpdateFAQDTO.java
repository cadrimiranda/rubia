package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFAQDTO {
    
    @Size(max = 500, message = "Question must not exceed 500 characters")
    private String question;
    
    private String answer;
    
    private List<String> keywords;
    
    private List<String> triggers;
    
    private Boolean isActive;
}