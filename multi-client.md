# Multi-Client WhatsApp Integration

## Visão Geral

Este documento descreve a implementação de um sistema multi-cliente onde cada departamento pode ter vários usuários, e cada usuário pode ter seu próprio número WhatsApp e gerenciar suas próprias conversas.

## Arquitetura

### Estrutura de Entidades

```
Department (1) -> (N) User (1) -> (N) Conversation (1) -> (N) Message
                      |                    |
                      +--------------------+
                      ownerUser relationship
```

### Relacionamentos

- **Department**: Pode ter múltiplos usuários
- **User**: Pertence a um departamento, pode ter número WhatsApp próprio
- **Conversation**: Tem um `ownerUser` (quem recebe as mensagens) e um `assignedUser` (quem está atendendo)
- **Message**: Vinculada à conversação

## Implementação

### 1. Modificações nas Entidades

#### User.java
```java
@Column(name = "whatsapp_number", unique = true)
private String whatsappNumber;

@Column(name = "is_whatsapp_active")
@Builder.Default
private Boolean isWhatsappActive = false;
```

#### Department.java
```java
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<User> users;
```

#### Conversation.java
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_user_id")
private User ownerUser;
```

### 2. Migrations

- **V6**: Adiciona campos WhatsApp aos usuários
- **V7**: Adiciona owner_user_id às conversações

### 3. TwilioAdapter Atualizado

Métodos sobregregados para permitir especificar o número de origem:
- `sendMessage(to, message, fromNumber)`
- `sendMediaMessage(to, mediaUrl, caption, fromNumber)`

## Configuração

### Variáveis de Ambiente (.env)

```bash
# Twilio Configuration
MESSAGING_PROVIDER=twilio
TWILIO_ACCOUNT_SID=your_account_sid_here
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_PHONE_NUMBER=whatsapp:+5511999999999
TWILIO_API_URL=https://api.twilio.com
```

### Configuração de Múltiplos Números

Para cada usuário que terá WhatsApp:

1. **Cadastrar usuário** com `whatsappNumber` preenchido
2. **Ativar WhatsApp** definindo `isWhatsappActive = true`
3. **Configurar webhook** no Twilio para o número específico

## Fluxo de Implementação

### Passo 1: Configurar Usuários
```sql
UPDATE users 
SET whatsapp_number = 'whatsapp:+5511999999999', 
    is_whatsapp_active = true 
WHERE email = 'usuario@empresa.com';
```

### Passo 2: Configurar Webhooks no Twilio
Para cada número WhatsApp, configurar:
- **URL**: `https://sua-api.com/api/messaging/webhook/incoming`
- **Método**: POST

### Passo 3: Roteamento de Mensagens
O sistema identifica o `ownerUser` baseado no número de destino da mensagem:

1. Mensagem chega via webhook
2. Sistema identifica o `User` pelo `whatsappNumber`
3. Cria/encontra `Conversation` com `ownerUser`
4. Se necessário, atribui a conversa (`assignedUser`)

### Passo 4: Envio de Mensagens
Ao enviar mensagens, usar o número WhatsApp do `ownerUser`:

```java
twilioAdapter.sendMessage(
    customerPhone, 
    messageContent, 
    conversation.getOwnerUser().getWhatsappNumber()
);
```

## Considerações de Segurança

1. **Isolamento**: Cada usuário só vê conversas onde é `ownerUser` ou `assignedUser`
2. **Autenticação**: Validar se usuário pode acessar a conversação
3. **Webhooks**: Implementar validação de assinatura do Twilio

## Próximos Passos

1. **Atualizar Services** para considerar `ownerUser`
2. **Modificar Controllers** para filtrar por usuário logado
3. **Atualizar Frontend** para mostrar apenas conversas do usuário
4. **Implementar roteamento automático** baseado no número de destino
5. **Criar interface** para gerenciar números WhatsApp por usuário

## Exemplo de Uso

```java
// Usuário A tem whatsappNumber = "whatsapp:+5511111111111"
// Usuário B tem whatsappNumber = "whatsapp:+5511222222222"

// Cliente envia mensagem para +5511111111111
// Sistema cria conversação com ownerUser = Usuário A

// Cliente envia mensagem para +5511222222222  
// Sistema cria conversação com ownerUser = Usuário B

// Cada usuário vê apenas suas próprias conversas
```