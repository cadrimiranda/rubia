-- V47: Adicionar campos de metadados de IA para MessageTemplateRevision
-- Criado para rastrear melhorias de templates feitas por IA

-- Adicionar campos para metadados de IA
ALTER TABLE message_template_revisions 
ADD COLUMN ai_agent_id UUID NULL,
ADD COLUMN ai_enhancement_type VARCHAR(50) NULL,
ADD COLUMN ai_tokens_used INTEGER NULL,
ADD COLUMN ai_credits_consumed INTEGER NULL,
ADD COLUMN ai_model_used VARCHAR(100) NULL,
ADD COLUMN ai_explanation TEXT NULL;

-- Adicionar foreign key para ai_agent
ALTER TABLE message_template_revisions 
ADD CONSTRAINT fk_message_template_revision_ai_agent 
FOREIGN KEY (ai_agent_id) REFERENCES ai_agents(id);

-- Adicionar índices para melhor performance
CREATE INDEX idx_message_template_revisions_ai_agent_id ON message_template_revisions(ai_agent_id);
CREATE INDEX idx_message_template_revisions_ai_enhancement_type ON message_template_revisions(ai_enhancement_type);

-- Comentários para documentação
COMMENT ON COLUMN message_template_revisions.ai_agent_id IS 'Agente de IA usado para esta melhoria (se aplicável)';
COMMENT ON COLUMN message_template_revisions.ai_enhancement_type IS 'Tipo de melhoria aplicada pela IA (friendly, professional, empathetic, urgent, motivational)';
COMMENT ON COLUMN message_template_revisions.ai_tokens_used IS 'Número de tokens consumidos pela IA para esta melhoria';
COMMENT ON COLUMN message_template_revisions.ai_credits_consumed IS 'Créditos consumidos pela IA para esta melhoria';
COMMENT ON COLUMN message_template_revisions.ai_model_used IS 'Nome do modelo de IA usado (ex: GPT-4, GPT-4 Mini)';
COMMENT ON COLUMN message_template_revisions.ai_explanation IS 'Explicação das melhorias aplicadas pela IA';