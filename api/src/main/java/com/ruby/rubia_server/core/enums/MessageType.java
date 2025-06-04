package com.ruby.rubia_server.core.enums;

public enum MessageType {
    TEXT("Texto"),
    IMAGE("Imagem"),
    AUDIO("Áudio"),
    FILE("Arquivo"),
    LOCATION("Localização"),
    CONTACT("Contato");
    
    private final String displayName;
    
    MessageType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}