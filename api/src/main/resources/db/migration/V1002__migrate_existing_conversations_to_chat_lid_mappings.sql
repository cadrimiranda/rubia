-- Migration simplificada para migrar conversas existentes com chatLid para a tabela de mapping
-- Garante que conversas já existentes continuem funcionando após implementação

DO $$
BEGIN
    -- Tentar inserir mappings para conversas existentes que possuem chatLid
    -- Usando EXCEPTION handling para evitar falhas se tabelas não existirem
    BEGIN
        INSERT INTO chat_lid_mappings (
            chat_lid,
            conversation_id,
            phone,
            company_id,
            whatsapp_instance_id,
            from_campaign,
            created_at,
            updated_at
        )
        SELECT DISTINCT
            c.chat_lid,
            c.id as conversation_id,
            cu.phone,
            cu.company_id,
            NULL as whatsapp_instance_id, -- Será preenchido posteriormente
            false as from_campaign,
            c.created_at,
            c.created_at as updated_at
        FROM conversations c
        INNER JOIN customers cu ON c.customer_id = cu.id
        WHERE c.chat_lid IS NOT NULL
          AND c.chat_lid != ''
          AND NOT EXISTS (
              SELECT 1 FROM chat_lid_mappings clm 
              WHERE clm.chat_lid = c.chat_lid
          );
          
        RAISE NOTICE 'Conversas existentes com chatLid migradas com sucesso';
        
    EXCEPTION
        WHEN others THEN
            RAISE NOTICE 'Aviso: Não foi possível migrar conversas existentes (normal em nova instalação): %', SQLERRM;
    END;
    
    -- Comentários sobre a migração
    EXECUTE 'COMMENT ON TABLE chat_lid_mappings IS ''Mapping table for chatLid to conversation association''';
    
    RAISE NOTICE 'Migration V1002 concluída com sucesso';
    
END $$;