package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.enums.AILogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AILogDTO {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID aiAgentId;
    private String aiAgentName;
    private UUID userId;
    private String userName;
    private UUID conversationId;
    private UUID messageId;
    private UUID messageTemplateId;
    private String messageTemplateName;
    private String requestPrompt;
    private String rawResponse;
    private String processedResponse;
    private Integer tokensUsedInput;
    private Integer tokensUsedOutput;
    private BigDecimal estimatedCost;
    private AILogStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
}