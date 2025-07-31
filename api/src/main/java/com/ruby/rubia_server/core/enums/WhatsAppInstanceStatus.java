package com.ruby.rubia_server.core.enums;

public enum WhatsAppInstanceStatus {
    NOT_CONFIGURED("Não Configurado"),
    CONFIGURING("Configurando"),
    AWAITING_QR_SCAN("Aguardando Escaneamento QR"),
    CONNECTING("Conectando"),
    CONNECTED("Conectado"),
    DISCONNECTED("Desconectado"),
    ERROR("Erro"),
    SUSPENDED("Suspenso");

    private final String displayName;

    WhatsAppInstanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}