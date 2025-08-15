-- V74__add_ai_message_limit_fields_to_conversations.sql
-- Adiciona campos para controle de limite de mensagens AI por conversa

ALTER TABLE conversations
ADD COLUMN ai_message_limit INTEGER NOT NULL DEFAULT 50,
ADD COLUMN ai_messages_used INTEGER NOT NULL DEFAULT 0,
ADD COLUMN ai_limit_reached_at TIMESTAMP;

-- Criar índice para performance em consultas de limite
CREATE INDEX idx_conversations_ai_limit ON conversations(ai_messages_used, ai_message_limit);

-- Comentários explicativos
COMMENT ON COLUMN conversations.ai_message_limit IS 'Limite máximo de mensagens que a IA pode responder para esta conversa';
COMMENT ON COLUMN conversations.ai_messages_used IS 'Quantidade de mensagens já utilizadas pela IA nesta conversa';
COMMENT ON COLUMN conversations.ai_limit_reached_at IS 'Timestamp de quando o limite foi atingido (null se não atingido)';