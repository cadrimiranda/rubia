package com.ruby.rubia_server.core.enums;

public enum CampaignStatus {
    DRAFT,      // Rascunho, ainda em configuração
    ACTIVE,     // Em execução, enviando mensagens
    PAUSED,     // Pausada temporariamente
    COMPLETED,  // Concluída (todas as mensagens enviadas ou data final atingida)
    CANCELED    // Cancelada antes de concluir
}