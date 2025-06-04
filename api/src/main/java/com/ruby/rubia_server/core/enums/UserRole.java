package com.ruby.rubia_server.core.enums;

public enum UserRole {
    ADMIN("Administrador"),
    SUPERVISOR("Supervisor"),
    AGENT("Agente");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}