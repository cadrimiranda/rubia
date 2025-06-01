export type ChatStatus = 'entrada' | 'esperando' | 'finalizados'

export type ContactType = 'comercial' | 'suporte' | 'vendas'

export type MessageStatus = 'sending' | 'sent' | 'delivered' | 'read'

export interface User {
  id: string
  name: string
  avatar: string
  isOnline: boolean
  lastSeen?: Date
  phone?: string
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
  createdAt: Date
  updatedAt: Date
}

export interface ChatStore {
  chats: Chat[]
  activeChat: Chat | null
  currentStatus: ChatStatus
  searchQuery: string
  isLoading: boolean
  setActiveChat: (chat: Chat | null) => void
  setCurrentStatus: (status: ChatStatus) => void
  setSearchQuery: (query: string) => void
  sendMessage: (chatId: string, content: string) => void
  markAsRead: (chatId: string) => void
  addTag: (chatId: string, tag: Tag) => void
  removeTag: (chatId: string, tagId: string) => void
  transferChat: (chatId: string, agentName: string) => void
  blockContact: (chatId: string) => void
  finalizeChat: (chatId: string) => void
  pinChat: (chatId: string) => void
  getFilteredChats: () => Chat[]
}

export interface MessageOptionsMenuItem {
  key: string
  label: string
  icon: React.ReactNode
  onClick: () => void
  danger?: boolean
}