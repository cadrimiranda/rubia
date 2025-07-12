package com.ruby.rubia_server.core.enums;

/**
 * Tipo de conversa - mapeado como ordinal para PostgreSQL
 * ONE_TO_ONE = 0, GROUP_CHAT = 1
 */
public enum ConversationType {
    ONE_TO_ONE,  // 0 - Conversa individual entre um Customer e a Empresa/Agente
    GROUP_CHAT   // 1 - Conversa em grupo com múltiplos Customers e/ou Agentes
}