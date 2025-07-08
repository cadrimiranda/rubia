package com.ruby.rubia_server.core.dto;

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
public class CreateUserAIAgentDTO {

    @NotNull(message = "Company ID é obrigatório")
    private UUID companyId;

    @NotNull(message = "User ID é obrigatório")
    private UUID userId;

    @NotNull(message = "AI Agent ID é obrigatório")
    private UUID aiAgentId;

    @Builder.Default
    private Boolean isDefault = false;
}