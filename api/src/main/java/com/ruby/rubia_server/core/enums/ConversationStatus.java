package com.ruby.rubia_server.core.enums;

/**
 * Status da conversa - mapeado como ordinal para PostgreSQL
 * ENTRADA = 0, ESPERANDO = 1, FINALIZADOS = 2
 */
public enum ConversationStatus {
    ENTRADA("Entrada"),     // 0 - Nova conversa
    ESPERANDO("Esperando"), // 1 - Aguardando atendimento
    FINALIZADOS("Finalizados"); // 2 - Conversa finalizada
    
    private final String displayName;
    
    ConversationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}