-- Adicionar campo de limite de agentes IA por empresa
-- e configurar limites baseados no plano da empresa

-- Adicionar coluna max_ai_agents
ALTER TABLE companies ADD COLUMN max_ai_agents INTEGER DEFAULT 1;

-- Configurar limites baseados no plano da empresa:
-- BASIC (0): 1 agente IA
-- PREMIUM (1): 3 agentes IA  
-- ENTERPRISE (2): 10 agentes IA

UPDATE companies SET max_ai_agents = 1 WHERE plan_type = 0; -- BASIC
UPDATE companies SET max_ai_agents = 3 WHERE plan_type = 1; -- PREMIUM  
UPDATE companies SET max_ai_agents = 10 WHERE plan_type = 2; -- ENTERPRISE

-- Garantir que NOT NULL após popular dados
ALTER TABLE companies ALTER COLUMN max_ai_agents SET NOT NULL;

-- Adicionar comentário para documentação
COMMENT ON COLUMN companies.max_ai_agents IS 'Limite máximo de agentes IA que a empresa pode criar baseado no plano: BASIC=1, PREMIUM=3, ENTERPRISE=10';