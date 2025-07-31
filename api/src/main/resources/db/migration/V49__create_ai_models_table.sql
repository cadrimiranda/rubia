-- Criação da tabela ai_models para armazenar os modelos de IA disponíveis
CREATE TABLE ai_models (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    provider VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger para atualizar updated_at automaticamente
CREATE TRIGGER ai_models_updated_at_trigger
    BEFORE UPDATE ON ai_models
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Inserir os modelos iniciais
INSERT INTO ai_models (name, display_name, description, provider, is_active, sort_order) VALUES
('gpt-4.1', 'GPT-4.1', 'Modelo GPT-4.1 da OpenAI com capacidades avançadas de raciocínio e compreensão', 'OpenAI', true, 1),
('gpt-4o-mini', 'GPT-4 Mini', 'Versão otimizada e mais rápida do GPT-4 para tarefas do dia a dia', 'OpenAI', true, 2),
('o3', 'O3', 'Modelo O3 da OpenAI com foco em raciocínio científico e matemático', 'OpenAI', true, 3);

-- Índices para performance
CREATE INDEX idx_ai_models_is_active ON ai_models(is_active);
CREATE INDEX idx_ai_models_provider ON ai_models(provider);
CREATE INDEX idx_ai_models_sort_order ON ai_models(sort_order);