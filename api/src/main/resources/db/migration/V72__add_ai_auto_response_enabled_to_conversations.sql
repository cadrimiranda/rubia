-- Adiciona campo para controlar respostas automáticas da IA por conversa
ALTER TABLE conversations
ADD COLUMN ai_auto_response_enabled BOOLEAN NOT NULL DEFAULT true;

-- Comentário para documentação
COMMENT ON COLUMN conversations.ai_auto_response_enabled 
IS 'Controla se respostas automáticas da IA estão habilitadas para esta conversa';