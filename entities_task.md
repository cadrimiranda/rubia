# Implementação de Entidades JPA - Rubia Chat

## Análise do Projeto Atual

**Stack configurado:**
- Spring Boot 3.5 + Java 21
- JPA + PostgreSQL + Flyway
- Lombok + Spring Security + WebSocket
- RabbitMQ + Redis + Actuator + Prometheus

**Estrutura atual:**
- Package: `com.ruby.rubia_server`
- Messaging já implementado (DTOs + Services + Controllers)
- Database desabilitado temporariamente (`application.properties`)

## Estratégia de Implementação

**Fases de desenvolvimento:**
1. **Core Entities** - Entidades principais
2. **Support Entities** - Entidades de apoio  
3. **Analytics Entities** - Auditoria e métricas
4. **Integration & Migration** - Flyway + configuração

---

## FASE 1: CORE ENTITIES

### 1.1 Department Entity
**Prioridade: ALTA** (prerequisito para User)

#### 1.1.1 Criar entidade Department
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/Department.java`
- [ ] Anotar com `@Entity`, `@Table`, `@Data`, `@Builder`
- [ ] Implementar campos: id, name, description, autoAssign, createdAt, updatedAt
- [ ] Anotar relacionamentos: `@OneToMany` para users, aiConfigurations

#### 1.1.2 Criar DepartmentRepository
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/repository/DepartmentRepository.java`
- [ ] Extender `JpaRepository<Department, UUID>`
- [ ] Adicionar métodos customizados: `findByName`, `findByAutoAssignTrue`

#### 1.1.3 Criar DTOs para Department
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/DepartmentDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CreateDepartmentDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UpdateDepartmentDTO.java`

#### 1.1.4 Criar DepartmentService
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/service/DepartmentService.java`
- [ ] Implementar CRUD: create, findById, findAll, update, delete
- [ ] Implementar métodos específicos: findByAutoAssign, countUsers
- [ ] Adicionar validações e tratamento de erro

#### 1.1.5 Criar DepartmentController
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/controller/DepartmentController.java`
- [ ] Anotar com `@RestController`, `@RequestMapping("/api/departments")`
- [ ] Implementar endpoints CRUD: POST, GET, PUT, DELETE
- [ ] Adicionar validação com `@Valid` e tratamento de exceções

#### 1.1.6 Criar testes unitários Department
- [ ] Criar `src/test/java/com/ruby/rubia_server/core/service/DepartmentServiceTest.java`
- [ ] Criar `src/test/java/com/ruby/rubia_server/core/controller/DepartmentControllerTest.java`
- [ ] Criar `src/test/java/com/ruby/rubia_server/core/repository/DepartmentRepositoryTest.java`

### 1.2 User Entity (Agentes)
**Prioridade: ALTA** (prerequisito para Conversation)

#### 1.2.1 Criar entidade User
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/User.java`
- [ ] Implementar campos: id, name, email, passwordHash, departmentId, role, avatarUrl, isOnline, lastSeen
- [ ] Anotar relacionamentos: `@ManyToOne` para department, `@OneToMany` para conversations

#### 1.2.2 Criar UserRole enum
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/UserRole.java`
- [ ] Implementar valores: ADMIN, SUPERVISOR, AGENT

#### 1.2.3 Criar UserRepository
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/repository/UserRepository.java`
- [ ] Adicionar métodos: `findByEmail`, `findByDepartmentId`, `findByIsOnlineTrue`, `findByRole`

#### 1.2.4 Criar DTOs para User
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UserDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CreateUserDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UpdateUserDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UserLoginDTO.java`

#### 1.2.5 Criar UserService
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/service/UserService.java`
- [ ] Implementar CRUD + autenticação
- [ ] Implementar métodos: updateOnlineStatus, findAvailableAgents, assignToDepartment
- [ ] Adicionar hash de senha com BCrypt

#### 1.2.6 Criar UserController
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/controller/UserController.java`
- [ ] Implementar endpoints CRUD + login/logout
- [ ] Adicionar endpoints: PUT /online-status, GET /available-agents

#### 1.2.7 Criar testes unitários User
- [ ] Criar testes para UserService (CRUD + business logic)
- [ ] Criar testes para UserController (endpoints + validações)
- [ ] Criar testes para UserRepository (queries customizadas)

### 1.3 Customer Entity
**Prioridade: ALTA** (prerequisito para Conversation)

#### 1.3.1 Criar entidade Customer
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/Customer.java`
- [ ] Implementar campos: id, phone, name, whatsappId, profileUrl, isBlocked
- [ ] Anotar relacionamentos: `@OneToMany` para conversations, customerContext

#### 1.3.2 Criar CustomerRepository
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/repository/CustomerRepository.java`
- [ ] Adicionar métodos: `findByPhone`, `findByWhatsappId`, `findByIsBlockedFalse`

#### 1.3.3 Criar DTOs para Customer
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CustomerDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CreateCustomerDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UpdateCustomerDTO.java`

#### 1.3.4 Criar CustomerService
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/service/CustomerService.java`
- [ ] Implementar CRUD + métodos específicos
- [ ] Implementar: findOrCreateByPhone, blockCustomer, unblockCustomer
- [ ] Adicionar validação de telefone (formato brasileiro)

#### 1.3.5 Criar CustomerController
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/controller/CustomerController.java`
- [ ] Implementar endpoints CRUD
- [ ] Adicionar endpoints: PUT /{id}/block, PUT /{id}/unblock

#### 1.3.6 Criar testes unitários Customer
- [ ] Criar testes para CustomerService
- [ ] Criar testes para CustomerController
- [ ] Criar testes para CustomerRepository

### 1.4 Conversation Entity
**Prioridade: ALTA** (core do sistema)

#### 1.4.1 Criar enums para Conversation
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/ConversationStatus.java`
- [ ] Implementar valores: ENTRADA, ESPERANDO, FINALIZADOS
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/ConversationChannel.java`
- [ ] Implementar valores: WHATSAPP, WEB, TELEGRAM

#### 1.4.2 Criar entidade Conversation
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/Conversation.java`
- [ ] Implementar campos: id, customerId, assignedUserId, departmentId, status, channel, priority, isPinned, closedAt
- [ ] Anotar relacionamentos: `@ManyToOne` para customer/user/department, `@OneToMany` para messages

#### 1.4.3 Criar ConversationRepository
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/repository/ConversationRepository.java`
- [ ] Adicionar métodos: `findByStatus`, `findByCustomerId`, `findByAssignedUserId`, `findByIsPinnedTrue`
- [ ] Adicionar queries com ordenação por updatedAt

#### 1.4.4 Criar DTOs para Conversation
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/ConversationDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CreateConversationDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UpdateConversationDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/ConversationSummaryDTO.java`

#### 1.4.5 Criar ConversationService
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/service/ConversationService.java`
- [ ] Implementar CRUD + business logic
- [ ] Implementar: assignToUser, changeStatus, pinConversation, getByStatusWithPagination
- [ ] Adicionar lógica de auto-assignment de departamento

#### 1.4.6 Criar ConversationController
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/controller/ConversationController.java`
- [ ] Implementar endpoints CRUD + ações específicas
- [ ] Adicionar endpoints: PUT /{id}/assign, PUT /{id}/status, PUT /{id}/pin
- [ ] Adicionar paginação para listagem

#### 1.4.7 Criar testes unitários Conversation
- [ ] Criar testes para ConversationService
- [ ] Criar testes para ConversationController
- [ ] Criar testes para ConversationRepository

### 1.5 Message Entity
**Prioridade: ALTA** (core do sistema)

#### 1.5.1 Criar enums para Message
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/SenderType.java`
- [ ] Implementar valores: CUSTOMER, AGENT, AI, SYSTEM
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/MessageType.java`
- [ ] Implementar valores: TEXT, IMAGE, AUDIO, FILE, LOCATION, CONTACT
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/enums/MessageStatus.java`
- [ ] Implementar valores: SENDING, SENT, DELIVERED, READ, FAILED

#### 1.5.2 Criar entidade Message
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/Message.java`
- [ ] Implementar campos: id, conversationId, content, senderType, senderId, messageType, mediaUrl, externalMessageId, isAiGenerated, aiConfidence, status, deliveredAt, readAt
- [ ] Anotar relacionamentos: `@ManyToOne` para conversation

#### 1.5.3 Criar MessageRepository
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/repository/MessageRepository.java`
- [ ] Adicionar métodos: `findByConversationId`, `findByExternalMessageId`, `findBySenderType`
- [ ] Adicionar query para busca full-text no content
- [ ] Adicionar paginação para mensagens de uma conversa

#### 1.5.4 Criar DTOs para Message
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/MessageDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/CreateMessageDTO.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/dto/UpdateMessageDTO.java`
- [ ] Adaptar DTOs existentes (IncomingMessage) para trabalhar com Message entity

#### 1.5.5 Criar MessageService
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/service/MessageService.java`
- [ ] Implementar CRUD + integração com messaging existente
- [ ] Implementar: sendMessage, markAsRead, markAsDelivered, searchInContent
- [ ] Integrar com MessagingService existente

#### 1.5.6 Criar MessageController
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/controller/MessageController.java`
- [ ] Implementar endpoints CRUD + ações específicas
- [ ] Adicionar endpoints: PUT /{id}/read, PUT /{id}/delivered, GET /search
- [ ] Adicionar WebSocket para mensagens em tempo real

#### 1.5.7 Criar testes unitários Message
- [ ] Criar testes para MessageService
- [ ] Criar testes para MessageController
- [ ] Criar testes para MessageRepository
- [ ] Criar testes de integração com MessagingService

---

## FASE 2: SUPPORT ENTITIES

### 2.1 Tag Entity
**Prioridade: MÉDIA**

#### 2.1.1 Criar entidade Tag + ConversationTag
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/Tag.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/ConversationTag.java` (tabela de junção)
- [ ] Implementar campos e relacionamentos many-to-many

#### 2.1.2 Criar TagRepository + ConversationTagRepository
- [ ] Criar repositories com métodos específicos
- [ ] Adicionar queries para buscar tags por conversa e vice-versa

#### 2.1.3 Criar DTOs, Service e Controller para Tag
- [ ] Implementar CRUD completo para tags
- [ ] Implementar endpoints para associar/desassociar tags de conversas

#### 2.1.4 Criar testes unitários Tag
- [ ] Criar testes para todas as camadas

### 2.2 AI Configuration Entity
**Prioridade: MÉDIA**

#### 2.2.1 Criar entidade AiConfiguration
- [ ] Criar `src/main/java/com/ruby/rubia_server/ai/entity/AiConfiguration.java`
- [ ] Implementar campos: departmentId, modelName, promptTemplate, maxTokens, temperature, isEnabled
- [ ] Anotar relacionamento com Department

#### 2.2.2 Criar AiConfigurationRepository
- [ ] Adicionar métodos: `findByDepartmentId`, `findByIsEnabledTrue`

#### 2.2.3 Criar DTOs, Service e Controller para AiConfiguration
- [ ] Implementar CRUD + métodos específicos para configuração de IA
- [ ] Adicionar validações para parâmetros de IA (temperature, maxTokens)

#### 2.2.4 Criar testes unitários AiConfiguration
- [ ] Criar testes para todas as camadas

### 2.3 Customer Context Entity
**Prioridade: BAIXA**

#### 2.3.1 Criar entidade CustomerContext
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/CustomerContext.java`
- [ ] Implementar campos: customerId, contextKey, contextValue (JSONB), expiresAt
- [ ] Anotar relacionamento com Customer

#### 2.3.2 Criar CustomerContextRepository
- [ ] Adicionar métodos: `findByCustomerIdAndContextKey`, `findByCustomerIdAndExpiresAtAfter`

#### 2.3.3 Criar DTOs, Service e Controller para CustomerContext
- [ ] Implementar CRUD + cache logic
- [ ] Implementar limpeza automática de contextos expirados

#### 2.3.4 Criar testes unitários CustomerContext
- [ ] Criar testes para todas as camadas

---

## FASE 3: ANALYTICS ENTITIES

### 3.1 Conversation Analytics Entity
**Prioridade: BAIXA**

#### 3.1.1 Criar entidade ConversationAnalytics
- [ ] Criar `src/main/java/com/ruby/rubia_server/analytics/entity/ConversationAnalytics.java`
- [ ] Implementar campos: conversationId, firstResponseTime, resolutionTime, messageCount, aiInteractionsCount, satisfactionScore, resolvedByAi, escalatedToHuman

#### 3.1.2 Criar ConversationAnalyticsRepository
- [ ] Adicionar métodos para relatórios e métricas

#### 3.1.3 Criar DTOs, Service e Controller para ConversationAnalytics
- [ ] Implementar endpoints para relatórios e métricas
- [ ] Adicionar cálculos automáticos de métricas

#### 3.1.4 Criar testes unitários ConversationAnalytics
- [ ] Criar testes para todas as camadas

### 3.2 Analytics Events Entity
**Prioridade: BAIXA**

#### 3.2.1 Criar entidade AnalyticsEvent
- [ ] Criar `src/main/java/com/ruby/rubia_server/analytics/entity/AnalyticsEvent.java`
- [ ] Implementar campos: conversationId, eventType, userId, metadata (JSONB)

#### 3.2.2 Criar AnalyticsEventRepository
- [ ] Adicionar métodos para queries de eventos por tipo e período

#### 3.2.3 Criar DTOs, Service e Controller para AnalyticsEvent
- [ ] Implementar sistema de eventos para tracking
- [ ] Implementar endpoints para consulta de eventos

#### 3.2.4 Criar testes unitários AnalyticsEvent
- [ ] Criar testes para todas as camadas

### 3.3 Message Actions Entity
**Prioridade: BAIXA**

#### 3.3.1 Criar entidade MessageAction
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/entity/MessageAction.java`
- [ ] Implementar campos: messageId, userId, actionType, metadata (JSONB)

#### 3.3.2 Criar MessageActionRepository
- [ ] Adicionar métodos para auditoria de ações

#### 3.3.3 Criar DTOs, Service e Controller para MessageAction
- [ ] Implementar logging automático de ações em mensagens
- [ ] Implementar endpoints para auditoria

#### 3.3.4 Criar testes unitários MessageAction
- [ ] Criar testes para todas as camadas

### 3.4 Integration Logs Entity
**Prioridade: BAIXA**

#### 3.4.1 Criar entidade IntegrationLog
- [ ] Criar `src/main/java/com/ruby/rubia_server/integration/entity/IntegrationLog.java`
- [ ] Implementar campos: conversationId, integrationType, requestData, responseData, statusCode, errorMessage

#### 3.4.2 Criar IntegrationLogRepository
- [ ] Adicionar métodos para debugging e monitoramento

#### 3.4.3 Criar DTOs, Service e Controller para IntegrationLog
- [ ] Implementar logging automático de integrações
- [ ] Implementar endpoints para monitoramento

#### 3.4.4 Criar testes unitários IntegrationLog
- [ ] Criar testes para todas as camadas

---

## FASE 4: INTEGRATION & MIGRATION

### 4.1 Database Migrations (Flyway)
**Prioridade: ALTA**

#### 4.1.1 Criar migrations principais
- [ ] Criar `src/main/resources/db/migration/V1__create_departments_table.sql`
- [ ] Criar `src/main/resources/db/migration/V2__create_users_table.sql`
- [ ] Criar `src/main/resources/db/migration/V3__create_customers_table.sql`
- [ ] Criar `src/main/resources/db/migration/V4__create_conversations_table.sql`
- [ ] Criar `src/main/resources/db/migration/V5__create_messages_table.sql`

#### 4.1.2 Criar migrations de apoio
- [ ] Criar `src/main/resources/db/migration/V6__create_tags_tables.sql`
- [ ] Criar `src/main/resources/db/migration/V7__create_ai_configurations_table.sql`
- [ ] Criar `src/main/resources/db/migration/V8__create_customer_context_table.sql`

#### 4.1.3 Criar migrations de analytics
- [ ] Criar `src/main/resources/db/migration/V9__create_analytics_tables.sql`
- [ ] Criar `src/main/resources/db/migration/V10__create_logs_tables.sql`

#### 4.1.4 Criar migrations de índices
- [ ] Criar `src/main/resources/db/migration/V11__create_indexes.sql`
- [ ] Criar `src/main/resources/db/migration/V12__create_triggers.sql`

#### 4.1.5 Criar dados iniciais
- [ ] Criar `src/main/resources/db/migration/V13__insert_initial_data.sql`
- [ ] Incluir departamentos padrão, usuário admin, tags básicas

### 4.2 Configuration & Integration
**Prioridade: ALTA**

#### 4.2.1 Atualizar configurações
- [ ] Modificar `application.properties` para habilitar database
- [ ] Criar `application-dev.properties` com configurações de desenvolvimento
- [ ] Criar `application-prod.properties` com configurações de produção

#### 4.2.2 Criar configurações adicionais
- [ ] Criar `src/main/java/com/ruby/rubia_server/config/DatabaseConfig.java`
- [ ] Criar `src/main/java/com/ruby/rubia_server/config/JpaConfig.java`
- [ ] Atualizar SecurityConfig para trabalhar com User entity

#### 4.2.3 Integrar com messaging existente
- [ ] Modificar MessagingService para usar Message entity
- [ ] Modificar MessagingController para trabalhar com Conversation entity
- [ ] Atualizar DTOs para mapear com entidades

#### 4.2.4 Criar mappers/converters
- [ ] Criar `src/main/java/com/ruby/rubia_server/core/mapper/EntityMapper.java`
- [ ] Implementar conversões entre DTOs e entities
- [ ] Adicionar validações e transformações

### 4.3 Exception Handling & Validation
**Prioridade: MÉDIA**

#### 4.3.1 Criar exception handling global
- [ ] Criar `src/main/java/com/ruby/rubia_server/common/exception/GlobalExceptionHandler.java`
- [ ] Criar exceptions customizadas: EntityNotFoundException, ValidationException, etc.

#### 4.3.2 Criar validações customizadas
- [ ] Criar validadores para telefone brasileiro
- [ ] Criar validadores para WhatsApp ID
- [ ] Criar validadores para configurações de IA

#### 4.3.3 Adicionar logging
- [ ] Configurar logback para diferentes níveis
- [ ] Adicionar MDC para tracking de conversas
- [ ] Implementar audit logging

---

## FASE 5: TESTING & DOCUMENTATION

### 5.1 Testes de Integração
**Prioridade: MÉDIA**

#### 5.1.1 Criar testes de integração principais
- [ ] Criar testes end-to-end para fluxo completo de conversa
- [ ] Criar testes de integração entre entidades
- [ ] Criar testes de performance para queries principais

#### 5.1.2 Criar testes de API
- [ ] Criar testes para todos os endpoints REST
- [ ] Criar testes de segurança e autenticação
- [ ] Criar testes de validação

### 5.2 Documentation
**Prioridade: BAIXA**

#### 5.2.1 Documentar APIs
- [ ] Adicionar Swagger/OpenAPI documentation
- [ ] Documentar todos os endpoints
- [ ] Criar exemplos de uso

#### 5.2.2 Criar documentação técnica
- [ ] Documentar arquitetura das entidades
- [ ] Criar guia de desenvolvimento
- [ ] Documentar fluxos de negócio

---

## CONFIGURAÇÃO FINAL

### 6.1 Settings Entities
**Prioridade: BAIXA**

#### 6.1.1 Criar IntegrationSettings entity
- [ ] Para configurações de WhatsApp, Telegram, etc.
- [ ] CRUD completo com validações específicas

#### 6.1.2 Criar SystemSettings entity
- [ ] Para configurações globais do sistema
- [ ] Suporte a encryption para valores sensíveis

---

## RESUMO DE EXECUÇÃO

**Total de tarefas: 150+**

**Prioridade ALTA (essencial):**
1. Department (6 tarefas)
2. User (7 tarefas) 
3. Customer (6 tarefas)
4. Conversation (7 tarefas)
5. Message (7 tarefas)
6. Database Migrations (5 tarefas)
7. Configuration & Integration (4 tarefas)

**Prioridade MÉDIA (importante):**
- Tag, AiConfiguration, Exception Handling, Testes de Integração

**Prioridade BAIXA (nice to have):**
- CustomerContext, Analytics, Logs, Documentation

**Estimativa:** 2-3 sprints para ALTA, +1 sprint para MÉDIA, +1 sprint para BAIXA