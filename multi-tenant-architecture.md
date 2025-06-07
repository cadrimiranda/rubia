# Arquitetura Multi-Tenant - Rubia Chat

## Visão Geral

Sistema multi-tenant onde cada empresa cliente possui seus próprios departamentos, usuários e números WhatsApp isolados.

## Estrutura Hierárquica

```
Company (Sua Empresa Cliente)
  ├── Department 1 (Vendas)
  │   ├── User A (whatsapp: +5511111111111)
  │   └── User B (whatsapp: +5511222222222)
  ├── Department 2 (Suporte)
  │   ├── User C (whatsapp: +5511333333333)
  │   └── User D (whatsapp: +5511444444444)
  └── Conversations/Messages (isoladas por company_id)
```

## Entidades Modificadas

### Company
```java
@Entity
public class Company {
    private UUID id;
    private String name;
    private String slug;            // Identificador único (ex: "empresa-abc")
    private String planType;        // BASIC, PRO, ENTERPRISE
    private Integer maxUsers;       // Limite de usuários
    private Integer maxWhatsappNumbers; // Limite de números WhatsApp
    private Boolean isActive;
    private List<Department> departments;
}
```

### Department
```java
@Entity
public class Department {
    private UUID id;
    private String name;
    @ManyToOne
    private Company company;        // Nova: Vinculação à empresa
    private List<User> users;
}
```

### User
```java
@Entity
public class User {
    private UUID id;
    private String email;
    @ManyToOne
    private Company company;        // Nova: Vinculação à empresa
    @ManyToOne
    private Department department;
    private String whatsappNumber;  // Número WhatsApp do usuário
    private Boolean isWhatsappActive;
}
```

### Conversation
```java
@Entity
public class Conversation {
    private UUID id;
    @ManyToOne
    private Company company;        // Nova: Isolamento por empresa
    @ManyToOne
    private User ownerUser;         // Usuário dono do número WhatsApp
    @ManyToOne
    private User assignedUser;      // Usuário atendendo a conversa
}
```

## Migrations Criadas

### V8: Criação da tabela companies
- Estrutura base da empresa
- Campos de configuração (plano, limites)
- Índices para performance

### V9: Adição de company_id em todas as tabelas
- Isolamento multi-tenant
- Foreign keys para integridade
- Constraints unique por empresa
- Índices para performance

## Isolamento de Dados

### Repositories Atualizados
```java
// UserRepository - Métodos por empresa
List<User> findByCompanyId(UUID companyId);
Optional<User> findByEmailAndCompanyId(String email, UUID companyId);
Optional<User> findByWhatsappNumberAndCompanyId(String whatsappNumber, UUID companyId);
List<User> findByIsWhatsappActiveTrueAndCompanyId(UUID companyId);
```

### Services Multi-Tenant
```java
// CompanyService - Gerenciamento de empresas
Company create(Company company);
List<Company> findActiveCompanies();
Optional<Company> findBySlug(String slug);

// MessagingService - Envio por usuário específico
MessageResult sendMessage(String to, String message, UUID companyId, UUID userId);
User findUserByWhatsappNumber(String whatsappNumber, UUID companyId);
```

## Fluxo de Implementação

### 1. Configuração de Empresa
```sql
-- Criar empresa cliente
INSERT INTO companies (name, slug, plan_type, max_users, max_whatsapp_numbers) 
VALUES ('Empresa ABC', 'empresa-abc', 'PRO', 50, 10);
```

### 2. Configuração de Departamentos
```sql
-- Criar departamentos da empresa
INSERT INTO departments (name, company_id) 
VALUES ('Vendas', 'uuid-da-empresa'), ('Suporte', 'uuid-da-empresa');
```

### 3. Configuração de Usuários
```sql
-- Criar usuários com números WhatsApp
INSERT INTO users (name, email, company_id, department_id, whatsapp_number, is_whatsapp_active) 
VALUES 
  ('João Vendas', 'joao@empresa.com', 'uuid-empresa', 'uuid-vendas', 'whatsapp:+5511111111111', true),
  ('Maria Suporte', 'maria@empresa.com', 'uuid-empresa', 'uuid-suporte', 'whatsapp:+5511222222222', true);
```

### 4. Configuração de Webhooks Twilio
Para cada número WhatsApp da empresa:
- **URL**: `https://sua-api.com/api/messaging/webhook/incoming`
- **Método**: POST
- Sistema identifica empresa pelo número de destino

## Roteamento de Mensagens

### Fluxo de Entrada (Webhook)
1. Cliente envia mensagem para `+5511111111111`
2. Webhook Twilio recebido
3. Sistema identifica:
   - `User` pelo `whatsappNumber`
   - `Company` via `user.company`
4. Cria/encontra `Conversation` com `ownerUser` e `company`
5. Cria `Message` isolada por `company_id`

### Fluxo de Saída
```java
// Envio usando número específico do usuário
messagingService.sendMessage(
    customerPhone, 
    messageContent, 
    conversation.getCompany().getId(),
    conversation.getOwnerUser().getId()
);
```

## Segurança e Isolamento

### Filtros por Empresa
- Todos os queries incluem `company_id`
- Controllers verificam acesso por empresa
- JWT tokens incluem `company_id`

### Constraints de Integridade
- Phone único por empresa: `(phone, company_id)`
- WhatsApp único por empresa: `(whatsapp_number, company_id)`
- Cascade delete: excluir empresa remove todos os dados

## Configuração Multi-Tenant

### Identificação da Empresa
```java
// Por slug na URL
GET /api/empresa-abc/conversations

// Por header
Authorization: Bearer jwt-token-with-company-id

// Por subdomain
empresa-abc.rubia.com.br
```

### Limites por Plano
```java
@Service
public class CompanyLimitService {
    public boolean canAddUser(UUID companyId) {
        Company company = companyRepository.findById(companyId);
        long currentUsers = userRepository.countByCompanyId(companyId);
        return currentUsers < company.getMaxUsers();
    }
    
    public boolean canAddWhatsappNumber(UUID companyId) {
        Company company = companyRepository.findById(companyId);
        long currentNumbers = userRepository.countByCompanyIdAndIsWhatsappActiveTrue(companyId);
        return currentNumbers < company.getMaxWhatsappNumbers();
    }
}
```

## Próximos Passos

1. **Autenticação**: JWT com `company_id`
2. **Controllers**: Filtros por empresa
3. **Frontend**: Interface por empresa
4. **Billing**: Cobrança por plano/uso
5. **Analytics**: Métricas isoladas por empresa
6. **Backup**: Estratégia por tenant

## Exemplo Prático

### Empresa "TechCorp" (slug: techcorp)
- **Vendas**: João (+5511111111111), Maria (+5511222222222)
- **Suporte**: Carlos (+5511333333333)

### Empresa "ComercialX" (slug: comercialx)  
- **Atendimento**: Ana (+5511444444444)
- **Financeiro**: Pedro (+5511555555555)

Cada empresa vê apenas suas próprias conversas e dados, completamente isoladas.