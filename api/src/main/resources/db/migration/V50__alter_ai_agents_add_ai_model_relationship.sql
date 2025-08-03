-- Adicionar coluna ai_model_id na tabela ai_agents
ALTER TABLE ai_agents ADD COLUMN ai_model_id UUID;

-- Migrar dados existentes: buscar o modelo correspondente baseado no ai_model_type
UPDATE ai_agents 
SET ai_model_id = (
    SELECT id FROM ai_models 
    WHERE name = CASE 
        WHEN ai_agents.ai_model_type = 'GPT-4' THEN 'gpt-4.1'
        WHEN ai_agents.ai_model_type = 'GPT-4.1' THEN 'gpt-4.1'
        WHEN ai_agents.ai_model_type = 'GPT-4 Mini' THEN 'gpt-4o-mini'
        WHEN ai_agents.ai_model_type = 'gpt-4o-mini' THEN 'gpt-4o-mini'
        WHEN ai_agents.ai_model_type = 'O3' THEN 'o3'
        ELSE 'gpt-4.1' -- default para casos não mapeados
    END
    LIMIT 1
);

-- Tornar ai_model_id obrigatório e adicionar foreign key
ALTER TABLE ai_agents ALTER COLUMN ai_model_id SET NOT NULL;
ALTER TABLE ai_agents ADD CONSTRAINT fk_ai_agents_ai_model 
    FOREIGN KEY (ai_model_id) REFERENCES ai_models(id);

-- Remover a coluna ai_model_type que agora será substituída pelo relacionamento
ALTER TABLE ai_agents DROP COLUMN ai_model_type;

-- Adicionar índice para performance
CREATE INDEX idx_ai_agents_ai_model_id ON ai_agents(ai_model_id);