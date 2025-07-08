package com.ruby.rubia_server.core.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationParticipantDTO {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    private UUID customerId;

    private UUID userId;

    private UUID aiAgentId;

    private Boolean isActive;

    @AssertTrue(message = "Exactly one participant (customer, user, or AI agent) must be provided")
    public boolean isValidParticipant() {
        int participantCount = 0;
        if (customerId != null) participantCount++;
        if (userId != null) participantCount++;
        if (aiAgentId != null) participantCount++;
        return participantCount == 1;
    }
}