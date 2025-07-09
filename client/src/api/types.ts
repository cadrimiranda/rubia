// Base types para paginação
export interface PageRequest {
  page?: number
  size?: number
  sort?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// Enums do backend
export type ConversationStatus = 'ENTRADA' | 'ESPERANDO' | 'FINALIZADOS'
export type ConversationChannel = 'WHATSAPP' | 'WEB' | 'TELEGRAM'
export type MessageType = 'TEXT' | 'IMAGE' | 'AUDIO' | 'FILE' | 'LOCATION' | 'CONTACT'
export type MessageStatus = 'SENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED'
export type SenderType = 'CUSTOMER' | 'AGENT' | 'AI' | 'SYSTEM'
export type UserRole = 'ADMIN' | 'SUPERVISOR' | 'AGENT'

// DTOs do backend (espelhando estrutura)
export interface DepartmentDTO {
  id: string
  name: string
  description: string
  autoAssign: boolean
  createdAt: string
  updatedAt: string
}

export interface UserDTO {
  id: string
  name: string
  email: string
  departmentId: string
  department?: DepartmentDTO
  role: UserRole
  avatarUrl?: string
  isOnline: boolean
  lastSeen?: string
  companyId: string
  birthDate?: string
  weight?: number
  height?: number
  address?: string
  createdAt: string
  updatedAt: string
}

export interface CustomerDTO {
  id: string
  phone: string
  name?: string
  whatsappId?: string
  profileUrl?: string
  isBlocked: boolean
  createdAt: string
  updatedAt: string
}

export interface ConversationDTO {
  id: string
  customerId: string
  customer?: CustomerDTO
  assignedUserId?: string
  assignedUser?: UserDTO
  departmentId?: string
  department?: DepartmentDTO
  status: ConversationStatus
  channel: ConversationChannel
  priority: number
  isPinned: boolean
  createdAt: string
  updatedAt: string
  closedAt?: string
  messageCount?: number
  lastMessage?: MessageDTO
}

export interface MessageDTO {
  id: string
  conversationId: string
  content: string
  senderType: SenderType
  senderId?: string
  messageType: MessageType
  mediaUrl?: string
  externalMessageId?: string
  isAiGenerated: boolean
  aiConfidence?: number
  status: MessageStatus
  createdAt: string
  deliveredAt?: string
  readAt?: string
}

// Request DTOs
export interface CreateConversationRequest {
  customerId: string
  departmentId?: string
  channel?: ConversationChannel
}

export interface UpdateConversationRequest {
  assignedUserId?: string
  status?: ConversationStatus
  isPinned?: boolean
}

export interface CreateMessageRequest {
  content: string
  messageType?: MessageType
  mediaUrl?: string
}

export interface CreateCustomerRequest {
  phone: string
  name?: string
  whatsappId?: string
  profileUrl?: string
}

export interface UpdateCustomerRequest {
  name?: string
  profileUrl?: string
}

export interface CreateUserRequest {
  name: string
  email: string
  password: string
  companyId: string
  departmentId?: string
  role: UserRole
  avatarUrl?: string
  birthDate?: string
  weight?: number
  height?: number
  address?: string
}

export interface UpdateUserRequest {
  name?: string
  email?: string
  password?: string
  departmentId?: string
  role?: UserRole
  avatarUrl?: string
  birthDate?: string
  weight?: number
  height?: number
  address?: string
}

export interface LoginRequest {
  email: string
  password: string
  companySlug?: string
}

export interface LoginResponse {
  token: string
  user: {
    id: string
    name: string
    email: string
    role: UserRole
    companyId: string
    companyGroupId: string
    companyGroupName: string
    companySlug: string
    departmentId?: string
    departmentName?: string
    avatarUrl?: string
    isOnline: boolean
  }
  expiresIn: number
  companyId: string
  companyGroupId: string
  companySlug: string
}

// Filtros para listagens
export interface ConversationFilters extends PageRequest {
  status?: ConversationStatus
  assignedUserId?: string
  departmentId?: string
  customerId?: string
  isPinned?: boolean
  search?: string
}

export interface MessageFilters extends PageRequest {
  conversationId?: string
  senderType?: SenderType
  messageType?: MessageType
  search?: string
  startDate?: string
  endDate?: string
}

export interface CustomerFilters extends PageRequest {
  isBlocked?: boolean
  search?: string
}

// Responses específicas
export interface ConversationAssignResponse {
  success: boolean
  assignedUser: UserDTO
}

export interface MessageStatusResponse {
  messageId: string
  status: MessageStatus
  timestamp: string
}

export interface SearchResponse<T> {
  results: T[]
  totalCount: number
  query: string
  searchTime: number
}