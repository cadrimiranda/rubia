# Integração Frontend ↔ Backend - Rubia Chat

## Análise da Estrutura Atual

**Frontend (React 19 + TypeScript + Zustand):**
- Store: `useChatStore.ts` (estado global com Zustand)
- Types: `index.ts` (interfaces Chat, Message, User, Tag)
- Mocks: `data.ts` (dados de desenvolvimento)
- Components: Chat UI totalmente implementado
- Status workflow: entrada → esperando → finalizados

**Backend (Spring Boot + JPA):**
- ✅ Entities: User, Customer, Conversation, Message, Department
- ✅ DTOs: Completos para todas as entidades
- ✅ Controllers: APIs REST implementadas
- ✅ Services: Business logic implementada
- ✅ Repositories: Queries customizadas
- ✅ Enums: Status, types, roles

## Estratégia de Integração

**Fases de integração:**
1. **API Service Layer** - Cliente HTTP + configuração
2. **Data Mapping** - Adaptadores Frontend ↔ Backend  
3. **Store Integration** - Substituir mocks por APIs
4. **Real-time** - WebSocket + eventos
5. **Authentication** - Login + JWT + proteção de rotas

---

## FASE 1: API SERVICE LAYER

### 1.1 Configuração Base da API
**Prioridade: ALTA**

#### 1.1.1 Criar cliente HTTP base
- [x] Criar `src/api/client.ts` - configuração Axios/Fetch
- [x] Configurar base URL, interceptors, headers
- [x] Implementar tratamento de erro global
- [x] Adicionar timeout e retry logic

#### 1.1.2 Criar tipos de API
- [x] Criar `src/api/types.ts` - tipos de request/response
- [x] Implementar interfaces de paginação
- [x] Criar tipos de erro da API
- [x] Definir tipos de autenticação

#### 1.1.3 Criar serviços de API
- [x] Criar `src/api/services/conversationApi.ts`
- [x] Criar `src/api/services/messageApi.ts`
- [x] Criar `src/api/services/customerApi.ts`
- [x] Criar `src/api/services/userApi.ts`
- [x] Criar `src/api/services/departmentApi.ts`

#### 1.1.4 Implementar ConversationAPI
- [x] `GET /api/conversations` - listar por status
- [x] `GET /api/conversations/{id}` - buscar por ID
- [x] `POST /api/conversations` - criar nova conversa
- [x] `PUT /api/conversations/{id}/status` - mudar status
- [x] `PUT /api/conversations/{id}/assign` - atribuir agente
- [x] `PUT /api/conversations/{id}/pin` - fixar conversa

#### 1.1.5 Implementar MessageAPI
- [x] `GET /api/conversations/{id}/messages` - listar mensagens
- [x] `POST /api/conversations/{id}/messages` - enviar mensagem
- [x] `PUT /api/messages/{id}/read` - marcar como lida
- [x] `PUT /api/messages/{id}/delivered` - marcar como entregue
- [x] `GET /api/messages/search` - busca full-text

#### 1.1.6 Implementar CustomerAPI
- [x] `GET /api/customers` - listar clientes
- [x] `GET /api/customers/{id}` - buscar cliente
- [x] `POST /api/customers` - criar cliente
- [x] `PUT /api/customers/{id}` - atualizar cliente
- [x] `PUT /api/customers/{id}/block` - bloquear cliente

---

## FASE 2: DATA MAPPING

### 2.1 Adaptadores de Dados
**Prioridade: ALTA**

#### 2.1.1 Criar adaptadores de conversão
- [x] Criar `src/adapters/conversationAdapter.ts`
- [x] Criar `src/adapters/messageAdapter.ts`
- [x] Criar `src/adapters/customerAdapter.ts`
- [x] Criar `src/adapters/tagAdapter.ts`

#### 2.1.2 Implementar ConversationAdapter
```typescript
// Backend DTO → Frontend interface
- ConversationDTO → Chat
- CustomerDTO → User (contact)
- MessageDTO → Message
- Mapear relacionamentos (customer, assignedUser, tags)
```

#### 2.1.3 Implementar MessageAdapter
```typescript
// Backend DTO → Frontend interface
- MessageDTO → Message
- Mapear senderType (CUSTOMER/AGENT/AI) → isFromUser
- Converter timestamps para Date objects
- Mapear messageType e status
```

#### 2.1.4 Implementar CustomerAdapter
```typescript
// Backend DTO → Frontend interface
- CustomerDTO → User
- Mapear phone, whatsappId, profileUrl → avatar
- Converter isBlocked para lógica frontend
```

#### 2.1.5 Implementar validações
- [x] Validar dados antes de enviar para API
- [x] Sanitizar inputs de usuário
- [x] Validar formato de telefone brasileiro
- [x] Validar tamanho de mensagens

---

## FASE 3: STORE INTEGRATION

### 3.1 Atualizar Store do Zustand
**Prioridade: ALTA**

#### 3.1.1 Modificar useChatStore
- [x] Substituir mock data por chamadas de API
- [x] Implementar loading states
- [x] Adicionar error handling
- [x] Implementar cache local

#### 3.1.2 Implementar carregamento de conversas
```typescript
loadConversations: async (status: ChatStatus) => {
  set({ isLoading: true })
  try {
    const conversations = await conversationApi.getByStatus(status)
    const chats = conversations.map(conversationAdapter.toChat)
    set({ chats, isLoading: false })
  } catch (error) {
    set({ error, isLoading: false })
  }
}
```

#### 3.1.3 Implementar envio de mensagens
```typescript
sendMessage: async (chatId: string, content: string) => {
  // Otimistic update
  const tempMessage = createTempMessage(content)
  addTempMessage(chatId, tempMessage)
  
  try {
    const message = await messageApi.send(chatId, content)
    replaceTempMessage(chatId, tempMessage.id, message)
  } catch (error) {
    removeTempMessage(chatId, tempMessage.id)
    showError(error)
  }
}
```

#### 3.1.4 Implementar ações de conversa
- [x] `assignToAgent()` - atribuir agente
- [x] `changeStatus()` - mudar status da conversa
- [x] `pinConversation()` - fixar/desfixar
- [x] `blockCustomer()` - bloquear cliente
- [x] `addTag()` / `removeTag()` - gerenciar tags

#### 3.1.5 Implementar busca
- [x] `searchConversations()` - busca local + servidor
- [x] `searchMessages()` - busca full-text no backend
- [x] Debounce para otimizar performance
- [x] Cache de resultados de busca

#### 3.1.6 Implementar paginação
- [x] `loadMoreConversations()` - carregamento incremental
- [x] `loadMoreMessages()` - histórico de mensagens
- [x] Infinite scroll para mensagens antigas
- [ ] Virtual scrolling para performance

---

## FASE 4: REAL-TIME

### 4.1 WebSocket Integration
**Prioridade: MÉDIA**

#### 4.1.1 Configurar WebSocket client
- [ ] Criar `src/websocket/client.ts`
- [ ] Implementar conexão automática
- [ ] Adicionar reconnect logic
- [ ] Implementar heartbeat/ping

#### 4.1.2 Implementar event handlers
```typescript
// Eventos do WebSocket
- NEW_MESSAGE → adicionar à conversa ativa
- MESSAGE_STATUS_UPDATED → atualizar status
- CONVERSATION_ASSIGNED → atualizar agente
- CONVERSATION_STATUS_CHANGED → mover entre abas
- USER_TYPING → mostrar indicador
```

#### 4.1.3 Integrar com store
- [ ] Conectar eventos WebSocket ao Zustand store
- [ ] Atualizar conversas em tempo real
- [ ] Mostrar notificações para novas mensagens
- [ ] Sincronizar estado entre múltiplas abas

#### 4.1.4 Implementar typing indicators
- [ ] Enviar eventos de digitação
- [ ] Mostrar indicador "está digitando"
- [ ] Timeout automático de typing
- [ ] Throttle de eventos de digitação

---

## FASE 5: AUTHENTICATION

### 5.1 Autenticação e Autorização
**Prioridade: ALTA**

#### 5.1.1 Implementar login/logout
- [ ] Criar `src/auth/authService.ts`
- [ ] Implementar login com email/senha
- [ ] Armazenar JWT token (localStorage/httpOnly cookie)
- [ ] Implementar logout e clear token

#### 5.1.2 Criar auth store
- [ ] Criar `src/store/useAuthStore.ts`
- [ ] Gerenciar estado do usuário logado
- [ ] Verificar token válido
- [ ] Refresh token automático

#### 5.1.3 Implementar proteção de rotas
- [ ] Criar `src/components/ProtectedRoute.tsx`
- [ ] Redirecionar para login se não autenticado
- [ ] Verificar permissões por role (admin/agent)
- [ ] Implementar route guards

#### 5.1.4 Configurar interceptors
- [ ] Adicionar Authorization header automaticamente
- [ ] Interceptar respostas 401/403
- [ ] Renovar token automaticamente
- [ ] Logout automático em token inválido

#### 5.1.5 Implementar context de usuário
- [ ] Mostrar dados do agente logado
- [ ] Implementar mudança de status online/offline
- [ ] Sync com backend do status do usuário
- [ ] Mostrar permissões e departamento

---

## FASE 6: UX ENHANCEMENTS

### 6.1 Loading States e Error Handling
**Prioridade: MÉDIA**

#### 6.1.1 Implementar estados de loading
- [ ] Skeletons para carregamento de conversas
- [ ] Loading spinners para ações
- [ ] Progress bars para uploads
- [ ] Shimmer effects para mensagens

#### 6.1.2 Implementar error handling
- [ ] Toast notifications para erros
- [ ] Retry buttons para falhas de rede
- [ ] Offline indicators
- [ ] Error boundaries para crashes

#### 6.1.3 Implementar feedback visual
- [ ] Confirmar ações importantes
- [ ] Mostrar status de conexão
- [ ] Indicadores de sync
- [ ] Badges de contadores

### 6.2 Otimizações de Performance
**Prioridade: BAIXA**

#### 6.2.1 Implementar cache inteligente
- [ ] Cache de conversas recentes
- [ ] Cache de mensagens por conversa
- [ ] Invalidação automática de cache
- [ ] Storage local para offline

#### 6.2.2 Implementar virtual scrolling
- [ ] Lista de conversas virtualizada
- [ ] Histórico de mensagens virtualizado
- [ ] Lazy loading de imagens
- [ ] Debounce de scroll

---

## MAPEAMENTOS PRINCIPAIS

### Frontend ↔ Backend Data Mapping

```typescript
// Conversation mapping
Frontend Chat {
  id: string
  contact: User              ← CustomerDTO
  messages: Message[]        ← MessageDTO[]
  status: ChatStatus         ← ConversationStatus enum
  assignedAgent: string      ← assignedUser.name
  tags: Tag[]               ← tags via conversation_tags
  isPinned: boolean         ← isPinned
  unreadCount: number       ← calculated client-side
}

// Message mapping
Frontend Message {
  id: string                ← id
  content: string           ← content
  timestamp: Date           ← createdAt
  senderId: string          ← senderId OR customerId
  messageType: string       ← messageType enum
  status: MessageStatus     ← status enum
  isFromUser: boolean       ← calculated from senderType
}

// Customer mapping (frontend User interface)
Frontend User {
  id: string                ← id
  name: string              ← name
  avatar: string            ← profileUrl OR default
  isOnline: boolean         ← calculated/cached
  phone: string             ← phone
}
```

### API Endpoints Mapping

```typescript
// Conversations
GET /api/conversations?status=entrada    → useChatStore.chats (entrada)
GET /api/conversations?status=esperando  → useChatStore.chats (esperando) 
GET /api/conversations?status=finalizados → useChatStore.chats (finalizados)
PUT /api/conversations/{id}/status       → changeStatus()
PUT /api/conversations/{id}/assign       → assignToAgent()
PUT /api/conversations/{id}/pin          → pinConversation()

// Messages
GET /api/conversations/{id}/messages     → chat.messages
POST /api/conversations/{id}/messages    → sendMessage()
PUT /api/messages/{id}/read              → markAsRead()

// Search
GET /api/conversations/search?q=term     → searchConversations()
GET /api/messages/search?q=term          → searchMessages()
```

---

## CONFIGURAÇÃO DE DESENVOLVIMENTO

### 7.1 Environment Setup
**Prioridade: ALTA**

#### 7.1.1 Configurar variáveis de ambiente
- [x] Criar `.env.local` com URL da API
- [ ] Configurar proxy do Vite para desenvolvimento
- [ ] Implementar different configs para dev/prod
- [ ] Configurar CORS no backend para frontend

#### 7.1.2 Implementar modo de desenvolvimento
- [ ] Flag para usar mocks vs API real
- [ ] Hot reload de configurações
- [ ] Debug logs para requests
- [ ] Mock de WebSocket para desenvolvimento

#### 7.1.3 Testes de integração
- [ ] Testes E2E com Cypress/Playwright
- [ ] Testes de API com MSW (Mock Service Worker)
- [ ] Testes de componentes integrados
- [ ] Testes de WebSocket

---

## RESUMO DE EXECUÇÃO

**Total de tarefas: 80+**

**Prioridade ALTA (MVP):**
1. API Service Layer (6 subtarefas)
2. Data Mapping (5 subtarefas)
3. Store Integration (6 subtarefas)
4. Authentication (5 subtarefas)
5. Environment Setup (3 subtarefas)

**Prioridade MÉDIA (importante):**
- Real-time WebSocket (4 subtarefas)
- UX Enhancements (3 subtarefas)

**Prioridade BAIXA (nice to have):**
- Performance Optimizations (2 subtarefas)
- Advanced Testing (3 subtarefas)

**Estimativa de tempo:**
- ALTA: 1-2 sprints (MVP funcional)
- MÉDIA: +1 sprint (experiência completa)
- BAIXA: +0.5 sprint (polimento)

**Ordem de implementação recomendada:**
1. API Client + Adapters (base)
2. Store Integration (funcionalidade core)
3. Authentication (segurança)
4. WebSocket (tempo real)
5. UX enhancements (polimento)