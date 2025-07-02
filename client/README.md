# Rubia Chat - Frontend

Sistema de chat corporativo em tempo real construído com React 19, TypeScript e Vite.

## 📋 Índice

- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Arquitetura](#arquitetura)
- [Configuração e Instalação](#configuração-e-instalação)
- [Desenvolvimento](#desenvolvimento)
- [Sistema de Autenticação](#sistema-de-autenticação)
- [Comunicação em Tempo Real (WebSocket)](#comunicação-em-tempo-real-websocket)
- [Gerenciamento de Estado](#gerenciamento-de-estado)
- [Componentes](#componentes)
- [Sistema de Notificações](#sistema-de-notificações)
- [Sistema de Tratamento de Erros](#sistema-de-tratamento-de-erros)
- [UX/UI Enhancements](#uxui-enhancements)
- [APIs e Adaptadores](#apis-e-adaptadores)
- [Build e Deploy](#build-e-deploy)

## 🛠 Tecnologias

### Core
- **React 19** - Framework frontend
- **TypeScript** - Tipagem estática
- **Vite** - Build tool e dev server

### UI/UX
- **Ant Design 5.25+** - Biblioteca de componentes
- **Tailwind CSS** - Framework CSS utilitário
- **Lucide React** - Ícones

### Estado e Dados
- **Zustand** - Gerenciamento de estado
- **Fetch API** - Requisições HTTP

### Tempo Real
- **WebSocket** - Comunicação bidirecional em tempo real
- **Custom Event System** - Sistema de eventos personalizados

### Qualidade de Código
- **ESLint** - Linting
- **TypeScript Compiler** - Verificação de tipos

## 📁 Estrutura do Projeto

```
src/
├── adapters/           # Adaptadores para transformação de dados
│   ├── conversationAdapter.ts
│   ├── customerAdapter.ts
│   ├── messageAdapter.ts
│   └── index.ts       # Barrel export
├── api/               # Configuração e serviços de API
│   ├── client.ts      # Cliente HTTP configurado
│   ├── services/      # Serviços específicos por entidade
│   │   ├── conversationApi.ts
│   │   ├── customerApi.ts
│   │   ├── departmentApi.ts
│   │   ├── messageApi.ts
│   │   └── userApi.ts
│   ├── types.ts       # Tipos da API
│   └── index.ts       # Barrel export
├── auth/              # Sistema de autenticação
│   └── authService.ts # Serviço de autenticação JWT
├── components/        # Componentes React organizados por funcionalidade
│   ├── AuthContext/   # Contexto de autenticação
│   │   └── index.tsx
│   ├── BloodCenterChat.tsx # Componente principal do chat para centro de sangue
│   ├── ChatHeader/    # Cabeçalho do chat com informações do doador
│   │   └── index.tsx
│   ├── ContextMenu/   # Menu de contexto com ações
│   │   └── index.tsx
│   ├── DonorInfoModal/ # Modal com informações detalhadas do doador
│   │   └── index.tsx
│   ├── DonorSidebar/  # Sidebar com lista de doadores/conversas
│   │   └── index.tsx
│   ├── ErrorBoundary/ # Error boundary para tratamento de erros
│   │   └── index.tsx
│   ├── FileAttachment/ # Componente para anexos de arquivo
│   │   └── index.tsx
│   ├── Message/       # Componente individual de mensagem
│   │   └── index.tsx
│   ├── MessageInput/  # Input de mensagem com botões de ação
│   │   └── index.tsx
│   ├── MessageList/   # Lista de mensagens da conversa
│   │   └── index.tsx
│   ├── NewChatModal/  # Modal para nova conversa/contato com abas
│   │   └── index.tsx
│   └── ProtectedRoute/ # Componente de rota protegida por autenticação
│       └── index.tsx
├── hooks/             # Custom hooks
│   └── useChatData.ts # Hook para dados do chat e notificações
├── pages/             # Páginas da aplicação
│   ├── ChatPage.tsx   # Página principal do chat
│   └── LoginPage.tsx  # Página de login
├── store/             # Gerenciamento de estado (Zustand)
│   ├── useAuthStore.ts # Store de autenticação
│   └── useChatStore.ts # Store do chat
├── types/             # Tipos TypeScript globais
│   ├── index.ts       # Tipos principais (Chat, User, Message, etc.)
│   └── types.ts       # Tipos específicos (Donor, FileAttachment, etc.)
├── utils/             # Utilitários e validações
│   ├── company.ts     # Utilitários da empresa
│   ├── format.ts      # Formatação de dados
│   ├── validation.ts  # Validações
│   └── index.ts       # Funções utilitárias gerais
├── websocket/         # Sistema WebSocket
│   ├── client.ts      # Cliente WebSocket
│   ├── eventHandlers.ts # Handlers de eventos
│   └── index.ts       # Manager principal
└── mocks/             # Dados de desenvolvimento
    └── data.ts        # Dados mock
```

### 🔄 Migração de Componentes (feat/ui_improvements)

A estrutura atual reflete a refatoração completa dos componentes para uma interface otimizada de **centro de doação de sangue**:

#### Componentes Principais Atuais
- **BloodCenterChat.tsx**: Componente raiz que substitui o antigo ChatPage
- **DonorSidebar/**: Sidebar especializada para doadores (substitui Sidebar genérico)
- **NewChatModal/**: Modal inteligente com abas para criação de contatos
- **DonorInfoModal/**: Modal com informações médicas dos doadores

#### Componentes de Mensagem
- **MessageList/**: Lista otimizada de mensagens
- **MessageInput/**: Input com funcionalidades específicas para centro médico
- **Message/**: Componente individual de mensagem com contexto médico

## 🏗 Arquitetura

### Fluxo de Dados
```
UI Components → Zustand Store → API Services → Backend
     ↑              ↓
WebSocket Client → Event Handlers → Store Updates
```

### Padrões Arquiteturais
1. **Single Source of Truth**: Zustand store centraliza todo estado da aplicação
2. **Adapter Pattern**: Transformação de dados entre frontend e backend
3. **Observer Pattern**: WebSocket eventos e notificações
4. **Component Composition**: Composição de componentes para reutilização
5. **Error Boundaries**: Tratamento de erros em diferentes níveis

## ⚙️ Configuração e Instalação

### Pré-requisitos
- Node.js 18+
- npm ou yarn

### Instalação
```bash
# Instalar dependências
npm install

# Configurar variáveis de ambiente
cp .env.example .env.local
```

### Variáveis de Ambiente
```env
# Configurações do ambiente
VITE_APP_TITLE=Rubia - Centro de Sangue

# Backend API
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws

# Mock Configuration
VITE_USE_MOCK_AUTH=true   # true = usar mock para login/logout
VITE_USE_MOCK_DATA=true   # true = usar mock para criação de usuários

# Development
VITE_DEBUG=true
```

### Modo Mock (Desenvolvimento)

O sistema oferece dois tipos de mock independentes:

**Mock de Autenticação (`VITE_USE_MOCK_AUTH=true`)**
- Login/logout sem backend
- **Credenciais disponíveis:**
  - **Admin**: `admin@centrodesangue.com` / `admin123`
  - **Supervisor**: `supervisor@centrodesangue.com` / `super123`
  - **Agente**: `agente@centrodesangue.com` / `agente123`

**Mock de Dados (`VITE_USE_MOCK_DATA=true`)**
- Criação de novos contatos/clientes sem backend
- Lista de clientes mockados para teste
- Validação de telefones e duplicatas

## 🚀 Desenvolvimento

### Comandos Disponíveis
```bash
# Desenvolvimento
npm run dev        # Inicia dev server

# Build
npm run build      # Build para produção
npm run preview    # Preview do build

# Qualidade
npm run lint       # ESLint
npm run type-check # TypeScript check
```

### Desenvolvimento Local
1. Inicie o backend (porta 8080)
2. Execute `npm run dev`
3. Acesse `http://localhost:5173`

## 🔐 Sistema de Autenticação

### Arquitetura JWT
```typescript
// authService.ts
class AuthService {
  // Login com credenciais
  async login(credentials: UserLoginDTO): Promise<AuthResponse>
  
  // Renovação automática de token
  async refreshToken(): Promise<AuthResponse>
  
  // Logout e limpeza
  logout(): void
  
  // Verificações
  isAuthenticated(): boolean
  getAccessToken(): string | null
}
```

### Store de Autenticação
```typescript
// useAuthStore.ts
interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
}

// Hooks utilitários
const isAuthenticated = useIsAuthenticated()
const currentUser = useCurrentUser()
const hasPermission = useHasPermission('ADMIN')
```

### Proteção de Rotas
```typescript
// ProtectedRoute.tsx
<ProtectedRoute requiredRoles={['ADMIN', 'SUPERVISOR']}>
  <AdminPanel />
</ProtectedRoute>
```

### Interceptação HTTP
- **Request**: Adiciona token automaticamente
- **Response**: Renova token em caso de expiração (401)
- **Error**: Redireciona para login em erros de auth

## 🔌 Comunicação em Tempo Real (WebSocket)

### Cliente WebSocket

#### Configuração Básica
```typescript
// websocket/client.ts
class WebSocketClient {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private heartbeatInterval: number | null = null
  
  // Conecta com autenticação JWT
  connect(): Promise<void>
  
  // Envia mensagens tipadas
  send(type: WebSocketEventType, data: unknown): void
  
  // Reconexão automática
  private attemptReconnect(): void
}
```

#### Tipos de Eventos
```typescript
type WebSocketEventType = 
  | 'NEW_MESSAGE'              // Nova mensagem recebida
  | 'MESSAGE_STATUS_UPDATED'   // Status da mensagem alterado
  | 'CONVERSATION_ASSIGNED'    // Conversa atribuída a agente
  | 'CONVERSATION_STATUS_CHANGED' // Status da conversa alterado
  | 'USER_TYPING'              // Usuário digitando
  | 'USER_STOP_TYPING'         // Usuário parou de digitar
  | 'USER_ONLINE'              // Usuário online
  | 'USER_OFFLINE'             // Usuário offline
  | 'CONVERSATION_UPDATED'     // Conversa atualizada
  | 'PING' | 'PONG'           // Heartbeat
```

### Event Handlers

#### Estrutura Principal
```typescript
// websocket/eventHandlers.ts
class WebSocketEventHandlers {
  private store = useChatStore.getState()
  
  constructor() {
    // Registra handlers no cliente WebSocket
    webSocketClient.on({
      onMessage: this.handleMessage.bind(this),
      onConnect: this.handleConnect.bind(this),
      onDisconnect: this.handleDisconnect.bind(this),
      onError: this.handleError.bind(this)
    })
  }
}
```

#### Handlers Específicos

**Nova Mensagem**
```typescript
private handleNewMessage = (data: { message: Message, conversationId: string }) => {
  // 1. Adiciona mensagem ao store
  this.store.addMessage(conversationId, data.message)
  
  // 2. Atualiza última mensagem da conversa
  this.store.updateConversationLastMessage(conversationId, data.message)
  
  // 3. Mostra notificação se conversa não estiver ativa
  if (activeConversationId !== conversationId) {
    this.showNotification('Nova mensagem', message.content)
  }
  
  // 4. Reproduz som de notificação
  this.playNotificationSound()
}
```

**Status de Conversa**
```typescript
private handleConversationStatusChanged = (data) => {
  const { conversationId, status, updatedBy } = data
  
  // Atualiza status no store
  this.store.updateConversationStatus(conversationId, status)
  
  // Move conversa para categoria correta
  this.store.moveConversationToStatus(conversationId, status)
  
  // Notifica mudança
  this.showNotification(`Conversa movida para ${status} por ${updatedBy}`)
}
```

### Typing Indicators

#### Componente Visual
```typescript
// components/TypingIndicator.tsx
const TypingIndicator: React.FC<{ conversationId: string }> = ({ conversationId }) => {
  const getTypingUsers = useChatStore(state => state.getTypingUsers)
  const [typingUsers, setTypingUsers] = useState<string[]>([])

  useEffect(() => {
    // Atualiza lista de usuários digitando a cada segundo
    const interval = setInterval(() => {
      const users = getTypingUsers(conversationId)
      setTypingUsers(users)
    }, 1000)

    return () => clearInterval(interval)
  }, [conversationId])

  if (typingUsers.length === 0) return null

  return (
    <div className="flex items-center space-x-2">
      {/* Animação de pontos */}
      <div className="flex space-x-1">
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
      </div>
      <span>{getTypingText(typingUsers)}</span>
    </div>
  )
}
```

#### Integração no ChatInput
```typescript
// components/ChatInput/index.tsx
const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
  const newMessage = e.target.value
  setMessage(newMessage)
  
  if (!activeChat) return
  
  // Inicia indicador de digitação
  if (newMessage.trim() && !isTypingRef.current) {
    webSocketEventHandlers.sendTyping(activeChat.id)
    isTypingRef.current = true
  }
  
  // Reset do timeout - para indicador após 3s de inatividade
  if (typingTimeoutRef.current) {
    clearTimeout(typingTimeoutRef.current)
  }
  
  typingTimeoutRef.current = setTimeout(() => {
    if (isTypingRef.current) {
      webSocketEventHandlers.stopTyping(activeChat.id)
      isTypingRef.current = false
    }
  }, 3000)
  
  // Para imediatamente se input vazio
  if (!newMessage.trim() && isTypingRef.current) {
    webSocketEventHandlers.stopTyping(activeChat.id)
    isTypingRef.current = false
  }
}
```

### Manager Principal

#### WebSocket Manager
```typescript
// websocket/index.ts
class WebSocketManager {
  private isInitialized = false
  
  async initialize() {
    // Verifica autenticação
    const isAuthenticated = useAuthStore.getState().isAuthenticated
    if (!isAuthenticated) return

    // Conecta ao WebSocket
    await webSocketClient.connect()
    this.isInitialized = true
  }
  
  // Métodos de conveniência
  sendTyping(conversationId: string)
  stopTyping(conversationId: string)
  sendUserStatus(isOnline: boolean)
  
  // Lifecycle
  reconnectOnLogin()
  disconnectOnLogout()
}
```

#### Inicialização Automática
```typescript
// components/ChatDataProvider.tsx
useEffect(() => {
  const initializeData = async () => {
    // Carrega dados iniciais
    await store.loadConversations(store.currentStatus, 0)
    
    // Inicializa WebSocket
    await webSocketManager.initialize()
  }
  
  initializeData()
  
  return () => {
    webSocketManager.disconnect()
  }
}, [])
```

## 🗄 Gerenciamento de Estado

### Store Principal (Zustand)

#### Estado da Aplicação
```typescript
// store/useChatStore.ts
interface ChatStoreState {
  // Estado principal
  chats: Chat[]
  activeChat: Chat | null
  currentStatus: ChatStatus
  searchQuery: string
  
  // Estados de carregamento
  isLoading: boolean
  isLoadingMessages: boolean
  isSending: boolean
  
  // Paginação
  currentPage: number
  hasMore: boolean
  totalChats: number
  
  // Cache de mensagens
  messagesCache: Record<string, {
    messages: Message[]
    page: number
    hasMore: boolean
  }>
  
  // Estados de tempo real
  typingUsers: Record<string, TypingUser[]>
  onlineUsers: Set<string>
  activeConversationId: string | null
}
```

#### Ações Principais

**Carregamento de Dados**
```typescript
// Carrega conversas com paginação
loadConversations: async (status?: ChatStatus, page = 0) => {
  set({ isLoading: true })
  
  const response = await conversationApi.getByStatus(status, page, 20)
  const newChats = conversationAdapter.toChatArray(response.content)
  
  set({
    chats: page === 0 ? newChats : [...state.chats, ...newChats],
    currentPage: page,
    hasMore: !response.last,
    isLoading: false
  })
}

// Carrega mensagens de uma conversa
loadMessages: async (chatId: string, page = 0) => {
  const response = await messageApi.getByConversation(chatId, page, 50)
  const newMessages = messageAdapter.toMessageArray(response.content)
  
  // Atualiza cache
  set({
    messagesCache: {
      ...state.messagesCache,
      [chatId]: {
        messages: mergedMessages,
        page,
        hasMore: !response.last
      }
    }
  })
}
```

**Envio de Mensagens (Optimistic Updates)**
```typescript
sendMessage: async (chatId: string, content: string) => {
  // 1. Validação
  const validation = MessageValidator.validate(content)
  if (!validation.valid) return
  
  // 2. Mensagem temporária (optimistic update)
  const tempMessage = messageAdapter.createTempMessage(content)
  
  // 3. Adiciona ao cache imediatamente
  const updatedMessages = [...cachedMessages, tempMessage]
  set({ messagesCache: { ...cache, [chatId]: { messages: updatedMessages } } })
  
  try {
    // 4. Envia para API
    const sentMessage = await messageApi.send(chatId, content)
    
    // 5. Substitui mensagem temporária pela real
    const finalMessages = messageAdapter.replaceTempMessage(
      updatedMessages, 
      tempMessage.id, 
      sentMessage
    )
    set({ messagesCache: { ...cache, [chatId]: { messages: finalMessages } } })
    
  } catch (error) {
    // Remove mensagem temporária em caso de erro
    const cleanedMessages = messageAdapter.removeTempMessage(updatedMessages, tempMessage.id)
    set({ messagesCache: { ...cache, [chatId]: { messages: cleanedMessages } } })
  }
}
```

**Ações WebSocket**
```typescript
// Adiciona mensagem recebida via WebSocket
addMessage: (conversationId: string, message: Message) => {
  const cached = state.messagesCache[conversationId]
  if (cached) {
    set({
      messagesCache: {
        ...state.messagesCache,
        [conversationId]: {
          ...cached,
          messages: [...cached.messages, message]
        }
      }
    })
  }
}

// Gerencia usuários digitando
setUserTyping: (conversationId: string, userId: string, userName: string, isTyping: boolean) => {
  const currentTyping = state.typingUsers[conversationId] || []
  
  if (isTyping) {
    const updatedTyping = [...currentTyping, { userId, userName, timestamp: Date.now() }]
    set({
      typingUsers: {
        ...state.typingUsers,
        [conversationId]: updatedTyping
      }
    })
  } else {
    const updatedTyping = currentTyping.filter(u => u.userId !== userId)
    set({
      typingUsers: {
        ...state.typingUsers,
        [conversationId]: updatedTyping
      }
    })
  }
}
```

### Hooks Utilitários
```typescript
// Hooks específicos para diferentes partes do estado
const activeChat = useChatStore(state => state.activeChat)
const isLoading = useChatStore(state => state.isLoading)
const filteredChats = useChatStore(state => state.getFilteredChats())

// Hook customizado para typing users
const useTypingUsers = (conversationId: string) => {
  return useChatStore(state => state.getTypingUsers(conversationId))
}
```

## 🧩 Componentes

### Estrutura de Componentes

#### BloodCenterChat (Página Principal)
```typescript
// components/BloodCenterChat.tsx
const BloodCenterChat = () => {
  const [donors, setDonors] = useState<Donor[]>([])
  const [selectedDonor, setSelectedDonor] = useState<Donor | null>(null)
  const [allContacts, setAllContacts] = useState<Donor[]>([])
  
  // Carrega conversas existentes
  const loadConversations = useCallback(async () => {
    const [entradaResponse, esperandoResponse, finalizadosResponse] = await Promise.all([
      conversationApi.getByStatus('ENTRADA', 0, 50),
      conversationApi.getByStatus('ESPERANDO', 0, 50),
      conversationApi.getByStatus('FINALIZADOS', 0, 50)
    ])
    
    const allConversations = [
      ...entradaResponse.content,
      ...esperandoResponse.content,
      ...finalizadosResponse.content
    ]
    
    const donorsFromConversations = allConversations.map(convertConversationToDonor)
    setDonors(donorsFromConversations)
  }, [])
  
  // Carrega todos os contatos (para modal de nova conversa)
  const loadAllContacts = useCallback(async () => {
    const customersResponse = await customerApi.getAll({ size: 200 })
    const customers = Array.isArray(customersResponse) ? customersResponse : customersResponse.content
    const donorsFromCustomers = customers.map(convertCustomerToDonor)
    setAllContacts(donorsFromCustomers)
  }, [])
  
  return (
    <div className="flex h-screen bg-white">
      <DonorSidebar
        donors={donors}
        selectedDonor={selectedDonor}
        onDonorSelect={setSelectedDonor}
        onNewChat={() => setShowNewChatModal(true)}
      />
      
      <div className="flex-1 flex flex-col">
        {selectedDonor ? (
          <>
            <ChatHeader donor={selectedDonor} />
            <MessageList messages={messages} />
            <ChatInput 
              onSendMessage={handleSendMessage}
              disabled={!selectedDonor}
            />
          </>
        ) : (
          <WelcomeScreen />
        )}
      </div>
      
      <NewChatModal
        show={showNewChatModal}
        availableDonors={allContacts}
        onClose={() => setShowNewChatModal(false)}
        onDonorSelect={handleDonorSelect}
        onNewContactCreate={handleNewContactCreate}
      />
    </div>
  )
}
```

#### DonorSidebar (Lista de Doadores/Conversas)
```typescript
// components/DonorSidebar/index.tsx
const DonorSidebar = ({ donors, selectedDonor, onDonorSelect, onNewChat }) => {
  // Filtra apenas doadores com conversas ativas
  const activeDonors = donors.filter((d) => d.lastMessage || d.hasActiveConversation)
  const filteredDonors = activeDonors.filter((donor) =>
    donor.name.toLowerCase().includes(searchTerm.toLowerCase())
  )
  
  return (
    <div className="w-80 bg-gray-50 border-r border-gray-200 flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center gap-3 mb-4">
          <Heart className="text-red-500 text-2xl" fill="currentColor" />
          <h1 className="text-lg font-semibold text-gray-800">Centro de Sangue</h1>
        </div>

        <div className="flex gap-2 mb-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <input
              type="text"
              placeholder="Buscar conversas..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg"
            />
          </div>
          <button
            onClick={onNewChat}
            className="bg-blue-500 hover:bg-blue-600 text-white p-2 rounded-lg"
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {filteredDonors.map((donor) => (
          <div
            key={donor.id}
            onClick={() => onDonorSelect(donor)}
            className={`p-3 mb-1 rounded-lg cursor-pointer transition-all ${
              selectedDonor?.id === donor.id
                ? "bg-blue-50 border border-blue-200"
                : "hover:bg-gray-100"
            }`}
          >
            {/* Donor card content */}
          </div>
        ))}
      </div>
    </div>
  )
}
```

#### ChatInput (Campo de Entrada)
```typescript
// components/ChatInput/index.tsx
const ChatInput = () => {
  const [message, setMessage] = useState("")
  const [activeTab, setActiveTab] = useState<"message" | "notes">("message")
  const activeChat = useChatStore(state => state.activeChat)
  const sendMessage = useChatStore(state => state.sendMessage)
  const isSending = useChatStore(state => state.isSending)
  
  // Typing indicators
  const typingTimeoutRef = useRef<number | undefined>(undefined)
  const isTypingRef = useRef(false)
  
  const handleSendMessage = async () => {
    if (!message.trim() || !activeChat || isSending) return
    
    // Para typing indicator
    if (isTypingRef.current) {
      webSocketEventHandlers.stopTyping(activeChat.id)
      isTypingRef.current = false
    }
    
    await sendMessage(activeChat.id, message.trim())
    setMessage("")
  }
  
  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newMessage = e.target.value
    setMessage(newMessage)
    
    // Gerencia typing indicator
    if (activeChat) {
      if (newMessage.trim() && !isTypingRef.current) {
        webSocketEventHandlers.sendTyping(activeChat.id)
        isTypingRef.current = true
      }
      
      // Auto-stop após 3s
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current)
      }
      
      typingTimeoutRef.current = setTimeout(() => {
        if (isTypingRef.current) {
          webSocketEventHandlers.stopTyping(activeChat.id)
          isTypingRef.current = false
        }
      }, 3000)
    }
  }
  
  return (
    <div className="p-4">
      <div className="bg-white rounded-3xl shadow-lg border p-4">
        {/* Tabs */}
        <div className="flex gap-1 pb-2">
          <button
            onClick={() => setActiveTab("message")}
            className={activeTab === "message" ? "active-tab" : "inactive-tab"}
          >
            Mensagem
          </button>
          <button
            onClick={() => setActiveTab("notes")}
            className={activeTab === "notes" ? "active-tab" : "inactive-tab"}
          >
            Notas
          </button>
        </div>
        
        {/* Input Area */}
        {activeTab === "message" && (
          <>
            <div className="flex items-end gap-3">
              <Input.TextArea
                value={message}
                onChange={handleInputChange}
                onKeyPress={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault()
                    handleSendMessage()
                  }
                }}
                placeholder="Digite sua mensagem…"
                autoSize={{ minRows: 1, maxRows: 4 }}
              />
              
              {message.trim() && (
                <button
                  onClick={handleSendMessage}
                  disabled={isSending}
                  className="send-button"
                >
                  {isSending ? <Spinner /> : <Send size={18} />}
                </button>
              )}
            </div>
            
            {/* Action Buttons */}
            <div className="flex items-center justify-between mt-3">
              <div className="flex gap-1">
                <button className="action-button"><Smile size={20} /></button>
                <button className="action-button"><Paperclip size={20} /></button>
                {/* ... outros botões */}
              </div>
              
              <button className="action-button">
                <Mic size={20} />
              </button>
            </div>
          </>
        )}
        
        {activeTab === "notes" && (
          <NotesArea />
        )}
      </div>
    </div>
  )
}
```

### Componentes de UI

#### Loading States (Skeletons)
```typescript
// components/skeletons/ChatListSkeleton.tsx
const ChatListSkeleton = () => (
  <div className="space-y-2 p-4">
    {Array.from({ length: 5 }).map((_, i) => (
      <div key={i} className="flex items-center space-x-3 p-3">
        <div className="w-12 h-12 bg-gray-200 rounded-full animate-pulse" />
        <div className="flex-1 space-y-2">
          <div className="h-4 bg-gray-200 rounded w-3/4 animate-pulse" />
          <div className="h-3 bg-gray-200 rounded w-1/2 animate-pulse" />
        </div>
        <div className="h-3 bg-gray-200 rounded w-12 animate-pulse" />
      </div>
    ))}
  </div>
)
```

#### Tratamento de Erros
```typescript
// components/ErrorBoundary.tsx
class ErrorBoundary extends Component<Props, State> {
  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }
  
  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
    
    // Dispatch evento para monitoramento
    window.dispatchEvent(new CustomEvent('app:error', {
      detail: { error: error.message, stack: error.stack, componentStack: errorInfo.componentStack }
    }))
  }
  
  public render() {
    if (this.state.hasError) {
      return (
        <div className="h-screen flex items-center justify-center">
          <Result
            icon={<AlertTriangle size={64} className="text-red-500" />}
            title="Ops! Algo deu errado"
            subTitle="Ocorreu um erro inesperado na aplicação."
            extra={[
              <Button type="primary" onClick={() => window.location.reload()}>
                Recarregar Página
              </Button>,
              <Button onClick={() => this.setState({ hasError: false })}>
                Tentar Novamente
              </Button>
            ]}
          />
        </div>
      )
    }
    
    return this.props.children
  }
}
```

## 🔔 Sistema de Notificações

### Provedor de Notificações
```typescript
// components/notifications/ToastProvider.tsx
export const useNotifications = () => {
  const showNotification = useCallback((notification: NotificationConfig) => {
    const toastId = `toast-${Date.now()}`
    
    // Cria elemento de notificação
    const toast = notification({
      id: toastId,
      duration: notification.duration || 5000,
      position: 'top-right',
      className: getToastClassName(notification.type)
    })
    
    // Auto-remove
    setTimeout(() => {
      toast.dismiss()
    }, notification.duration || 5000)
    
  }, [])
  
  return {
    success: (config: NotificationConfig) => showNotification({ ...config, type: 'success' }),
    error: (config: NotificationConfig) => showNotification({ ...config, type: 'error' }),
    warning: (config: NotificationConfig) => showNotification({ ...config, type: 'warning' }),
    info: (config: NotificationConfig) => showNotification({ ...config, type: 'info' })
  }
}
```

### Hook de Notificações
```typescript
// hooks/useNotifications.ts
export const useNotifications = () => {
  useEffect(() => {
    // Escuta eventos customizados do WebSocket
    const handleChatNotification = (event: CustomEvent) => {
      const { type, message, description, duration, onClick } = event.detail
      
      notification[type]({
        message,
        description,
        duration,
        onClick,
        placement: 'topRight'
      })
    }
    
    window.addEventListener('chat:notification', handleChatNotification)
    
    return () => {
      window.removeEventListener('chat:notification', handleChatNotification)
    }
  }, [])
}
```

### Integração com WebSocket
```typescript
// No eventHandlers.ts, em vez de usar hooks diretamente:
private showNotification(type: string, message: string, description: string) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('chat:notification', {
      detail: { type, message, description, duration: 5 }
    }))
  }
}
```

## 🛡 Sistema de Tratamento de Erros

### Error Boundaries

#### Aplicação Level
```typescript
// components/ErrorBoundary.tsx - Para erros críticos que quebram a app
<ErrorBoundary>
  <App />
</ErrorBoundary>
```

#### Component Level
```typescript
// components/ComponentErrorBoundary.tsx - Para erros específicos de componente
<ComponentErrorBoundary componentName="ChatList" fallback={<ChatListSkeleton />}>
  <ChatList />
</ComponentErrorBoundary>
```

### Tratamento de Erros de API
```typescript
// api/client.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000
})

// Interceptor de resposta para tratamento global de erros
apiClient.interceptors.response.use(
  response => response,
  async error => {
    const { response, config } = error
    
    if (response?.status === 401) {
      // Token expirado - tenta renovar
      try {
        await authService.refreshToken()
        return apiClient(config) // Retry original request
      } catch {
        authService.logout()
        window.location.href = '/login'
      }
    }
    
    if (response?.status >= 500) {
      // Erro de servidor
      window.dispatchEvent(new CustomEvent('app:error', {
        detail: { message: 'Erro interno do servidor', type: 'server' }
      }))
    }
    
    throw error
  }
)
```

### Validação de Dados
```typescript
// utils/validation.ts
export class MessageValidator {
  static validate(content: string): ValidationResult {
    if (!content.trim()) {
      return { valid: false, error: 'Mensagem não pode estar vazia' }
    }
    
    if (content.length > 1000) {
      return { valid: false, error: 'Mensagem muito longa (máx. 1000 caracteres)' }
    }
    
    return { valid: true }
  }
}

export class PhoneValidator {
  static validate(phone: string): ValidationResult {
    const phoneRegex = /^\+?[1-9]\d{1,14}$/
    
    if (!phoneRegex.test(phone)) {
      return { valid: false, error: 'Formato de telefone inválido' }
    }
    
    return { valid: true }
  }
}
```

## 🎨 UX/UI Enhancements

### Estados de Carregamento
- **Skeleton Loading**: Componentes que mostram estrutura enquanto carrega
- **Progressive Loading**: Carregamento incremental de dados
- **Optimistic Updates**: Atualizações instantâneas com rollback em erro

### Indicadores Visuais
- **Typing Indicators**: Mostra quando usuários estão digitando
- **Online Status**: Indicadores de usuários online/offline
- **Connection Status**: Status da conexão com servidor
- **Message Status**: Enviado, entregue, lido

### Responsividade
- **Mobile-First**: Design otimizado para dispositivos móveis
- **Adaptive Layout**: Layout que se adapta ao tamanho da tela
- **Touch-Friendly**: Elementos adequados para touch

### Acessibilidade
- **ARIA Labels**: Labels descritivos para screen readers
- **Keyboard Navigation**: Navegação completa via teclado
- **High Contrast**: Suporte a temas de alto contraste
- **Focus Management**: Gerenciamento adequado de foco

## 🆕 Sistema de Criação de Contatos e Conversas

### Fluxo de Criação Diferida
A aplicação implementa um sistema inteligente onde **novos contatos aparecem no sidebar apenas após a primeira mensagem ser enviada**:

#### Fluxo Completo
```typescript
// 1. Usuário cria novo contato no NewChatModal
const handleNewContactSubmit = async () => {
  // Apenas cria o customer, NÃO cria conversa
  const newCustomer = await customerApi.create(createRequest)
  
  // Marca como sem conversa ativa
  const donor: Donor = {
    ...customerData,
    hasActiveConversation: false,
    lastMessage: "" // Vazio = não aparece no sidebar
  }
  
  // Adiciona à lista geral mas não ao sidebar
  onNewContactCreate({ donor })
  onDonorSelect(donor) // Abre chat
}

// 2. Usuário envia primeira mensagem
const handleSendMessage = async () => {
  const isFirstMessage = !selectedDonor.hasActiveConversation && !selectedDonor.lastMessage
  
  if (isFirstMessage) {
    // Agora sim cria a conversa
    await conversationApi.create({
      customerId: selectedDonor.id,
      channel: 'WEB'
    })
    
    // Atualiza o donor para aparecer no sidebar
    const updatedDonor = {
      ...selectedDonor,
      hasActiveConversation: true,
      lastMessage: messageInput,
      timestamp: getCurrentTimestamp()
    }
    
    // Atualiza estado - donor aparece no DonorSidebar
    setDonors(prev => prev.map(d => 
      d.id === selectedDonor.id ? updatedDonor : d
    ))
  }
  
  // Envia a mensagem normalmente
  // ...
}
```

### NewChatModal - Modal de Criação
```typescript
// components/NewChatModal/index.tsx
const NewChatModal = ({ availableDonors, onNewContactCreate, onDonorSelect }) => {
  const [activeTab, setActiveTab] = useState<"existing" | "new">("existing")
  
  return (
    <div className="modal">
      {/* Tabs: Doadores Existentes | Novo Contato */}
      <div className="tabs">
        <button onClick={() => setActiveTab("existing")}>
          Doadores Existentes
        </button>
        <button onClick={() => setActiveTab("new")}>
          Novo Contato
        </button>
      </div>
      
      {activeTab === "existing" ? (
        // Lista todos os contatos (com e sem conversas)
        <ContactList contacts={availableDonors} onSelect={onDonorSelect} />
      ) : (
        // Formulário para criar novo contato
        <ContactForm onSubmit={handleNewContactSubmit} />
      )}
    </div>
  )
}
```

### Diferenciação de Estados
```typescript
// types/types.ts
interface Donor {
  id: string
  name: string
  phone: string
  lastMessage: string
  hasActiveConversation?: boolean // Nova propriedade
  // ... outros campos
}

// DonorSidebar filtra por conversas ativas
const activeDonors = donors.filter(d => 
  d.lastMessage || d.hasActiveConversation
)

// NewChatModal mostra TODOS os contatos
const allContacts = customers.map(convertCustomerToDonor)
```

### Vantagens desta Abordagem
- **UX Intuitiva**: Sidebar limpo, mostra apenas conversas realmente iniciadas
- **Performance**: Não cria conversas desnecessárias no banco
- **Flexibilidade**: Permite ter contatos "preparados" sem poluir interface
- **Consistência**: Alinha com fluxo natural de comunicação

## 🔄 APIs e Adaptadores

### Estrutura de API
```typescript
// api/client.ts
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Interceptors para auth automático
apiClient.interceptors.request.use(config => {
  const token = authService.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

### Serviços de API
```typescript
// api/services/conversationApi.ts
export const conversationApi = {
  getByStatus: (status: ConversationStatus, page: number, size: number) =>
    apiClient.get(`/conversations`, { params: { status, page, size } }),
    
  getById: (id: string) =>
    apiClient.get(`/conversations/${id}`),
    
  search: (query: string, filters: SearchFilters) =>
    apiClient.get(`/conversations/search`, { params: { q: query, ...filters } }),
    
  assignToUser: (conversationId: string, userId: string) =>
    apiClient.put(`/conversations/${conversationId}/assign`, { userId }),
    
  changeStatus: (conversationId: string, status: ConversationStatus) =>
    apiClient.put(`/conversations/${conversationId}/status`, { status }),
    
  pin: (conversationId: string) =>
    apiClient.put(`/conversations/${conversationId}/pin`),
    
  unpin: (conversationId: string) =>
    apiClient.delete(`/conversations/${conversationId}/pin`)
}
```

### Padrão Adapter
```typescript
// adapters/conversationAdapter.ts
export const conversationAdapter = {
  // Backend → Frontend
  toChat: (dto: ConversationDTO): Chat => ({
    id: dto.id,
    contact: {
      id: dto.customer.id,
      name: dto.customer.name,
      phone: dto.customer.phone,
      avatar: dto.customer.avatar
    },
    lastMessage: dto.lastMessage ? messageAdapter.toMessage(dto.lastMessage) : null,
    unreadCount: dto.unreadCount,
    status: mapStatusFromBackend(dto.status),
    assignedAgent: dto.assignedUser?.name,
    isPinned: dto.pinned,
    tags: dto.tags?.map(tagAdapter.toTag) || [],
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt)
  }),
  
  toChatArray: (dtos: ConversationDTO[]): Chat[] =>
    dtos.map(conversationAdapter.toChat),
  
  // Frontend → Backend
  mapStatusToBackend: (status: ChatStatus): ConversationStatus => {
    const statusMap: Record<ChatStatus, ConversationStatus> = {
      entrada: 'OPEN',
      esperando: 'IN_PROGRESS', 
      finalizados: 'CLOSED'
    }
    return statusMap[status]
  },
  
  mapStatusFromBackend: (status: ConversationStatus): ChatStatus => {
    const statusMap: Record<ConversationStatus, ChatStatus> = {
      OPEN: 'entrada',
      IN_PROGRESS: 'esperando',
      CLOSED: 'finalizados'
    }
    return statusMap[status]
  }
}
```

### Adapter de Mensagens
```typescript
// adapters/messageAdapter.ts
export const messageAdapter = {
  toMessage: (dto: MessageDTO): Message => ({
    id: dto.id,
    content: dto.content,
    type: dto.type,
    senderType: dto.senderType,
    senderName: dto.senderName,
    timestamp: new Date(dto.createdAt),
    status: dto.status,
    attachments: dto.attachments?.map(attachmentAdapter.toAttachment) || []
  }),
  
  toMessageArray: (dtos: MessageDTO[]): Message[] =>
    dtos.map(messageAdapter.toMessage),
  
  // Optimistic updates
  createTempMessage: (content: string): Message => ({
    id: `temp-${Date.now()}-${Math.random()}`,
    content,
    type: 'TEXT',
    senderType: 'AGENT',
    senderName: 'Você',
    timestamp: new Date(),
    status: 'SENDING',
    attachments: []
  }),
  
  isTempMessage: (message: Message): boolean =>
    message.id.startsWith('temp-'),
  
  replaceTempMessage: (messages: Message[], tempId: string, realMessage: Message): Message[] =>
    messages.map(msg => msg.id === tempId ? realMessage : msg),
  
  removeTempMessage: (messages: Message[], tempId: string): Message[] =>
    messages.filter(msg => msg.id !== tempId),
  
  mergeMessages: (existing: Message[], newMessages: Message[]): Message[] => {
    const existingIds = new Set(existing.map(m => m.id))
    const filtered = newMessages.filter(m => !existingIds.has(m.id))
    return [...existing, ...filtered].sort((a, b) => 
      a.timestamp.getTime() - b.timestamp.getTime()
    )
  },
  
  markMessagesAsRead: (messages: Message[]): Message[] =>
    messages.map(msg => ({ ...msg, status: 'READ' })),
  
  toCreateRequest: (content: string): CreateMessageDTO => ({
    content,
    type: 'TEXT'
  })
}
```

## 🚀 Build e Deploy

### Build de Produção
```bash
# Build otimizado
npm run build

# Preview do build
npm run preview
```

### Otimizações
- **Code Splitting**: Divisão automática do código
- **Tree Shaking**: Remoção de código não utilizado
- **Asset Optimization**: Otimização de imagens e assets
- **Gzip Compression**: Compressão automática

### Configuração de Deploy
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['antd'],
          utils: ['zustand', 'axios']
        }
      }
    }
  },
  preview: {
    port: 4173,
    host: true
  }
})
```

### Variáveis de Ambiente por Ambiente
```bash
# .env.development
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws

# .env.production  
VITE_API_URL=https://api.production.com/api
VITE_WS_URL=wss://api.production.com/ws
```

## 🔧 Troubleshooting

### Problemas Comuns

**WebSocket não conecta**
1. Verificar se backend está rodando
2. Verificar URL do WebSocket
3. Verificar autenticação JWT

**Mensagens não aparecem em tempo real**
1. Verificar conexão WebSocket
2. Verificar event handlers
3. Verificar store updates

**Build falha**
1. Executar `npm run type-check`
2. Executar `npm run lint`
3. Verificar imports e tipos

**Performance lenta**
1. Verificar se há memory leaks
2. Otimizar re-renders desnecessários
3. Implementar virtualization para listas grandes

### Logs e Debug
```typescript
// Habilitar logs de WebSocket
localStorage.setItem('debug-websocket', 'true')

// Habilitar logs de store
localStorage.setItem('debug-store', 'true')

// Ver estado atual
console.log('Store state:', useChatStore.getState())
```

---

## 📝 Notas de Desenvolvimento

- **Convenções**: Usar inglês para código, português para UI
- **Commits**: Seguir Conventional Commits
- **Testes**: Implementar testes para componentes críticos
- **Performance**: Monitorar bundle size e performance
- **Acessibilidade**: Seguir guidelines WCAG 2.1

Esta documentação reflete o estado atual da aplicação e deve ser atualizada conforme novas funcionalidades são implementadas.