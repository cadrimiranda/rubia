package com.ruby.rubia_server.core.enums;

public enum ConversationChannel {
    WHATSAPP("WhatsApp"),
    WEB("Web"),
    TELEGRAM("Telegram"),
    EMAIL("Email");
    
    private final String displayName;
    
    ConversationChannel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}