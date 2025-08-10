-- Adicionar coluna campaign_id para relacionar mappings com campanhas
-- Permite identificar qual campanha originou cada mapping para melhor rastreamento

ALTER TABLE chat_lid_mappings 
ADD COLUMN campaign_id UUID;

-- Criar índice para performance em consultas por campanha
CREATE INDEX idx_chat_lid_mappings_campaign_id ON chat_lid_mappings (campaign_id);

-- Criar índice composto para buscar campanhas ativas por telefone
CREATE INDEX idx_chat_lid_mappings_campaign_phone_created ON chat_lid_mappings (campaign_id, phone, created_at DESC);

-- Adicionar constraint de integridade referencial
ALTER TABLE chat_lid_mappings 
ADD CONSTRAINT fk_chat_lid_mappings_campaign 
FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL;

-- Comentário para documentação
COMMENT ON COLUMN chat_lid_mappings.campaign_id IS 'ID da campanha que originou este mapping - permite rastrear e priorizar conversas de campanha ativas';