package com.ruby.rubia_server.core.enums;

public enum CampaignContactStatus {
    PENDING,    // Aguardando envio da mensagem
    SENT,       // Mensagem enviada
    FAILED,     // Falha no envio da mensagem
    RESPONDED,  // Contato respondeu à campanha
    CONVERTED,  // Contato se tornou um cliente ou atingiu o objetivo da campanha (necessita lógica de negócio)
    OPT_OUT     // Contato pediu para não receber mais mensagens da campanha
}