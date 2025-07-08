package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.AILogStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAILogDTO {

    @Size(max = 10000, message = "Raw response must not exceed 10000 characters")
    private String rawResponse;

    @Size(max = 10000, message = "Processed response must not exceed 10000 characters")
    private String processedResponse;

    private Integer tokensUsedOutput;

    private BigDecimal estimatedCost;

    private AILogStatus status;

    @Size(max = 1000, message = "Error message must not exceed 1000 characters")
    private String errorMessage;
}