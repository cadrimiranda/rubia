package com.ruby.rubia_server.core.entity;

public enum DraftStatus {
    PENDING,    // Aguardando revisão do operador
    APPROVED,   // Aprovado e enviado como mensagem real
    REJECTED,   // Rejeitado pelo operador
    EDITED      // Editado pelo operador antes do envio
}