# 🚀 ROADMAP: Sistema de Agentes IA Avançado - Rubia

## 📋 Estrutura de Epics e Tasks para Jira

---

## 🎯 **EPIC 1: Context-Aware Conversation System**
**Estimativa:** 6-8 semanas | **Prioridade:** Crítica

### **SPRINT 1.1: Database Foundation (2 semanas)**

#### **RBY-101: Criar Schema de Contexto de Conversa**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Backend/Database

**Descrição:**
Como desenvolvedor, preciso de um schema de banco para armazenar contexto de conversas para que os agentes IA possam ter memória entre interações.

**Critérios de Aceitação:**
- [ ] Criar migração V59 com tabela `conversation_context`
- [ ] Campos: conversation_id, donor_profile, conversation_state, last_interaction, preferences
- [ ] Índices otimizados para consultas por conversation_id e donor_id
- [ ] Constraints e validações adequadas
- [ ] Documentação da estrutura

**Subtasks:**
- [ ] Desenhar schema da tabela conversation_context
- [ ] Criar migração Flyway V59
- [ ] Adicionar índices de performance
- [ ] Criar constraints de integridade referencial
- [ ] Testar migração em ambiente local

---

#### **RBY-102: Entidade JPA ConversationContext**
**Tipo:** Story | **Pontos:** 5 | **Componente:** Backend/Entity

**Descrição:**
Como desenvolvedor, preciso da entidade JPA ConversationContext para mapear o contexto de conversas no código.

**Critérios de Aceitação:**
- [ ] Criar entidade ConversationContext com mapeamentos JPA
- [ ] Relacionamentos com Conversation e Donor
- [ ] Validações de campo usando Bean Validation
- [ ] Métodos de conveniência para estado da conversa
- [ ] Testes unitários da entidade

**Dependências:** RBY-101

---

#### **RBY-103: Repository e Service de ConversationContext**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Backend/Service

**Descrição:**
Como desenvolvedor, preciso de repository e service para gerenciar contexto de conversas.

**Critérios de Aceitação:**
- [ ] Criar ConversationContextRepository com queries customizadas
- [ ] Implementar ConversationContextService com CRUD
- [ ] Métodos para buscar contexto por conversation_id
- [ ] Métodos para atualizar estado da conversa
- [ ] Cache de contexto para performance
- [ ] Testes unitários e de integração

**Dependências:** RBY-102

---

### **SPRINT 1.2: Context Integration (2 semanas)**

#### **RBY-104: Integrar Contexto no AIAgentService**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Backend/Service

**Descrição:**
Como sistema, preciso que o AIAgentService use contexto de conversa para gerar respostas mais personalizadas.

**Critérios de Aceitação:**
- [ ] Modificar AIAgentService.enhanceMessage() para usar contexto
- [ ] Criar prompts contextualizados com histórico do doador
- [ ] Implementar logic para diferentes estados de conversa
- [ ] Salvar contexto atualizado após cada interação
- [ ] Fallback gracioso quando contexto não existe
- [ ] Logs detalhados para debugging

**Dependências:** RBY-103

---

#### **RBY-105: API Endpoints para Contexto**
**Tipo:** Story | **Pontos:** 5 | **Componente:** Backend/API

**Descrição:**
Como frontend, preciso de endpoints para consultar e atualizar contexto de conversas.

**Critérios de Aceitação:**
- [ ] GET /api/conversations/{id}/context
- [ ] PUT /api/conversations/{id}/context
- [ ] POST /api/conversations/{id}/context/reset
- [ ] Validação de permissões por empresa
- [ ] Documentação OpenAPI
- [ ] Testes de API

**Dependências:** RBY-103

---

### **SPRINT 1.3: Frontend Integration (2 semanas)**

#### **RBY-106: Frontend Context Management**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Frontend

**Descrição:**
Como usuário, quero que o sistema lembre do contexto da conversa para ter interações mais naturais.

**Critérios de Aceitação:**
- [ ] Modificar BloodCenterChat para carregar contexto
- [ ] Exibir indicadores visuais do estado da conversa
- [ ] Atualizar contexto quando trocar de conversa
- [ ] Loading states durante carregamento de contexto
- [ ] Error handling para falhas de contexto

**Dependências:** RBY-105

---

---

## 🎯 **EPIC 2: Blood Donation Knowledge Base**
**Estimativa:** 4-6 semanas | **Prioridade:** Alta

### **SPRINT 2.1: Knowledge Base Foundation (2 semanas)**

#### **RBY-201: Schema da Base de Conhecimento**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Backend/Database

**Descrição:**
Como sistema, preciso de uma base de conhecimento estruturada sobre doação de sangue para fornecer informações precisas.

**Critérios de Aceitação:**
- [ ] Criar migração V60 com tabelas knowledge_base e knowledge_categories
- [ ] Campos: category, content, blood_types, urgency_level, seasonal_relevance
- [ ] Índices full-text para busca de conteúdo
- [ ] Versionamento de conhecimento
- [ ] Seed data com informações básicas de doação

**Subtasks:**
- [ ] Desenhar schema knowledge_base
- [ ] Criar categorias padrão (eligibility, procedures, health)
- [ ] Popular com dados iniciais de doação
- [ ] Criar índices de busca
- [ ] Validar estrutura com especialista em doação

---

#### **RBY-202: Entidades e Services de Knowledge Base**
**Tipo:** Story | **Pontos:** 10 | **Componente:** Backend/Service

**Descrição:**
Como desenvolvedor, preciso de entidades e services para gerenciar a base de conhecimento.

**Critérios de Aceitação:**
- [ ] Entidade KnowledgeBase com mapeamentos JPA
- [ ] KnowledgeBaseRepository com busca full-text
- [ ] KnowledgeBaseService com cache inteligente
- [ ] Métodos de busca por categoria e tipo sanguíneo
- [ ] Versionamento automático de conhecimento
- [ ] Testes unitários completos

**Dependências:** RBY-201

---

### **SPRINT 2.2: Knowledge Integration (2 semanas)**

#### **RBY-203: Integrar Knowledge Base no AI Agent**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Backend/Service

**Descrição:**
Como agente IA, preciso consultar a base de conhecimento para dar respostas precisas sobre doação de sangue.

**Critérios de Aceitação:**
- [ ] Modificar buildEnhancementPrompt() para incluir conhecimento relevante
- [ ] Busca automática de conhecimento baseada em keywords da mensagem
- [ ] Priorização por urgência e relevância sazonal
- [ ] Limite de tokens para evitar prompts muito longos
- [ ] Fallback quando conhecimento não encontrado
- [ ] Métricas de uso da base de conhecimento

**Dependências:** RBY-202

---

#### **RBY-204: Admin Interface para Knowledge Base**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Frontend

**Descrição:**
Como administrador, preciso de interface para gerenciar a base de conhecimento do sistema.

**Critérios de Aceitação:**
- [ ] Página de administração na configuração
- [ ] CRUD completo de conhecimento
- [ ] Editor rich text para conteúdo
- [ ] Filtros por categoria e tipo sanguíneo
- [ ] Preview de como conhecimento afeta prompts
- [ ] Versionamento visual com histórico

**Dependências:** RBY-202

---

---

## 🎯 **EPIC 3: Multiple Specialized AI Agents**
**Estimativa:** 6-8 semanas | **Prioridade:** Alta

### **SPRINT 3.1: Agent Roles System (3 semanas)**

#### **RBY-301: Enum e Schema para Agent Roles**
**Tipo:** Story | **Pontos:** 5 | **Componente:** Backend/Database

**Descrição:**
Como sistema, preciso suportar diferentes tipos de agentes especializados por função.

**Critérios de Acritação:**
- [ ] Criar enum AgentRole (RECRUITMENT, RETENTION, EMERGENCY, EDUCATION, SUPPORT)
- [ ] Adicionar coluna agent_role na tabela ai_agents
- [ ] Migração V61 para adicionar campo
- [ ] Atualizar constraints e validações
- [ ] Documentar cada tipo de agente

---

#### **RBY-302: Modificar Entidade AIAgent para Roles**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Backend/Entity

**Descrição:**
Como desenvolvedor, preciso que a entidade AIAgent suporte roles especializados.

**Critérios de Aceitação:**
- [ ] Adicionar campo agentRole na entidade AIAgent
- [ ] Atualizar DTOs (Create e Update) para incluir role
- [ ] Validações específicas por tipo de agente
- [ ] Métodos de conveniência para verificar role
- [ ] Testes unitários para todas as roles

**Dependências:** RBY-301

---

#### **RBY-303: Agent Selection Logic**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Backend/Service

**Descrição:**
Como sistema, preciso de lógica inteligente para selecionar o agente adequado baseado no contexto da conversa.

**Critérios de Aceitação:**
- [ ] Criar AgentSelectionService
- [ ] Algoritmo para detectar intent da mensagem
- [ ] Seleção automática baseada em palavras-chave
- [ ] Fallback para agente padrão quando não houver especialista
- [ ] Logs de decisão para auditoria
- [ ] Métricas de acertividade da seleção

**Dependências:** RBY-302

---

### **SPRINT 3.2: Frontend Multi-Agent (2 semanas)**

#### **RBY-304: Interface para Multiple Agents**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Frontend

**Descrição:**
Como usuário, preciso poder criar e gerenciar múltiplos agentes especializados.

**Critérios de Aceitação:**
- [ ] Atualizar AgentManagement para suportar múltiplos agentes
- [ ] Dropdown para seleção de role na criação
- [ ] Lista de agentes agrupada por role
- [ ] Indicadores visuais para cada tipo de agente
- [ ] Validação de regras de negócio (máx agentes por role)
- [ ] UX intuitiva para gestão de múltiplos agentes

**Dependências:** RBY-303

---

#### **RBY-305: Agent Selector no Chat**
**Tipo:** Story | **Pontos:** 8 | **Componente:** Frontend

**Descrição:**
Como usuário, quero ver qual agente está respondendo e poder trocar manualmente se necessário.

**Critérios de Aceitação:**
- [ ] Indicador visual do agente ativo no chat
- [ ] Dropdown para troca manual de agente
- [ ] Histórico de qual agente respondeu cada mensagem
- [ ] Feedback visual quando agente é selecionado automaticamente
- [ ] Tooltips explicando especialidade de cada agente

**Dependências:** RBY-304

---

---

## 🎯 **EPIC 4: Advanced Analytics & Intelligence**
**Estimativa:** 8-10 semanas | **Prioridade:** Média

### **SPRINT 4.1: Sentiment Analysis (3 semanas)**

#### **RBY-401: Sentiment Analysis Integration**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Backend/AI

**Descrição:**
Como sistema, preciso analisar o sentimento das mensagens dos doadores para melhorar atendimento.

**Critérios de Aceitação:**
- [ ] Integrar biblioteca de sentiment analysis (Azure Cognitive ou similar)
- [ ] Análise automática de mensagens recebidas
- [ ] Armazenar scores de sentimento na auditoria
- [ ] Alertas para sentimentos muito negativos
- [ ] Dashboard de sentimentos por período
- [ ] Calibração para contexto brasileiro

---

#### **RBY-402: Intent Recognition System**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Backend/AI

**Descrição:**
Como sistema, preciso reconhecer automaticamente a intenção dos doadores para routing adequado.

**Critérios de Aceitação:**
- [ ] Modelo de classificação de intents (schedule, info, complaint, emergency)
- [ ] Training data específico para doação de sangue
- [ ] API para classificação em tempo real
- [ ] Confidence scores para decisões
- [ ] Fallback para classificação manual
- [ ] Métricas de acurácia do modelo

---

### **SPRINT 4.2: Analytics Dashboard (3 semanas)**

#### **RBY-403: Backend Analytics Service**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Backend/Service

**Descrição:**
Como administrador, preciso de service backend para consolidar analytics de conversas e agentes.

**Critérios de Aceitação:**
- [ ] AnalyticsService com agregações de dados
- [ ] Métricas de performance por agente
- [ ] Análise de conversão por temperamento
- [ ] Relatórios de sentimento por período
- [ ] Cache de métricas para performance
- [ ] APIs para dashboard frontend

---

#### **RBY-404: Frontend Analytics Dashboard**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Frontend

**Descrição:**
Como gestor, preciso de dashboard visual para acompanhar performance dos agentes IA.

**Critérios de Aceitação:**
- [ ] Dashboard com gráficos de performance
- [ ] Métricas de conversão por agente/temperamento
- [ ] Análise de sentimento timeline
- [ ] Comparação A/B entre agentes
- [ ] Filtros por período e agente
- [ ] Export de relatórios em PDF/Excel

**Dependências:** RBY-403

---

### **SPRINT 4.3: Optimization Features (2 semanas)**

#### **RBY-405: A/B Testing Framework**
**Tipo:** Story | **Pontos:** 13 | **Componente:** Backend/Service

**Descrição:**
Como administrador, preciso testar diferentes versões de agentes para otimizar performance.

**Critérios de Aceitação:**
- [ ] Framework para A/B testing de agentes
- [ ] Distribuição automática de tráfego
- [ ] Métricas de conversão por variante
- [ ] Interface para configurar testes
- [ ] Significância estatística automática
- [ ] Relatórios de resultado dos testes

---

---

## 🎯 **EPIC 5: Proactive Engagement System**
**Estimativa:** 6-8 semanas | **Prioridade:** Média

### **SPRINT 5.1: Smart Reminders (3 semanas)**

#### **RBY-501: Reminder Scheduling System**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Backend/Service

**Descrição:**
Como sistema, preciso agendar lembretes inteligentes para doadores baseado em seu perfil e histórico.

**Critérios de Aceitação:**
- [ ] Scheduler para lembretes automáticos
- [ ] Personalização baseada em preferências do doador
- [ ] Templates de lembrete por tipo (pre-donation, post-donation)
- [ ] Integração com sistema de agendamento
- [ ] Opt-out automático para doadores
- [ ] Métricas de efetividade dos lembretes

---

#### **RBY-502: Emergency Campaign System**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Backend/Service

**Descrição:**
Como centro de doação, preciso disparar campanhas automáticas quando há escassez de tipos sanguíneos específicos.

**Critérios de Aceitação:**
- [ ] Sistema de monitoramento de estoque (mock inicialmente)
- [ ] Triggers automáticos para campanhas de urgência
- [ ] Segmentação de doadores por tipo sanguíneo
- [ ] Templates de mensagem por nível de urgência
- [ ] Rate limiting para não spam
- [ ] Dashboard de campanhas ativas

---

### **SPRINT 5.2: Campaign Management (3 semanas)**

#### **RBY-503: Campaign Builder Interface**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Frontend

**Descrição:**
Como administrador, preciso de interface para criar e gerenciar campanhas de doação.

**Critérios de Aceitação:**
- [ ] Builder visual para campanhas
- [ ] Segmentação de audiência por critérios
- [ ] Agendamento de envio de campanhas
- [ ] Preview de mensagens com diferentes agentes
- [ ] Análise de performance em tempo real
- [ ] Histórico de campanhas executadas

**Dependências:** RBY-502

---

---

## 🎯 **EPIC 6: Healthcare Integration**
**Estimativa:** 8-12 semanas | **Prioridade:** Baixa

### **SPRINT 6.1: Integration Framework (4 semanas)**

#### **RBY-601: Healthcare API Gateway**
**Tipo:** Story | **Pontos:** 34 | **Componente:** Backend/Integration

**Descrição:**
Como sistema, preciso de gateway para integração segura com sistemas de saúde externos.

**Critérios de Aceitação:**
- [ ] API Gateway com autenticação segura
- [ ] Conectores para sistemas hospitalares comuns
- [ ] Webhook endpoints para notificações de estoque
- [ ] Mapeamento de dados entre sistemas
- [ ] Logs de auditoria para compliance
- [ ] Rate limiting e circuit breakers

---

#### **RBY-602: Blood Inventory Integration**
**Tipo:** Story | **Pontos:** 21 | **Componente:** Backend/Integration

**Descrição:**
Como sistema, preciso me integrar com sistemas de estoque de sangue para campanhas automáticas.

**Critérios de Aceitação:**
- [ ] APIs para consulta de estoque em tempo real
- [ ] Notificações automáticas de níveis baixos
- [ ] Integração com sistema de campanhas
- [ ] Dashboard de estoque atual
- [ ] Alertas para gestores
- [ ] Histórico de níveis de estoque

**Dependências:** RBY-601

---

---

## 📋 **CONFIGURAÇÃO DO JIRA**

### **Componentes Sugeridos:**
- Backend/Database
- Backend/Entity  
- Backend/Service
- Backend/API
- Backend/Integration
- Backend/AI
- Frontend
- DevOps
- Testing
- Documentation

### **Labels Sugeridos:**
- ai-agent
- conversation-context
- knowledge-base
- analytics
- proactive
- healthcare
- security
- performance
- ux-improvement

### **Epics Hierarchy:**
```
🎯 EPIC 1: Context-Aware Conversation System [RBY-100]
├── SPRINT 1.1: Database Foundation [RBY-101 to RBY-103]
├── SPRINT 1.2: Context Integration [RBY-104 to RBY-105]  
└── SPRINT 1.3: Frontend Integration [RBY-106]

🎯 EPIC 2: Blood Donation Knowledge Base [RBY-200]
├── SPRINT 2.1: Knowledge Base Foundation [RBY-201 to RBY-202]
└── SPRINT 2.2: Knowledge Integration [RBY-203 to RBY-204]

🎯 EPIC 3: Multiple Specialized AI Agents [RBY-300]
├── SPRINT 3.1: Agent Roles System [RBY-301 to RBY-303]
└── SPRINT 3.2: Frontend Multi-Agent [RBY-304 to RBY-305]

🎯 EPIC 4: Advanced Analytics & Intelligence [RBY-400]
├── SPRINT 4.1: Sentiment Analysis [RBY-401 to RBY-402]
├── SPRINT 4.2: Analytics Dashboard [RBY-403 to RBY-404]
└── SPRINT 4.3: Optimization Features [RBY-405]

🎯 EPIC 5: Proactive Engagement System [RBY-500]
├── SPRINT 5.1: Smart Reminders [RBY-501 to RBY-502]
└── SPRINT 5.2: Campaign Management [RBY-503]

🎯 EPIC 6: Healthcare Integration [RBY-600]
└── SPRINT 6.1: Integration Framework [RBY-601 to RBY-602]
```

### **Estimativas Totais:**
- **Total Story Points:** ~400 pontos
- **Duração Estimada:** 8-12 meses (dependendo da velocidade da equipe)
- **Recursos Necessários:** 2-3 desenvolvedores backend, 1-2 frontend, 1 DevOps

### **Definition of Ready:**
- [ ] Critérios de aceitação claros e testáveis
- [ ] Dependências identificadas e resolvidas
- [ ] Designs/mockups aprovados (para stories de frontend)
- [ ] Estimativa de esforço definida
- [ ] Sprint capacity disponível

### **Definition of Done:**
- [ ] Código implementado e revisado
- [ ] Testes unitários e de integração
- [ ] Documentação atualizada
- [ ] Deploy em ambiente de staging
- [ ] Aprovação do Product Owner
- [ ] Métricas de performance validadas

---

## 🚨 **RISCOS E MITIGAÇÕES**

### **Riscos Técnicos:**
1. **Complexidade de IA:** Mitigar com POCs antes de implementação
2. **Performance:** Implementar cache e otimizações desde o início
3. **Integrações:** Criar mocks para testar independentemente

### **Riscos de Negócio:**
1. **Mudança de Requisitos:** Desenvolvimento incremental com validação
2. **Adoção do Usuário:** Testes de usabilidade frequentes
3. **Compliance:** Revisão legal desde o início

### **Recomendações de Implementação:**
1. **Começar por EPIC 1 e 2** - Base fundamental
2. **Validar com usuários** após cada epic
3. **Implementação incremental** com releases frequentes
4. **Métricas desde o início** para validar valor
5. **Documentação contínua** para manutenibilidade

Este roadmap garante evolução controlada e minimiza riscos de implementação! 🚀