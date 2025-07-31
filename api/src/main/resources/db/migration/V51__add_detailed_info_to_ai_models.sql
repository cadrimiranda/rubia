-- Adicionar novos campos descritivos na tabela ai_models
ALTER TABLE ai_models ADD COLUMN capabilities TEXT;
ALTER TABLE ai_models ADD COLUMN impact_description TEXT;
ALTER TABLE ai_models ADD COLUMN cost_per_1k_tokens INTEGER;
ALTER TABLE ai_models ADD COLUMN performance_level VARCHAR(20);

-- Atualizar dados dos modelos existentes com informações detalhadas

-- GPT-4.1
UPDATE ai_models SET 
    capabilities = 'Modelo mais avançado da OpenAI com raciocínio superior, compreensão contextual profunda e capacidade de análise complexa. Excelente para tarefas que exigem pensamento crítico, resolução de problemas sofisticados e geração de conteúdo de alta qualidade.',
    impact_description = 'Ideal para atendimento premium, consultas complexas, análise de documentos técnicos e situações que requerem máxima precisão. Proporciona a melhor experiência do cliente com respostas mais assertivas e contextualmente relevantes.',
    cost_per_1k_tokens = 15,
    performance_level = 'PREMIUM'
WHERE name = 'gpt-4.1';

-- GPT-4 Mini
UPDATE ai_models SET 
    capabilities = 'Versão otimizada do GPT-4 com excelente custo-benefício. Mantém alta qualidade nas respostas com processamento mais rápido e menor consumo de recursos. Adequado para a maioria dos casos de uso cotidianos.',
    impact_description = 'Perfeito para atendimento geral, FAQ automatizado, suporte básico e interações do dia a dia. Oferece respostas rápidas e precisas com consumo moderado de créditos, sendo ideal para alto volume de conversas.',
    cost_per_1k_tokens = 5,
    performance_level = 'INTERMEDIARIO'
WHERE name = 'gpt-4o-mini';

-- O3
UPDATE ai_models SET 
    capabilities = 'Modelo especializado em raciocínio científico, matemático e lógico. Destaca-se em resolução de problemas complexos, análise de dados, cálculos avançados e tarefas que exigem pensamento estruturado e metodológico.',
    impact_description = 'Recomendado para consultas técnicas especializadas, análise de dados médicos, cálculos de compatibilidade, questões científicas e casos que demandam raciocínio analítico profundo. Excelente para centros de hematologia e hemoterapia.',
    cost_per_1k_tokens = 20,
    performance_level = 'PREMIUM'
WHERE name = 'o3';

-- Adicionar índices para performance
CREATE INDEX idx_ai_models_performance_level ON ai_models(performance_level);
CREATE INDEX idx_ai_models_cost ON ai_models(cost_per_1k_tokens);