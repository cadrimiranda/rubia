package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.AILogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAILogDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotNull(message = "AI Agent ID is required")
    private UUID aiAgentId;

    private UUID userId;

    private UUID conversationId;

    private UUID messageId;

    private UUID messageTemplateId;

    @NotBlank(message = "Request prompt is required")
    @Size(max = 10000, message = "Request prompt must not exceed 10000 characters")
    private String requestPrompt;

    @Size(max = 10000, message = "Raw response must not exceed 10000 characters")
    private String rawResponse;

    @Size(max = 10000, message = "Processed response must not exceed 10000 characters")
    private String processedResponse;

    private Integer tokensUsedInput;

    private Integer tokensUsedOutput;

    private BigDecimal estimatedCost;

    @NotNull(message = "Status is required")
    private AILogStatus status;

    @Size(max = 1000, message = "Error message must not exceed 1000 characters")
    private String errorMessage;
}