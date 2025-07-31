package com.ruby.rubia_server.core.enums;

public enum MessagingProvider {
    Z_API("Z-API"),
    TWILIO("Twilio"),
    WHATSAPP_BUSINESS_API("WhatsApp Business API"),
    MOCK("Mock Provider");

    private final String displayName;

    MessagingProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}