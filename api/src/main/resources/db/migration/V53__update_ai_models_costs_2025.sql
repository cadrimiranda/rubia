-- Atualizar custos dos modelos de IA para valores de janeiro 2025 (preços OpenAI)
-- e otimizar configurações para templates de doação de sangue

-- GPT-4o mini: Modelo IDEAL para nosso caso (templates de marketing/doação)
UPDATE ai_models SET 
    capabilities = 'Modelo otimizado do GPT-4 com excelente custo-benefício. Ideal para templates de marketing, copy persuasivo e comunicação com doadores. Mantém alta qualidade com velocidade e economia exemplares.',
    impact_description = 'RECOMENDADO: Perfeito para otimização de templates de doação de sangue. Entende contexto médico, produz copy persuasivo, mantém tom empático e respeita placeholders. Custo 17x menor que GPT-4o premium.',
    cost_per_1k_tokens = 1, -- $0.15/1M input + $0.60/1M output ≈ $0.75/1M average = R$0.001/1k
    performance_level = 'HIGH',
    sort_order = 1 -- Prioridade máxima como padrão
WHERE name = 'gpt-4o-mini';

-- GPT-4o: Modelo premium para casos excepcionais
UPDATE ai_models SET 
    capabilities = 'Modelo GPT-4 premium com máxima qualidade. Raciocínio avançado, criatividade superior e compreensão contextual profunda. Para estratégias complexas e conteúdo altamente sofisticado.',
    impact_description = 'Para campanhas especiais, estratégias complexas de captação e conteúdo criativo avançado. Recomendado apenas para casos que exigem máxima sofisticação e orçamento premium.',
    cost_per_1k_tokens = 15, -- $2.50/1M input + $10.00/1M output ≈ $6.25/1M average = R$0.015/1k  
    performance_level = 'PREMIUM',
    sort_order = 2
WHERE name = 'gpt-4';

-- O3: Especializado em raciocínio científico (para casos médicos complexos)
UPDATE ai_models SET 
    capabilities = 'Modelo especializado em raciocínio científico, análise médica e lógica complexa. Excelente para questões técnicas de hematologia, compatibilidade sanguínea e protocolos médicos.',
    impact_description = 'Para consultas médicas especializadas, análise de dados hematológicos, protocolos de doação complexos e questões técnicas que exigem conhecimento científico profundo.',
    cost_per_1k_tokens = 25, -- Modelo premium científico
    performance_level = 'PREMIUM',
    sort_order = 3
WHERE name = 'o3';

-- GPT-3.5 Turbo: Modelo econômico básico (fallback)
INSERT INTO ai_models (name, display_name, provider, capabilities, impact_description, cost_per_1k_tokens, performance_level, is_active, sort_order)
VALUES (
    'gpt-3.5-turbo',
    'GPT-3.5 Turbo',
    'OpenAI',
    'Modelo econômico para tarefas básicas. Adequado para respostas simples, FAQ automatizado e casos que não exigem sofisticação. Velocidade alta com custo baixo.',
    'Fallback econômico para operações básicas. Recomendado apenas quando orçamento é extremamente limitado e qualidade pode ser comprometida.',
    1, -- Mesmo custo que gpt-4o-mini mas qualidade inferior
    'BASIC',
    true,
    4
) ON CONFLICT (name) DO UPDATE SET
    capabilities = EXCLUDED.capabilities,
    impact_description = EXCLUDED.impact_description,
    cost_per_1k_tokens = EXCLUDED.cost_per_1k_tokens,
    performance_level = EXCLUDED.performance_level,
    sort_order = EXCLUDED.sort_order;

-- Garantir que GPT-4o mini esteja ativo e seja o padrão
UPDATE ai_models SET 
    is_active = true,
    sort_order = 1
WHERE name = 'gpt-4o-mini';

-- Comentário para documentação
COMMENT ON COLUMN ai_models.cost_per_1k_tokens IS 'Custo em centavos de real por 1000 tokens (baseado em USD convertido). GPT-4o mini: ~R$0.001/1k tokens';
COMMENT ON COLUMN ai_models.sort_order IS 'Ordem de prioridade - menor número = maior prioridade. GPT-4o mini tem prioridade 1 (padrão)';