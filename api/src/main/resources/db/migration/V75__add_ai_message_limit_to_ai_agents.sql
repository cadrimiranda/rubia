-- V75__add_ai_message_limit_to_ai_agents.sql
-- Move o campo ai_message_limit de conversations para ai_agents

-- Adicionar campo ai_message_limit na tabela ai_agents
ALTER TABLE ai_agents
ADD COLUMN ai_message_limit INTEGER NOT NULL DEFAULT 50;

-- Comentário explicativo
COMMENT ON COLUMN ai_agents.ai_message_limit IS 'Limite padrão de mensagens que este agente AI pode responder por conversa';

-- Remover campo ai_message_limit da tabela conversations (movido para ai_agents)
ALTER TABLE conversations
DROP COLUMN ai_message_limit;