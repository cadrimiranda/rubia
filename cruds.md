# CRUD Implementation Status - Rubia Backend

## ✅ Implementados (Completos)
- **AIAgent** - ✅ TDD + Backend (Service, Controller, Repository, DTOs)
- **AILog** - ✅ TDD + Backend (Service, Controller, Repository, DTOs)
- **Company** - ✅ Backend (CompanyController existente)
- **CompanyGroup** - ✅ Backend (CompanyGroupController existente)
- **User** - ✅ Backend (UserController existente)
- **Customer** - ✅ Backend (CustomerController existente)
- **Department** - ✅ Backend (DepartmentController existente)
- **Conversation** - ✅ Backend (ConversationController existente)
- **Message** - ✅ Backend (MessageController existente)

## 🔄 Em Andamento
- **Campaign** - ✅ TDD tests criados → ❌ Backend pendente

## ❌ CRUDs Backend Pendentes (8 entidades)

1. **Campaign** 🔄
2. **CampaignContact**
3. **ConversationMedia**
4. **ConversationParticipant**
5. **DonationAppointment**
6. **MessageTemplate**
7. **MessageTemplateRevision**
8. **UserAIAgent**

---

## 🎯 Estratégia de Implementação TDD

### Filosofia
Seguir **Test-Driven Development (TDD)** rigorosamente:
1. 🔴 **Red**: Escrever testes que falham
2. 🟢 **Green**: Implementar código mínimo para os testes passarem
3. 🔵 **Refactor**: Melhorar o código mantendo os testes passando

### Padrões Estabelecidos
- **Mock-based tests** para evitar problemas de database/enum
- **Cobertura 90%+** com testes abrangentes
- **16-22 testes por entidade** cobrindo todos os cenários
- **Nomenclatura**: `*ServiceMockTest.java`
- **Validação de relacionamentos** obrigatória
- **Logs estruturados** em todos os services

---

## 📋 Passo a Passo para Cada CRUD

### Fase 1: Análise da Entidade (5-10 min)
```bash
# 1. Ler entidade
cat api/src/main/java/com/ruby/rubia_server/core/entity/{Entity}.java

# 2. Verificar enums relacionados
find api/src -name "*{Enum}*.java"

# 3. Identificar relacionamentos obrigatórios/opcionais
```

### Fase 2: TDD Tests (30-45 min)
```bash
# Criar arquivo de teste
touch api/src/test/java/com/ruby/rubia_server/core/service/{Entity}ServiceMockTest.java
```

**Template de testes obrigatórios:**
- ✅ `create{Entity}_ShouldCreateAndReturn{Entity}_WhenValidData`
- ✅ `create{Entity}_ShouldCreateWithoutOptionalEntities_WhenOnlyRequiredDataProvided`
- ✅ `create{Entity}_ShouldThrowException_When{Relation}NotFound` (para cada relacionamento)
- ✅ `get{Entity}ById_ShouldReturn{Entity}_WhenExists`
- ✅ `get{Entity}ById_ShouldReturnEmpty_WhenNotExists`
- ✅ `getAll{Entity}s_ShouldReturnPagedResults`
- ✅ `get{Entity}sByCompanyId_ShouldReturn{Entity}sForCompany`
- ✅ `update{Entity}_ShouldUpdateAndReturn{Entity}_WhenValidData`
- ✅ `update{Entity}_ShouldReturnEmpty_WhenNotExists`
- ✅ `delete{Entity}_ShouldReturnTrue_WhenExists`
- ✅ `delete{Entity}_ShouldReturnFalse_WhenNotExists`
- ✅ Métodos específicos da entidade (count, exists, queries customizadas)

### Fase 3: Implementação Backend (45-60 min)

#### 3.1 DTOs
```bash
# Criar DTOs
touch api/src/main/java/com/ruby/rubia_server/core/dto/Create{Entity}DTO.java
touch api/src/main/java/com/ruby/rubia_server/core/dto/Update{Entity}DTO.java
touch api/src/main/java/com/ruby/rubia_server/core/dto/{Entity}DTO.java  # se necessário
```

#### 3.2 Repository
```bash
touch api/src/main/java/com/ruby/rubia_server/core/repository/{Entity}Repository.java
```

#### 3.3 Service
```bash
touch api/src/main/java/com/ruby/rubia_server/core/service/{Entity}Service.java
```

#### 3.4 Controller
```bash
touch api/src/main/java/com/ruby/rubia_server/core/controller/{Entity}Controller.java
```

### Fase 4: Validação (10-15 min)
```bash
# Rodar testes específicos
./mvnw test -Dtest={Entity}ServiceMockTest

# Rodar todos os testes
./mvnw test

# Verificar cobertura
echo "Verificar se todos os testes passam e cobertura está 90%+"
```

### Fase 5: Commit (5 min)
```bash
git add .
git commit -m "feat: Implement complete CRUD operations for {Entity} entity

- Add TDD test suite with X comprehensive unit tests for {Entity}Service
- Create DTOs: Create{Entity}DTO, Update{Entity}DTO with validation
- Implement {Entity}Repository with custom queries
- Add {Entity}Service with full CRUD operations and business logic
- Create {Entity}Controller with REST endpoints for all operations
- Include specific methods for [listar métodos específicos]
- Support relationships with [listar entidades relacionadas]

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 📝 Tarefas por Entidade

### 🔄 1. Campaign (EM ANDAMENTO)
- [x] **Análise**: Entidade analisada
- [x] **TDD Tests**: `CampaignServiceMockTest.java` criado (22 testes)
- [ ] **DTOs**: `CreateCampaignDTO`, `UpdateCampaignDTO`
- [ ] **Repository**: `CampaignRepository` com queries customizadas
- [ ] **Service**: `CampaignService` com lógica de negócio
- [ ] **Controller**: `CampaignController` com endpoints REST
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

**Relacionamentos**: Company (obrigatório), User (opcional), MessageTemplate (opcional)

### ❌ 2. CampaignContact
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `CampaignContactServiceMockTest.java`
- [ ] **DTOs**: `CreateCampaignContactDTO`, `UpdateCampaignContactDTO`
- [ ] **Repository**: `CampaignContactRepository`
- [ ] **Service**: `CampaignContactService`
- [ ] **Controller**: `CampaignContactController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 3. ConversationMedia
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `ConversationMediaServiceMockTest.java`
- [ ] **DTOs**: `CreateConversationMediaDTO`, `UpdateConversationMediaDTO`
- [ ] **Repository**: `ConversationMediaRepository`
- [ ] **Service**: `ConversationMediaService`
- [ ] **Controller**: `ConversationMediaController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 4. ConversationParticipant
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `ConversationParticipantServiceMockTest.java`
- [ ] **DTOs**: `CreateConversationParticipantDTO`, `UpdateConversationParticipantDTO`
- [ ] **Repository**: `ConversationParticipantRepository`
- [ ] **Service**: `ConversationParticipantService`
- [ ] **Controller**: `ConversationParticipantController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 5. DonationAppointment
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `DonationAppointmentServiceMockTest.java`
- [ ] **DTOs**: `CreateDonationAppointmentDTO`, `UpdateDonationAppointmentDTO`
- [ ] **Repository**: `DonationAppointmentRepository`
- [ ] **Service**: `DonationAppointmentService`
- [ ] **Controller**: `DonationAppointmentController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 6. MessageTemplate
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `MessageTemplateServiceMockTest.java`
- [ ] **DTOs**: `CreateMessageTemplateDTO`, `UpdateMessageTemplateDTO`
- [ ] **Repository**: `MessageTemplateRepository` (já existe, revisar se completo)
- [ ] **Service**: `MessageTemplateService`
- [ ] **Controller**: `MessageTemplateController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 7. MessageTemplateRevision
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `MessageTemplateRevisionServiceMockTest.java`
- [ ] **DTOs**: `CreateMessageTemplateRevisionDTO`, `UpdateMessageTemplateRevisionDTO`
- [ ] **Repository**: `MessageTemplateRevisionRepository`
- [ ] **Service**: `MessageTemplateRevisionService`
- [ ] **Controller**: `MessageTemplateRevisionController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

### ❌ 8. UserAIAgent
- [ ] **Análise**: Ler entidade e identificar relacionamentos
- [ ] **TDD Tests**: Criar `UserAIAgentServiceMockTest.java`
- [ ] **DTOs**: `CreateUserAIAgentDTO`, `UpdateUserAIAgentDTO`
- [ ] **Repository**: `UserAIAgentRepository`
- [ ] **Service**: `UserAIAgentService`
- [ ] **Controller**: `UserAIAgentController`
- [ ] **Validação**: Testes passando
- [ ] **Commit**: Implementação completa

---

## 🎯 Meta Final

**Objetivo**: Implementar 8 CRUDs backend completos seguindo TDD

**Estimativa de Tempo**: 
- Campaign (já iniciado): ~1h
- Demais 7 entidades: ~1.5h cada = ~10.5h
- **Total**: ~11.5h de implementação

**Critérios de Sucesso**:
- ✅ Todos os testes passando (target: 150+ testes)
- ✅ Cobertura 90%+ em todos os services
- ✅ Commits organizados com mensagens descritivas
- ✅ Padrão consistente em todas as implementações
- ✅ Zero regressões nos testes existentes

**Comando para verificar progresso**:
```bash
./mvnw test | grep "Tests run"
```