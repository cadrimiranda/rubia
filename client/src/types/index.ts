export type ChatStatus = 'ativos' | 'aguardando' | 'inativo' | 'entrada' | 'esperando' | 'finalizados'

export type ContactType = 'comercial' | 'suporte' | 'vendas'

export type MessageStatus = 'sending' | 'sent' | 'delivered' | 'read'

export type WhatsAppInstanceStatus = 
  | 'NOT_CONFIGURED' 
  | 'CONFIGURING' 
  | 'AWAITING_QR_SCAN' 
  | 'CONNECTING' 
  | 'CONNECTED' 
  | 'DISCONNECTED' 
  | 'ERROR' 
  | 'SUSPENDED'

export type MessagingProvider = 'Z_API' | 'TWILIO' | 'WHATSAPP_BUSINESS_API' | 'MOCK'

export interface User {
  id: string
  name: string
  avatar: string
  isOnline: boolean
  lastSeen?: Date
  phone?: string
  whatsappId?: string
  isBlocked?: boolean
  bloodType?: string
  lastDonation?: string
  totalDonations?: number
  birthDate?: string
  weight?: number
  height?: number
  address?: string
  email?: string
}

export interface Tag {
  id: string
  name: string
  color: string
  type: ContactType
}

export interface Message {
  id: string
  content: string
  timestamp: Date
  senderId: string
  messageType: 'text' | 'image' | 'file' | 'audio'
  status: MessageStatus
  isFromUser: boolean
  mediaUrl?: string
  externalMessageId?: string
  isAiGenerated?: boolean
  aiConfidence?: number
  deliveredAt?: Date
  readAt?: Date
  audioDuration?: number
  mimeType?: string
}

export interface Chat {
  id: string
  contact: User
  messages: Message[]
  lastMessage?: Message
  unreadCount: number
  isPinned: boolean
  status: ChatStatus
  assignedAgent?: string
  tags: Tag[]
  priority?: number
  channel?: string
  closedAt?: Date
  createdAt: Date
  updatedAt: Date
}

export interface WhatsAppInstance {
  id: string
  phoneNumber: string
  displayName?: string
  provider: MessagingProvider
  isPrimary: boolean
  isActive: boolean
  createdAt: Date
}

export interface WhatsAppInstanceWithStatus extends WhatsAppInstance {
  status: WhatsAppInstanceStatus
  connected: boolean
  error?: string
  lastConnectedAt?: Date
}

export interface WhatsAppSetupStatus {
  requiresSetup: boolean
  hasConfiguredInstance: boolean
  hasConnectedInstance: boolean
  totalInstances: number
  maxAllowedInstances: number
  instances: WhatsAppInstanceWithStatus[]
}

export interface AuthUser {
  id: string
  name: string
  email: string
  avatar?: string
  role: string
}

export interface AuthResponse {
  token: string
  user: AuthUser
  expiresIn: number
  companyId: string
  companyGroupId: string
  companySlug: string
  requiresWhatsAppSetup: boolean
}

export interface ChatStore {
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
  
  // Erros
  error: string | null
  
  // Cache de mensagens por conversa
  messagesCache: Record<string, {
    messages: Message[]
    page: number
    hasMore: boolean
  }>
  
  // Navegação
  setActiveChat: (chat: Chat | null) => Promise<void>
  setCurrentStatus: (status: ChatStatus) => void
  setSearchQuery: (query: string) => void
  
  // Carregamento de dados
  loadConversations: (status?: ChatStatus, page?: number) => Promise<void>
  loadMessages: (chatId: string, page?: number) => Promise<void>
  refreshConversations: () => Promise<void>
  
  // Envio de mensagens
  sendMessage: (chatId: string, content: string) => Promise<void>
  
  // Ações de conversa
  markAsRead: (chatId: string) => Promise<void>
  assignToAgent: (chatId: string, agentId: string) => Promise<void>
  changeStatus: (chatId: string, status: ChatStatus) => Promise<void>
  pinConversation: (chatId: string) => Promise<void>
  blockCustomer: (chatId: string) => Promise<void>
  
  // Tags
  addTag: (chatId: string, tag: Tag) => Promise<void>
  removeTag: (chatId: string, tagId: string) => Promise<void>
  
  // Busca
  searchConversations: (query: string) => Promise<void>
  searchMessages: (query: string) => Promise<void>
  
  // Utilitários
  getFilteredChats: () => Chat[]
  clearError: () => void
  clearCache: () => void
  
  // Métodos legados (para compatibilidade)
  transferChat: (chatId: string, agentName: string) => Promise<void>
  blockContact: (chatId: string) => Promise<void>
  finalizeChat: (chatId: string) => Promise<void>
  pinChat: (chatId: string) => Promise<void>
}

export interface MessageOptionsMenuItem {
  key: string
  label: string
  icon: React.ReactNode
  onClick: () => void
  danger?: boolean
}