package com.ruby.rubia_server.core.enums;

public enum SenderType {
    CUSTOMER("Cliente"),
    AGENT("Agente"),
    AI("IA"),
    SYSTEM("Sistema");
    
    private final String displayName;
    
    SenderType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}