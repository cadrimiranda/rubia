-- Migration para tabela de mapeamento chatLid <-> conversa
-- Resolve problema de conversas de campanha sem chatLid inicial

CREATE TABLE chat_lid_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_lid VARCHAR(100) UNIQUE,
    conversation_id UUID NOT NULL,
    phone VARCHAR(20) NOT NULL,
    company_id UUID NOT NULL,
    whatsapp_instance_id UUID,
    from_campaign BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_chat_lid_mappings_chat_lid ON chat_lid_mappings (chat_lid);
CREATE INDEX idx_chat_lid_mappings_conversation_phone ON chat_lid_mappings (conversation_id, phone);
CREATE INDEX idx_chat_lid_mappings_company_phone ON chat_lid_mappings (company_id, phone);
CREATE INDEX idx_chat_lid_mappings_instance ON chat_lid_mappings (whatsapp_instance_id);
CREATE INDEX idx_chat_lid_mappings_from_campaign ON chat_lid_mappings (from_campaign, company_id);
CREATE INDEX idx_chat_lid_mappings_created_at ON chat_lid_mappings (created_at);

-- Constraints de integridade referencial
ALTER TABLE chat_lid_mappings 
ADD CONSTRAINT fk_chat_lid_mappings_conversation 
FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;

ALTER TABLE chat_lid_mappings 
ADD CONSTRAINT fk_chat_lid_mappings_company 
FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Trigger para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_chat_lid_mappings_updated_at 
    BEFORE UPDATE ON chat_lid_mappings 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comentários para documentação
COMMENT ON TABLE chat_lid_mappings IS 'Mapeamento entre chatLid do WhatsApp e conversas internas - resolve problema de campanhas sem chatLid inicial';
COMMENT ON COLUMN chat_lid_mappings.chat_lid IS 'ChatLid recebido do WhatsApp (ex: 269161355821173@lid) - pode ser NULL para campanhas';
COMMENT ON COLUMN chat_lid_mappings.conversation_id IS 'ID da conversa interna associada';
COMMENT ON COLUMN chat_lid_mappings.phone IS 'Telefone do cliente para busca rápida';
COMMENT ON COLUMN chat_lid_mappings.company_id IS 'ID da empresa para isolamento multi-tenant';
COMMENT ON COLUMN chat_lid_mappings.whatsapp_instance_id IS 'Instância WhatsApp que criou o mapping';
COMMENT ON COLUMN chat_lid_mappings.from_campaign IS 'Indica se mapping foi criado por campanha (inicialmente sem chatLid)';