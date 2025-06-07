# Rubia Chat Server API

Rubia é uma aplicação de chat corporativo com funcionalidades de chatbot alimentado por IA. Esta é a API backend construída com Spring Boot 3.5 e Java 21.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Configuração](#configuração)
- [Instalação e Execução](#instalação-e-execução)
- [Banco de Dados](#banco-de-dados)
- [Entidades](#entidades)
- [APIs Disponíveis](#apis-disponíveis)
- [Integração com WhatsApp](#integração-com-whatsapp)
- [Testes](#testes)
- [Docker](#docker)

## 🎯 Visão Geral

O Rubia Chat Server é uma API REST que gerencia conversas de atendimento ao cliente através de múltiplos canais (WhatsApp, Web Chat). O sistema suporta:

- **Multi-tenant**: Isolamento de dados por empresa
- **Gerenciamento de usuários**: Administradores, supervisores e agentes
- **Departamentos**: Organização hierárquica de usuários
- **Conversas**: Fluxo completo de atendimento com status e atribuição
- **Mensagens**: Sistema de mensagens com busca full-text
- **Integração WhatsApp**: Via Twilio ou outros provedores
- **Tempo real**: WebSockets para atualizações instantâneas

## 🛠 Tecnologias

- **Java 21**
- **Spring Boot 3.5**
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - ORM e acesso a dados
- **Spring WebSocket** - Comunicação em tempo real
- **PostgreSQL** - Banco de dados principal
- **Flyway** - Migração de banco de dados
- **Redis** - Cache (desabilitado temporariamente)
- **RabbitMQ** - Mensageria (desabilitado temporariamente)
- **Twilio SDK** - Integração WhatsApp
- **Lombok** - Redução de boilerplate
- **Spring Boot Actuator** - Monitoramento
- **Prometheus** - Métricas

## 📁 Estrutura do Projeto

```
api/
├── src/main/java/com/ruby/rubia_server/
│   ├── RubiaChatServerApplication.java     # Classe principal
│   ├── config/
│   │   └── SecurityConfig.java             # Configuração de segurança
│   ├── core/                               # Módulo principal de negócio
│   │   ├── controller/                     # Controladores REST
│   │   │   ├── ConversationController.java
│   │   │   ├── CustomerController.java
│   │   │   ├── DepartmentController.java
│   │   │   ├── MessageController.java
│   │   │   └── UserController.java
│   │   ├── dto/                           # Data Transfer Objects
│   │   │   ├── ConversationDTO.java
│   │   │   ├── CreateUserDTO.java
│   │   │   └── ...
│   │   ├── entity/                        # Entidades JPA
│   │   │   ├── Company.java
│   │   │   ├── User.java
│   │   │   ├── Department.java
│   │   │   ├── Customer.java
│   │   │   ├── Conversation.java
│   │   │   └── Message.java
│   │   ├── enums/                         # Enumerações
│   │   │   ├── UserRole.java
│   │   │   ├── ConversationStatus.java
│   │   │   └── ...
│   │   ├── repository/                    # Repositórios JPA
│   │   └── service/                       # Serviços de negócio
│   └── messaging/                         # Módulo de mensageria
│       ├── adapter/                       # Adaptadores para provedores
│       │   ├── MessagingAdapter.java
│       │   └── impl/
│       │       ├── MockAdapter.java
│       │       └── TwilioAdapter.java
│       ├── config/
│       ├── controller/
│       ├── model/
│       └── service/
├── src/main/resources/
│   ├── application.properties             # Configurações principais
│   ├── application-docker.properties      # Configurações para Docker
│   └── db/migration/                      # Scripts Flyway
└── src/test/                             # Testes unitários e integração
```

## ⚙️ Configuração

### Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto `/api/` com as seguintes variáveis:

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/rubia_chat
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha

# WhatsApp (Twilio)
WHATSAPP_ACCOUNT_ID=your_twilio_account_sid
WHATSAPP_AUTH_TOKEN=your_twilio_auth_token
WHATSAPP_PHONE_NUMBER=whatsapp:+5511999999999
WHATSAPP_API_URL=https://api.twilio.com
```

### Profiles Disponíveis

- **default**: Configuração local de desenvolvimento
- **docker**: Configuração para execução em containers
- **whatsapp-cloud**: Configuração para WhatsApp Cloud API

## 🚀 Instalação e Execução

### Pré-requisitos

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- (Opcional) Docker e Docker Compose

### Execução Local

1. Clone o repositório
2. Configure o arquivo `.env`
3. Execute o PostgreSQL localmente
4. Compile e execute:

```bash
# Compilar
./mvnw clean compile

# Executar testes
./mvnw test

# Executar aplicação
./mvnw spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`

### Execução com Docker

```bash
# Na raiz do projeto
docker-compose up -d
```

## 🗄️ Banco de Dados

### Migrações Flyway

As migrações estão em `src/main/resources/db/migration/`:

- **V1**: Criação da tabela departments
- **V2**: Criação da tabela users
- **V3**: Criação da tabela customers
- **V4**: Criação da tabela conversations
- **V5**: Criação da tabela messages
- **V6**: Adição de campos WhatsApp nos usuários
- **V7**: Adição do campo owner_user nas conversas
- **V8**: Criação da tabela companies (multi-tenant)
- **V9**: Adição de company_id em todas as tabelas

### Índices e Performance

- Índices em campos de busca frequente
- Full-text search nas mensagens
- Triggers para atualização automática de timestamps

## 🏗️ Entidades

### Company (Multi-tenant)
```java
- UUID id
- String name
- String domain
- LocalDateTime createdAt/updatedAt
```

### User (Usuários do sistema)
```java
- UUID id
- String name, email
- String passwordHash
- UserRole role (ADMIN, SUPERVISOR, AGENT)
- Department department
- Company company
- String whatsappNumber
- Boolean isOnline, isWhatsappActive
- LocalDateTime lastSeen, createdAt, updatedAt
```

### Department (Departamentos)
```java
- UUID id
- String name, description
- Boolean autoAssign
- Company company
- LocalDateTime createdAt, updatedAt
```

### Customer (Clientes)
```java
- UUID id
- String name, phone, email
- Boolean isBlocked
- Company company
- LocalDateTime createdAt, updatedAt
```

### Conversation (Conversas)
```java
- UUID id
- Customer customer
- Department department
- User ownerUser
- ConversationChannel channel (WHATSAPP, WEBCHAT)
- ConversationStatus status (WAITING, IN_PROGRESS, CLOSED)
- String subject
- Company company
- LocalDateTime createdAt, updatedAt
```

### Message (Mensagens)
```java
- UUID id
- Conversation conversation
- String content
- MessageType type (TEXT, IMAGE, DOCUMENT, etc.)
- SenderType senderType (CUSTOMER, AGENT, SYSTEM)
- User sender
- MessageStatus status (SENT, DELIVERED, READ)
- String externalMessageId
- Company company
- LocalDateTime createdAt
```

## 🔌 APIs Disponíveis

### Endpoints Principais

#### Users
- `POST /api/users` - Criar usuário
- `GET /api/users` - Listar usuários
- `GET /api/users/{id}` - Buscar usuário
- `PUT /api/users/{id}` - Atualizar usuário
- `DELETE /api/users/{id}` - Deletar usuário

#### Departments
- `POST /api/departments` - Criar departamento
- `GET /api/departments` - Listar departamentos
- `GET /api/departments/{id}` - Buscar departamento
- `PUT /api/departments/{id}` - Atualizar departamento

#### Customers
- `POST /api/customers` - Criar cliente
- `GET /api/customers` - Listar clientes
- `GET /api/customers/{id}` - Buscar cliente
- `PUT /api/customers/{id}` - Atualizar cliente

#### Conversations
- `POST /api/conversations` - Criar conversa
- `GET /api/conversations` - Listar conversas
- `GET /api/conversations/{id}` - Buscar conversa
- `PUT /api/conversations/{id}` - Atualizar conversa
- `POST /api/conversations/{id}/assign` - Atribuir conversa

#### Messages
- `POST /api/messages` - Enviar mensagem
- `GET /api/conversations/{conversationId}/messages` - Listar mensagens
- `PUT /api/messages/{id}` - Atualizar mensagem

#### Messaging (WhatsApp)
- `POST /messaging/webhook` - Webhook para receber mensagens
- `POST /messaging/send` - Enviar mensagem via WhatsApp

### Exemplo de Uso

#### Criar um usuário:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@empresa.com",
    "password": "senha123",
    "companyId": "550e8400-e29b-41d4-a716-446655440000",
    "role": "AGENT"
  }'
```

## 📱 Integração com WhatsApp

### Provedores Suportados

O sistema usa o padrão Adapter para suportar múltiplos provedores:

- **Twilio** (implementado)
- **WhatsApp Cloud API** (configurável)
- **Mock** (para desenvolvimento)

### Configuração Twilio

1. Configure as variáveis de ambiente com suas credenciais Twilio
2. Configure o webhook no Twilio Console: `https://seudominio.com/messaging/webhook`
3. Configure o provider no `application.properties`: `messaging.provider=twilio`

### Fluxo de Mensagens

1. Cliente envia mensagem WhatsApp
2. Twilio chama webhook `/messaging/webhook`
3. Sistema cria/atualiza conversa e mensagem
4. Agente responde via interface web
5. Sistema envia resposta via Twilio

## 🧪 Testes

### Executar Testes

```bash
# Todos os testes
./mvnw test

# Testes específicos
./mvnw test -Dtest=UserServiceTest
```

### Cobertura de Testes

- Testes unitários para serviços
- Testes de integração para controladores
- Testes de repositório com banco H2
- Mocks para integrações externas

## 🐳 Docker

### Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Docker Compose

O projeto inclui `docker-compose.yml` na raiz com:
- PostgreSQL
- Redis
- RabbitMQ
- API Spring Boot
- Frontend React

## 📊 Monitoramento

### Actuator Endpoints

- `/actuator/health` - Status da aplicação
- `/actuator/metrics` - Métricas Prometheus
- `/actuator/info` - Informações da aplicação

### Logs

A aplicação usa SLF4J com Logback para logging estruturado.

## 🔐 Segurança

- Autenticação baseada em JWT (configurável)
- Autorização por roles (ADMIN, SUPERVISOR, AGENT)
- Isolamento multi-tenant por company_id
- Validação de entrada com Bean Validation
- Proteção CSRF desabilitada para APIs REST

## 🚧 Desenvolvimento

### Convenções

- Package naming: `com.ruby.rubia_server`
- Uso de Lombok para reduzir boilerplate
- DTOs para todas as operações de API
- Separação clara entre camadas (Controller → Service → Repository)
- Testes unitários obrigatórios para novos serviços

### Próximos Passos

- [ ] Implementar autenticação JWT completa
- [ ] Ativar Redis para cache
- [ ] Ativar RabbitMQ para mensageria assíncrona
- [ ] Implementar WebSockets para tempo real
- [ ] Adicionar rate limiting
- [ ] Implementar audit logs
- [ ] Adicionar métricas customizadas

## 🤝 Contribuição

1. Faça fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está sob licença privada da Ruby Tech.