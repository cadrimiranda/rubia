package com.ruby.rubia_server.core.enums;

public enum MessageStatus {
    DRAFT("Rascunho"),
    SENDING("Enviando"),
    SENT("Enviado"),
    DELIVERED("Entregue"),
    READ("Lido"),
    FAILED("Falhou");
    
    private final String displayName;
    
    MessageStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}