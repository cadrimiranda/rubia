-- Migration para migrar conversas existentes com chatLid para a tabela de mapping
-- Garante que conversas já existentes continuem funcionando após implementação

-- Inserir mappings para conversas existentes que possuem chatLid
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
    (
        -- Buscar instância ativa da empresa
        SELECT wi.id 
        FROM whats_app_instances wi 
        WHERE wi.company_id = cu.company_id 
          AND wi.is_active = true 
        LIMIT 1
    ) as whatsapp_instance_id,
    false as from_campaign, -- Conversas existentes não são de campanha
    c.created_at,
    c.created_at as updated_at
FROM conversations c
INNER JOIN customers cu ON c.customer_id = cu.id
WHERE c.chat_lid IS NOT NULL
  AND c.chat_lid != ''
  AND c.channel = 'WHATSAPP'
  AND NOT EXISTS (
      -- Evitar duplicatas caso script rode múltiplas vezes
      SELECT 1 FROM chat_lid_mappings clm 
      WHERE clm.chat_lid = c.chat_lid
  );

-- Log do que foi migrado
DO $$
DECLARE
    migrated_count integer;
BEGIN
    SELECT count(*) INTO migrated_count
    FROM chat_lid_mappings 
    WHERE from_campaign = false;
    
    RAISE NOTICE 'Migrados % mappings de conversas existentes', migrated_count;
END $$;

-- Identificar conversas de campanhas (podem não ter chatLid ainda)
-- Criar mappings parciais para facilitar futuras atualizações
INSERT INTO chat_lid_mappings (
    conversation_id,
    phone,
    company_id,
    whatsapp_instance_id,
    from_campaign,
    created_at,
    updated_at
)
SELECT DISTINCT
    c.id as conversation_id,
    cu.phone,
    cu.company_id,
    (
        -- Buscar instância ativa da empresa  
        SELECT wi.id 
        FROM whats_app_instances wi 
        WHERE wi.company_id = cu.company_id 
          AND wi.is_active = true 
        LIMIT 1
    ) as whatsapp_instance_id,
    true as from_campaign,
    c.created_at,
    c.created_at as updated_at
FROM conversations c
INNER JOIN customers cu ON c.customer_id = cu.id
INNER JOIN messages m ON m.conversation_id = c.id
WHERE c.channel = 'WHATSAPP'
  AND c.created_at >= CURRENT_DATE - INTERVAL '30 days' -- Apenas conversas recentes
  AND (c.chat_lid IS NULL OR c.chat_lid = '') -- Conversas sem chatLid (provavelmente campanhas)
  AND m.sender_type = 'AGENT' -- Tem mensagem enviada por agente (indica campanha)
  AND NOT EXISTS (
      -- Evitar duplicatas
      SELECT 1 FROM chat_lid_mappings clm 
      WHERE clm.conversation_id = c.id
  )
-- Limitar para evitar overhead, processar apenas mais relevantes
ORDER BY c.created_at DESC
LIMIT 1000;

-- Log das campanhas identificadas
DO $$
DECLARE
    campaign_count integer;
BEGIN
    SELECT count(*) INTO campaign_count
    FROM chat_lid_mappings 
    WHERE from_campaign = true;
    
    RAISE NOTICE 'Identificadas % conversas potenciais de campanha', campaign_count;
END $$;

-- Criar índices específicos para performance pós-migração
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_conversations_chat_lid_lookup 
ON conversations (chat_lid) 
WHERE chat_lid IS NOT NULL AND chat_lid != '';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_conversations_recent_whatsapp 
ON conversations (company_id, customer_id, created_at DESC) 
WHERE channel = 'WHATSAPP' 
  AND created_at >= CURRENT_DATE - INTERVAL '90 days';

-- Comentários sobre a migração
COMMENT ON TABLE chat_lid_mappings IS 'Mapping table populated from existing conversations during V1002 migration. Handles both regular conversations and campaign conversations.';

-- Análise final
DO $$
DECLARE
    total_mappings integer;
    regular_mappings integer;
    campaign_mappings integer;
BEGIN
    SELECT count(*) INTO total_mappings FROM chat_lid_mappings;
    SELECT count(*) INTO regular_mappings FROM chat_lid_mappings WHERE from_campaign = false;
    SELECT count(*) INTO campaign_mappings FROM chat_lid_mappings WHERE from_campaign = true;
    
    RAISE NOTICE '=== MIGRAÇÃO CONCLUÍDA ===';
    RAISE NOTICE 'Total de mappings criados: %', total_mappings;
    RAISE NOTICE 'Conversas regulares: %', regular_mappings;
    RAISE NOTICE 'Conversas de campanha: %', campaign_mappings;
    RAISE NOTICE '==============================';
END $$;