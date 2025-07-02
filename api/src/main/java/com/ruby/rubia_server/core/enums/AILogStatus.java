package com.ruby.rubia_server.core.enums;

public enum AILogStatus {
    SUCCESS,  // Interação da IA bem-sucedida
    FAILED,   // Interação da IA falhou (ex: erro na API externa)
    PARTIAL,  // Resposta parcial (ex: limite de tokens atingido, truncado)
    TIMEOUT   // A interação excedeu o tempo limite
}