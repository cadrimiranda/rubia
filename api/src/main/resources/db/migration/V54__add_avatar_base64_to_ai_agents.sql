-- Adicionar campo avatar_base64 para armazenar imagem em base64
-- e remover campo avatar_url obsoleto

-- Adicionar novo campo para base64
ALTER TABLE ai_agents ADD COLUMN avatar_base64 TEXT;

-- Comentário para documentação
COMMENT ON COLUMN ai_agents.avatar_base64 IS 'Avatar do agente em formato base64 (data:image/jpeg;base64,/9j/4AAQ...). Limite recomendado: 1-2MB para performance.';

-- Remover campo obsoleto avatar_url após migração
-- ALTER TABLE ai_agents DROP COLUMN avatar_url; -- Comentado para safety, ativar após verificar que tudo funciona