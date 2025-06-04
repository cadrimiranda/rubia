# WhatsApp POC - Implementação Simples para HOJE

## Overview
POC simples de WhatsApp com Twilio - saindo hoje! MVC básico, sem over-engineering.

---

## Tasks - HOJE (3-4 horas total)

### Task 1: Basic Setup (30 min)
1. **Add Twilio dependency** - `pom.xml`
2. **Basic config** - `application.properties`

### Task 2: Simple Entity (30 min)
1. **Message entity** básica com JPA
2. **Repository** simples
3. **Migration** SQL básica

### Task 3: Twilio Service (1 hora)
1. **WhatsAppService** com Twilio client
2. **sendMessage()** method direto
3. **Basic error handling**

### Task 4: Controller (45 min)
1. **MessageController** REST básico
2. **POST /send-message** endpoint
3. **Request/Response** DTOs simples

### Task 5: Test Integration (45 min)  
1. **Test manual** com Postman
2. **Fix bugs** encontrados
3. **Documentation** básica

---

## Arquitetura Simples

```
Controller -> Service -> Twilio API
     ↓
   Database (Message log)
```

**Sem**:
- Adapter pattern complexo
- Circuit breakers
- WebSockets
- Webhooks
- Testes unitários extensivos

**Com**:
- CRUD básico
- Twilio integration direta
- Log de mensagens
- Validation simples

---

## Files to Create

1. **Message.java** - Entity básica
2. **MessageRepository.java** - JPA repository  
3. **WhatsAppService.java** - Twilio integration
4. **MessageController.java** - REST API
5. **SendMessageRequest.java** - DTO
6. **V1__create_messages.sql** - Migration

**Timeline**: 3-4 horas - SAIRÁ HOJE!

Posso começar agora?