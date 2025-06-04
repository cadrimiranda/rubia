package com.ruby.rubia_server.core.enums;

public enum ConversationStatus {
    ENTRADA("Entrada"),
    ESPERANDO("Esperando"),
    FINALIZADOS("Finalizados");
    
    private final String displayName;
    
    ConversationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}