package com.ruby.rubia_server.core.dto;

import com.ruby.rubia_server.core.entity.DraftStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftReviewDTO {
    
    @NotNull(message = "Ação é obrigatória")
    private DraftStatus action; // APPROVED, REJECTED, EDITED
    
    private String editedContent; // Novo conteúdo se action = EDITED
    
    private String rejectionReason; // Motivo se action = REJECTED
    
    private Boolean sendImmediately = true; // Se deve enviar imediatamente após aprovação
}