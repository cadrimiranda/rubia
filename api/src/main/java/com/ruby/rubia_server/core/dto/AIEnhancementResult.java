package com.ruby.rubia_server.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para retornar o resultado do enhancement com metadados do payload enviado
 */
@Data
@Builder
public class AIEnhancementResult {
    
    private String enhancedMessage; // A mensagem melhorada
    
    // Mensagens enviadas para OpenAI (para auditoria)
    private String systemMessage;
    private String userMessage;
    
    // Payload completo como JSON (opcional)
    private String fullPayloadJson;
    
    // Metadados da resposta
    private Integer tokensUsed; // Se disponível da resposta
    private String modelUsed;
    private Double temperatureUsed;
    private Integer maxTokensUsed;
}