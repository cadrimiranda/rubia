# Database Entities - Rubia Chat Corporativo

## Visão Geral

Este documento define as entidades do banco de dados para o sistema Rubia, um chat corporativo com integração WhatsApp e IA. O modelo segue princípios de normalização (3FN) e boas práticas de design de banco de dados.

## Integração com Estruturas Existentes

**Frontend já implementado (`/client/src/types/index.ts`):**
- ✅ `User` interface - representa clientes do WhatsApp (mapeará para `customers`)
- ✅ `Chat` interface - representa conversas (mapeará para `conversations`)
- ✅ `Message` interface - representa mensagens (compatível com `messages`)
- ✅ `Tag` interface - representa tags (compatível com `tags`)
- ✅ `ChatStatus` type - fluxo 'entrada' → 'esperando' → 'finalizados'

**Backend DTOs existentes (`/api/.../messaging/model/`):**
- ✅ `IncomingMessage` - DTO para integração WhatsApp (mantido)
- ✅ `MessageResult` - DTO para resposta de envio (mantido)

**Novas entidades JPA a serem criadas:**
- 🆕 `User` entity - colaboradores/agentes do sistema
- 🆕 `Customer` entity - clientes do WhatsApp 
- 🆕 `Conversation` entity - sessões de chat
- 🆕 `Message` entity - mensagens persistidas
- 🆕 Demais entidades de negócio e controle

## Diagrama Relacional

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   users         │    │   departments   │    │   customers     │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ id (PK)         │    │ id (PK)         │    │ id (PK)         │
│ name            │    │ name            │    │ phone           │
│ email           │    │ description     │    │ name            │
│ password_hash   │    │ auto_assign     │    │ whatsapp_id     │
│ department_id   │◄───┤ created_at      │    │ profile_url     │
│ role            │    │ updated_at      │    │ is_blocked      │
│ avatar_url      │    └─────────────────┘    │ created_at      │
│ is_online       │                           │ updated_at      │
│ last_seen       │                           └─────────────────┘
│ created_at      │                                    │
│ updated_at      │                                    │
└─────────────────┘                                    │
         │                                             │
         │    ┌─────────────────┐                     │
         │    │  conversations  │                     │
         │    ├─────────────────┤                     │
         └────┤ assigned_user_id│                     │
              │ id (PK)         │◄────────────────────┘
              │ customer_id (FK)│
              │ department_id   │
              │ status          │
              │ channel         │
              │ priority        │
              │ is_pinned       │
              │ created_at      │
              │ updated_at      │
              │ closed_at       │
              └─────────────────┘
                       │
                       │
         ┌─────────────▼─────────┐     ┌─────────────────┐
         │      messages         │     │      tags       │
         ├───────────────────────┤     ├─────────────────┤
         │ id (PK)               │     │ id (PK)         │
         │ conversation_id (FK)  │     │ name            │
         │ content               │     │ color           │
         │ sender_type           │     │ tag_type        │
         │ sender_id             │     │ created_at      │
         │ message_type          │     └─────────────────┘
         │ media_url             │              │
         │ external_message_id   │              │
         │ is_ai_generated       │     ┌────────▼────────┐
         │ ai_confidence         │     │conversation_tags│
         │ status                │     ├─────────────────┤
         │ created_at            │     │ conversation_id │
         │ delivered_at          │     │ tag_id          │
         │ read_at               │     │ created_at      │
         └───────────────────────┘     └─────────────────┘
                       │
                       │
              ┌────────▼────────┐
              │ message_actions │
              ├─────────────────┤
              │ id (PK)         │
              │ message_id (FK) │
              │ user_id (FK)    │
              │ action_type     │
              │ metadata        │
              │ created_at      │
              └─────────────────┘

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ ai_configurations│    │customer_context │    │integration_logs │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ id (PK)         │    │ id (PK)         │    │ id (PK)         │
│ department_id   │◄──┐│ customer_id (FK)│◄──┐│ conversation_id │◄──┐
│ model_name      │   ││ context_key     │   ││ integration_type│   │
│ prompt_template │   ││ context_value   │   ││ request_data    │   │
│ max_tokens      │   ││ expires_at      │   ││ response_data   │   │
│ temperature     │   ││ created_at      │   ││ status_code     │   │
│ is_enabled      │   ││ updated_at      │   ││ error_message   │   │
│ created_at      │   │└─────────────────┘   ││ created_at      │   │
│ updated_at      │   │                      │└─────────────────┘   │
└─────────────────┘   │                      │                     │
         ▲            │                      │                     │
         │            │                      │                     │
    departments       │                 conversations              │
                      │                      ▲                     │
                      │                      │                     │
                 customers                   │                     │
                                            │                     │
┌─────────────────┐    ┌─────────────────┐  │  ┌─────────────────┐ │
│ analytics_events│    │conversation_analytics │  │integration_settings│
├─────────────────┤    ├─────────────────┤  │  ├─────────────────┤ │
│ id (PK)         │    │ id (PK)         │  │  │ id (PK)         │ │
│ conversation_id │◄───┤ conversation_id │◄─┘  │ provider        │ │
│ event_type      │    │ first_response  │     │ api_key         │ │
│ user_id         │◄─┐ │ resolution_time │     │ webhook_url     │ │
│ metadata        │  │ │ satisfaction    │     │ settings_json   │ │
│ created_at      │  │ │ resolved_by_ai  │     │ is_active       │ │
└─────────────────┘  │ │ created_at      │     │ created_at      │ │
                     │ │ updated_at      │     │ updated_at      │ │
                     │ └─────────────────┘     └─────────────────┘ │
                     │                                             │
                    users                                          │
                                                                   │
┌─────────────────┐                                               │
│ system_settings │                                               │
├─────────────────┤            ┌─────────────────┐                │
│ id (PK)         │            │ message_actions │◄───────────────┘
│ setting_key     │            ├─────────────────┤
│ setting_value   │            │ id (PK)         │
│ description     │            │ message_id (FK) │◄──messages
│ is_encrypted    │            │ user_id (FK)    │◄──users
│ created_at      │            │ action_type     │
│ updated_at      │            │ metadata        │
└─────────────────┘            │ created_at      │
                               └─────────────────┘
```

## Entidades Principais

### 1. users (JPA Entity)
**Função**: Gerencia colaboradores/agentes do sistema (não confundir com User interface do frontend)
**Problema que resolve**: Controle de acesso, atribuição de conversas, auditoria
**Compatibilidade**: Nova entidade - representa os agentes que atendem chats

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    department_id UUID REFERENCES departments(id),
    role VARCHAR(50) NOT NULL CHECK (role IN ('admin', 'supervisor', 'agent')),
    avatar_url TEXT,
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

**Campos importantes**:
- `role`: Define permissões (admin, supervisor, agent)
- `is_online`: Status em tempo real para distribuição de conversas
- `department_id`: Para roteamento automático

### 2. customers (JPA Entity)
**Função**: Armazena dados dos clientes/contatos do WhatsApp
**Problema que resolve**: Identificação única, histórico, contexto
**Compatibilidade**: Mapeia para interface `User` do frontend (representa clientes)

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255),
    whatsapp_id VARCHAR(255) UNIQUE,
    profile_url TEXT,
    is_blocked BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

**Campos importantes**:
- `phone`: Identificador principal (normalizado)
- `whatsapp_id`: ID do WhatsApp para integração
- `is_blocked`: Controle de bloqueio

### 3. departments (JPA Entity)
**Função**: Organiza filas/departamentos
**Problema que resolve**: Roteamento automático, especialização
**Compatibilidade**: Nova entidade - não existe no frontend atual

```sql
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    auto_assign BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 4. conversations (JPA Entity)
**Função**: Sessões de chat individuais
**Problema que resolve**: Agrupamento de mensagens, workflow, analytics
**Compatibilidade**: Mapeia para interface `Chat` do frontend

```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    assigned_user_id UUID REFERENCES users(id),
    department_id UUID REFERENCES departments(id),
    status VARCHAR(20) NOT NULL DEFAULT 'entrada' 
        CHECK (status IN ('entrada', 'esperando', 'finalizados')),
    channel VARCHAR(20) NOT NULL DEFAULT 'whatsapp',
    priority INTEGER DEFAULT 0,
    is_pinned BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    closed_at TIMESTAMP WITH TIME ZONE
);
```

**Campos importantes**:
- `status`: Fluxo do chat (entrada → esperando → finalizados)
- `priority`: Para ordenação (0 = normal, 1 = alta, -1 = baixa)
- `channel`: Origem da conversa (whatsapp, web, etc.)

### 5. messages (JPA Entity)
**Função**: Armazena todas as mensagens
**Problema que resolve**: Histórico completo, busca, auditoria
**Compatibilidade**: Compatível com interface `Message` do frontend + campos adicionais

```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    sender_type VARCHAR(20) NOT NULL CHECK (sender_type IN ('customer', 'agent', 'ai', 'system')),
    sender_id UUID, -- user_id quando sender_type = 'agent'
    message_type VARCHAR(20) DEFAULT 'text' 
        CHECK (message_type IN ('text', 'image', 'audio', 'file', 'location', 'contact')),
    media_url TEXT,
    external_message_id VARCHAR(255), -- ID do WhatsApp/provider
    is_ai_generated BOOLEAN DEFAULT false,
    ai_confidence DECIMAL(3,2), -- Score de confiança da IA (0.00-1.00)
    status VARCHAR(20) DEFAULT 'sent' 
        CHECK (status IN ('sending', 'sent', 'delivered', 'read', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE
);
```

**Campos importantes**:
- `sender_type`: Distingue origem (customer, agent, ai, system)
- `external_message_id`: Para sincronização com WhatsApp
- `ai_confidence`: Para analytics de IA

## Entidades de Apoio

### 6. tags (JPA Entity)
**Função**: Categorização flexível de conversas
**Problema que resolve**: Organização, filtros, relatórios
**Compatibilidade**: Compatível com interface `Tag` do frontend

```sql
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL, -- Hex color (#FFFFFF)
    tag_type VARCHAR(50) NOT NULL CHECK (tag_type IN ('comercial', 'suporte', 'vendas', 'custom')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE conversation_tags (
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (conversation_id, tag_id)
);
```

### 7. ai_configurations (JPA Entity) 
**Função**: Configurações da IA por departamento
**Problema que resolve**: Personalização, A/B testing
**Compatibilidade**: Nova entidade - atende requisito de `AI_Configurations`

```sql
CREATE TABLE ai_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id),
    model_name VARCHAR(100) NOT NULL,
    prompt_template TEXT NOT NULL,
    max_tokens INTEGER DEFAULT 1000,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 8. customer_context (JPA Entity)
**Função**: Cache de contexto do cliente
**Problema que resolve**: Performance, personalização
**Compatibilidade**: Nova entidade - atende requisito de `Customer_Context`

```sql
CREATE TABLE customer_context (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    context_key VARCHAR(100) NOT NULL, -- 'last_purchase', 'preferred_language', etc.
    context_value JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, context_key)
);
```

## Entidades de Auditoria e Analytics

### 9. message_actions
**Função**: Log de ações em mensagens
**Problema que resolve**: Auditoria, analytics

```sql
CREATE TABLE message_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL, -- 'edit', 'delete', 'pin', 'forward'
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 10. analytics_events
**Função**: Eventos para analytics
**Problema que resolve**: KPIs, relatórios, otimização

```sql
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id),
    event_type VARCHAR(100) NOT NULL, -- 'conversation_started', 'first_response', 'resolved'
    user_id UUID REFERENCES users(id),
    metadata JSONB, -- Dados específicos do evento
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 11. integration_logs
**Função**: Logs de integrações externas
**Problema que resolve**: Debug, monitoramento

```sql
CREATE TABLE integration_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id),
    integration_type VARCHAR(50) NOT NULL, -- 'whatsapp', 'telegram', 'api'
    request_data JSONB,
    response_data JSONB,
    status_code INTEGER,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 12. integration_settings (JPA Entity)
**Função**: Configurações de integrações externas (WhatsApp, etc.)
**Problema que resolve**: Flexibilidade, configuração sem deploy
**Compatibilidade**: Nova entidade - atende requisito de `Integration_Settings`

```sql
CREATE TABLE integration_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL, -- 'whatsapp', 'telegram', etc.
    api_key TEXT,
    webhook_url TEXT,
    settings_json JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(provider)
);
```

### 13. conversation_analytics (JPA Entity)
**Função**: Métricas específicas de conversas
**Problema que resolve**: KPIs, relatórios de performance
**Compatibilidade**: Nova entidade - atende requisito de `Conversation_Analytics`

```sql
CREATE TABLE conversation_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    first_response_time INTEGER, -- segundos para primeira resposta
    resolution_time INTEGER, -- segundos até finalização
    message_count INTEGER DEFAULT 0,
    ai_interactions_count INTEGER DEFAULT 0,
    satisfaction_score DECIMAL(3,2), -- 0.00-5.00
    resolved_by_ai BOOLEAN DEFAULT false,
    escalated_to_human BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(conversation_id)
);
```

### 14. system_settings (JPA Entity)
**Função**: Configurações globais do sistema
**Problema que resolve**: Configurações gerais não relacionadas a integrações

```sql
CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## Índices Principais

```sql
-- Performance para consultas frequentes
CREATE INDEX idx_conversations_status_updated ON conversations(status, updated_at DESC);
CREATE INDEX idx_conversations_customer_created ON conversations(customer_id, created_at DESC);
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_users_department_online ON users(department_id, is_online);
CREATE INDEX idx_analytics_events_type_created ON analytics_events(event_type, created_at);

-- Busca full-text em mensagens
CREATE INDEX idx_messages_content_gin ON messages USING gin(to_tsvector('portuguese', content));
```

## Triggers e Funções

```sql
-- Atualiza updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## Princípios de Normalização Aplicados

1. **1FN**: Todos os campos são atômicos
2. **2FN**: Eliminação de dependências parciais (conversation_tags separada)
3. **3FN**: Eliminação de dependências transitivas (departments separado de users)

## Compatibilidade Frontend ↔ Backend

**Mapeamentos diretos:**
- Frontend `User` ↔ Backend `Customer` entity (clientes WhatsApp)
- Frontend `Chat` ↔ Backend `Conversation` entity
- Frontend `Message` ↔ Backend `Message` entity
- Frontend `Tag` ↔ Backend `Tag` entity
- Frontend `ChatStatus` ↔ Backend `conversations.status`

**Entidades Backend exclusivas (não expostas ao frontend):**
- `User` entity (agentes/colaboradores)
- `Department`, `AI_Configuration`, `Customer_Context`
- `Integration_Settings`, `Conversation_Analytics`
- Entidades de auditoria e logs

## Estratégia de Implementação

1. **Fase 1**: Criar entidades JPA principais (`User`, `Customer`, `Conversation`, `Message`, `Tag`)
2. **Fase 2**: Adaptar DTOs existentes para trabalhar com novas entidades
3. **Fase 3**: Implementar entidades de negócio (`Department`, `AI_Configuration`)
4. **Fase 4**: Adicionar analytics e auditoria

## Vantagens do Design

- **Compatibilidade**: Mantém estruturas frontend existentes
- **Escalabilidade**: UUIDs permitem sharding futuro
- **Performance**: Índices otimizados para consultas principais
- **Flexibilidade**: JSONB para metadados dinâmicos
- **Auditoria**: Logs completos de ações
- **Analytics**: Eventos estruturados para BI
- **Integração**: Preparado para múltiplos canais