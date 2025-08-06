package com.ruby.rubia_server.core.enums;

/**
 * Enum para representar os tipos de revisão de template
 */
public enum RevisionType {
    /**
     * Revisão criada quando o template é criado pela primeira vez
     */
    CREATE,
    
    /**
     * Revisão criada quando o template é editado
     */
    EDIT,
    
    /**
     * Revisão criada quando o template é excluído (soft delete)
     */
    DELETE,
    
    /**
     * Revisão criada quando o template é restaurado
     */
    RESTORE,
    
    /**
     * Revisão criada quando o template é melhorado por IA
     */
    AI_ENHANCEMENT
}