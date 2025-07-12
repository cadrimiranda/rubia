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

// Enums do backend (mapeados como ordinal no PostgreSQL)
export type ConversationStatus = 'ENTRADA' | 'ESPERANDO' | 'FINALIZADOS'
export type ConversationChannel = 'WHATSAPP' | 'INSTAGRAM' | 'FACEBOOK' | 'WEB_CHAT' | 'EMAIL'
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
  companyId: string
  phone: string
  name?: string
  whatsappId?: string
  profileUrl?: string
  isBlocked: boolean
  sourceSystemName?: string
  sourceSystemId?: string
  importedAt?: string
  birthDate?: string
  lastDonationDate?: string
  nextEligibleDonationDate?: string
  bloodType?: string
  height?: number
  weight?: number
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressPostalCode?: string
  addressCity?: string
  addressState?: string
  createdAt: string
  updatedAt: string
}

export interface ConversationDTO {
  id: string
  companyId: string
  customerId: string
  customerName?: string
  customerPhone?: string
  assignedUserId?: string
  assignedUserName?: string
  departmentId?: string
  departmentName?: string
  status: ConversationStatus
  channel: ConversationChannel
  priority: number
  isPinned?: boolean
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
  assignedUserId?: string
  departmentId?: string
  status?: ConversationStatus
  channel?: ConversationChannel
  priority?: number
  isPinned?: boolean
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