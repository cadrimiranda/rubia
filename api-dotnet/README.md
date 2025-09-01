# Rubia Server - .NET Migration

Este projeto representa a migração da API Java Spring Boot do Rubia para .NET 9.

## Estrutura do Projeto

```
Rubia.Server/
├── Controllers/          # Controllers da API
├── Data/                # DbContext e configurações do banco
├── Entities/            # Entidades do domínio (equivalente aos entities do Java)
├── Enums/              # Enumerações
├── DTOs/               # Data Transfer Objects (para serem implementados)
├── Services/           # Serviços de negócio (para serem implementados)
├── Models/             # View Models (para serem implementados)
├── Program.cs          # Configuração da aplicação
├── appsettings.json    # Configurações da aplicação
└── Rubia.Server.csproj # Arquivo de projeto
```

## Entidades Migradas

Todas as principais entidades do Java foram migradas para C#:

### Core Entities
- ✅ `BaseEntity` - Classe base para todas as entidades
- ✅ `CompanyGroup` - Grupos de empresas
- ✅ `Company` - Empresas
- ✅ `Department` - Departamentos
- ✅ `User` - Usuários do sistema
- ✅ `Customer` - Clientes/Contatos
- ✅ `WhatsAppInstance` - Instâncias do WhatsApp

### AI & Messaging
- ✅ `AIModel` - Modelos de IA disponíveis
- ✅ `AIAgent` - Agentes de IA configurados
- ✅ `MessageTemplate` - Templates de mensagens
- ✅ `MessageTemplateRevision` - Revisões dos templates
- ✅ `Campaign` - Campanhas de marketing
- ✅ `CampaignContact` - Contatos das campanhas

### Conversations & Messages
- ✅ `Conversation` - Conversas
- ✅ `ConversationParticipant` - Participantes das conversas
- ✅ `ConversationMedia` - Mídias das conversas
- ✅ `Message` - Mensagens

### Enumerations
- ✅ `UserRole` - Papéis dos usuários
- ✅ `CompanyPlanType` - Tipos de plano das empresas
- ✅ `ConversationStatus` - Status das conversas
- ✅ `ConversationType` - Tipos de conversa
- ✅ `Channel` - Canais de comunicação
- ✅ `MessageStatus` - Status das mensagens
- ✅ `SenderType` - Tipos de remetente
- ✅ `MessagingProvider` - Provedores de mensagem
- ✅ `CampaignStatus` - Status das campanhas
- ✅ `CampaignContactStatus` - Status dos contatos nas campanhas
- ✅ `MediaType` - Tipos de mídia
- ✅ `RevisionType` - Tipos de revisão

## Configuração do Banco de Dados

O projeto está configurado para usar PostgreSQL com Entity Framework Core. As principais configurações incluem:

- Relacionamentos entre entidades configurados
- Índices únicos onde necessário
- Constraints de check para validações
- Soft delete implementado onde aplicável
- Timestamps automáticos (CreatedAt/UpdatedAt)

## Principais Diferenças da Versão Java

1. **Nomenclatura**: Propriedades seguem PascalCase (padrão C#)
2. **Tipos**: `LocalDateTime` → `DateTime`, `LocalDate` → `DateOnly`
3. **Annotations**: `@Entity` → `[Table]`, `@Column` → `[Column]`
4. **Enums**: Armazenados como strings no PostgreSQL
5. **Relacionamentos**: Configurados via Fluent API no `OnModelCreating`

## Como Executar

1. Instalar .NET 9 SDK
2. Configurar string de conexão do PostgreSQL no `appsettings.json`
3. Executar migrações (quando implementadas):
   ```bash
   dotnet ef database update
   ```
4. Executar aplicação:
   ```bash
   dotnet run
   ```

## Status da Migração

### ✅ Concluído
- **Entidades**: Todas as 16 entidades principais migradas
- **Enums**: Todos os 12 enums migrados
- **DbContext**: Configurado com relacionamentos e constraints
- **Services**: CompanyGroup, Company, Department, User migrados
- **DTOs**: DTOs completos (Create, Update, Response) para as 4 entidades
- **Controllers**: APIs REST completas para as 4 entidades
- **Dependency Injection**: Configurado no Program.cs

### 🔄 Em Andamento
- Nenhum item em andamento no momento

### ⏳ Próximos Passos
1. **Configurar Authentication**: Implementar JWT/Security
2. **Implementar SignalR**: Para funcionalidade de WebSocket
3. **Configurar Redis**: Para cache
4. **Migrar Services restantes**: AIAgent, Message, Conversation, etc.
5. **Implementar Controllers restantes**: APIs para outras entidades
6. **Testes**: Implementar testes unitários e integração
7. **Migrations**: Criar migrações do Entity Framework
8. **Docker**: Configurar containerização

## APIs Migradas

### CompanyGroups (`/api/company-groups`)
- `GET /` - Listar todos os grupos
- `GET /{id}` - Buscar por ID
- `POST /` - Criar novo grupo
- `PUT /{id}` - Atualizar grupo
- `DELETE /{id}` - Excluir grupo

### Companies (`/api/companies`)
- `GET /` - Listar todas as empresas
- `GET /{id}` - Buscar por ID
- `GET /slug/{slug}` - Buscar por slug
- `POST /` - Criar nova empresa
- `PUT /{id}` - Atualizar empresa
- `DELETE /{id}` - Excluir empresa

### Departments (`/api/departments`)
- `GET /` - Listar departamentos (requer companyId)
- `GET /{id}` - Buscar por ID (requer companyId)
- `POST /` - Criar novo departamento (requer companyId)
- `PUT /{id}` - Atualizar departamento (requer companyId)
- `DELETE /{id}` - Excluir departamento (requer companyId)

### Users (`/api/users`)
- `GET /` - Listar usuários (requer companyId, opcional departmentId)
- `GET /{id}` - Buscar por ID
- `GET /email/{email}` - Buscar por email (requer companyId)
- `GET /available-agents` - Listar agentes disponíveis (requer companyId)
- `POST /` - Criar novo usuário
- `POST /login` - Validar login (requer companyId)
- `PUT /{id}` - Atualizar usuário
- `PUT /{id}/online-status` - Atualizar status online
- `PUT /{userId}/assign-department/{departmentId}` - Atribuir departamento
- `DELETE /{id}` - Excluir usuário

## Tecnologias Utilizadas

- **.NET 9**: Framework principal
- **Entity Framework Core 9**: ORM
- **PostgreSQL**: Banco de dados
- **ASP.NET Core**: Web API
- **Swagger/OpenAPI**: Documentação da API