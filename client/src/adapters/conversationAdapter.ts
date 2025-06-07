import type { ConversationDTO, CustomerDTO, MessageDTO } from '../api/types'
import type { Chat, ChatStatus } from '../types'


class ConversationAdapter {
  /**
   * Converte ConversationDTO do backend para Chat do frontend
   */
  toChat(dto: ConversationDTO): Chat {
    return {
      id: dto.id,
      contact: dto.customer ? this.convertCustomer(dto.customer) : {
        id: dto.customerId,
        name: 'Cliente Desconhecido',
        avatar: '',
        isOnline: false,
        phone: ''
      },
      messages: dto.lastMessage ? [this.convertMessage(dto.lastMessage)] : [],
      lastMessage: dto.lastMessage ? this.convertMessage(dto.lastMessage) : undefined,
      unreadCount: this.calculateUnreadCount(),
      isPinned: dto.isPinned,
      status: this.mapStatus(dto.status),
      assignedAgent: dto.assignedUser?.name,
      tags: [], // TODO: implementar quando tags estiverem disponíveis
      createdAt: new Date(dto.createdAt),
      updatedAt: new Date(dto.updatedAt)
    }
  }

  /**
   * Converte CustomerDTO para User (método interno para evitar dependência circular)
   */
  private convertCustomer(customer: CustomerDTO) {
    return {
      id: customer.id,
      name: customer.name || this.formatPhoneAsName(customer.phone),
      avatar: customer.profileUrl || this.generateAvatarUrl(customer.name || customer.phone),
      isOnline: false,
      lastSeen: customer.updatedAt ? new Date(customer.updatedAt) : undefined,
      phone: customer.phone
    }
  }

  /**
   * Converte MessageDTO para Message (método interno)
   */
  private convertMessage(message: MessageDTO) {
    return {
      id: message.id,
      content: message.content,
      timestamp: new Date(message.createdAt),
      senderId: message.senderId || message.conversationId,
      messageType: this.mapMessageType(message.messageType),
      status: this.mapMessageStatus(message.status),
      isFromUser: message.senderType === 'AGENT'
    }
  }

  /**
   * Formata telefone como nome
   */
  private formatPhoneAsName(phone: string): string {
    const cleaned = phone.replace(/\D/g, '')
    if (cleaned.length === 11 && cleaned.startsWith('55')) {
      const ddd = cleaned.substring(2, 4)
      const number = cleaned.substring(4)
      const firstPart = number.substring(0, number.length - 4)
      const lastPart = number.substring(number.length - 4)
      return `(${ddd}) ${firstPart}-${lastPart}`
    }
    return phone
  }

  /**
   * Gera URL de avatar
   */
  private generateAvatarUrl(nameOrPhone: string): string {
    const initials = this.getInitials(nameOrPhone)
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(initials)}&background=random&size=150`
  }

  /**
   * Extrai iniciais
   */
  private getInitials(nameOrPhone: string): string {
    if (nameOrPhone.includes('(') || nameOrPhone.includes('+')) {
      const digits = nameOrPhone.replace(/\D/g, '')
      return digits.substring(0, 2)
    }
    
    const words = nameOrPhone.trim().split(' ')
    if (words.length >= 2) {
      return words[0][0] + words[1][0]
    }
    return words[0].substring(0, 2)
  }

  /**
   * Mapeia tipo de mensagem
   */
  private mapMessageType(type: string): 'text' | 'image' | 'file' | 'audio' {
    const typeMap: Record<string, 'text' | 'image' | 'file' | 'audio'> = {
      'TEXT': 'text',
      'IMAGE': 'image',
      'AUDIO': 'audio',
      'FILE': 'file',
      'LOCATION': 'text',
      'CONTACT': 'text'
    }
    return typeMap[type] || 'text'
  }

  /**
   * Mapeia status de mensagem
   */
  private mapMessageStatus(status: string): 'sending' | 'sent' | 'delivered' | 'read' {
    const statusMap: Record<string, 'sending' | 'sent' | 'delivered' | 'read'> = {
      'SENDING': 'sending',
      'SENT': 'sent',
      'DELIVERED': 'delivered',
      'READ': 'read',
      'FAILED': 'sent'
    }
    return statusMap[status] || 'sent'
  }

  /**
   * Mapeia status do backend para frontend
   */
  private mapStatus(backendStatus: string): ChatStatus {
    const statusMap: Record<string, ChatStatus> = {
      'ENTRADA': 'entrada',
      'ESPERANDO': 'esperando', 
      'FINALIZADOS': 'finalizados'
    }
    
    return statusMap[backendStatus] || 'entrada'
  }

  /**
   * Mapeia status do frontend para backend
   */
  mapStatusToBackend(frontendStatus: ChatStatus): string {
    const statusMap: Record<ChatStatus, string> = {
      'entrada': 'ENTRADA',
      'esperando': 'ESPERANDO',
      'finalizados': 'FINALIZADOS'
    }
    
    return statusMap[frontendStatus]
  }

  /**
   * Calcula quantidade de mensagens não lidas
   * Por enquanto retorna 0, será implementado quando tiver as mensagens completas
   */
  private calculateUnreadCount(): number {
    // TODO: implementar cálculo real baseado nas mensagens
    return 0
  }

  /**
   * Converte array de ConversationDTO para array de Chat
   */
  toChatArray(dtos: ConversationDTO[]): Chat[] {
    return dtos.map(dto => this.toChat(dto))
  }

  /**
   * Atualiza um chat existente com dados do DTO
   */
  updateChat(existingChat: Chat, dto: ConversationDTO): Chat {
    return {
      ...existingChat,
      contact: dto.customer ? this.convertCustomer(dto.customer) : existingChat.contact,
      isPinned: dto.isPinned,
      status: this.mapStatus(dto.status),
      assignedAgent: dto.assignedUser?.name,
      updatedAt: new Date(dto.updatedAt),
      // Manter mensagens existentes se não houver novas
      lastMessage: dto.lastMessage ? this.convertMessage(dto.lastMessage) : existingChat.lastMessage
    }
  }

  /**
   * Cria um request DTO para criar nova conversa
   */
  toCreateRequest(customerId: string, departmentId?: string) {
    return {
      customerId,
      departmentId,
      channel: 'WHATSAPP' as const
    }
  }

  /**
   * Cria um request DTO para atualizar conversa
   */
  toUpdateRequest(data: {
    assignedUserId?: string
    status?: ChatStatus
    isPinned?: boolean
  }) {
    return {
      assignedUserId: data.assignedUserId,
      status: data.status ? this.mapStatusToBackend(data.status) : undefined,
      isPinned: data.isPinned
    }
  }

  /**
   * Verifica se uma conversa precisa ser atualizada com base nos dados do DTO
   */
  needsUpdate(chat: Chat, dto: ConversationDTO): boolean {
    return (
      chat.updatedAt.getTime() !== new Date(dto.updatedAt).getTime() ||
      chat.isPinned !== dto.isPinned ||
      chat.status !== this.mapStatus(dto.status) ||
      chat.assignedAgent !== dto.assignedUser?.name
    )
  }

  /**
   * Mescla dados de uma conversa local com dados do servidor
   */
  mergeWithServerData(localChat: Chat, serverDto: ConversationDTO): Chat {
    // Prioriza dados do servidor para campos de controle
    const baseChat = this.toChat(serverDto)
    
    // Mantém mensagens locais se forem mais recentes
    return {
      ...baseChat,
      messages: localChat.messages.length > 0 ? localChat.messages : baseChat.messages,
      unreadCount: localChat.unreadCount // Mantém contagem local
    }
  }
}

export const conversationAdapter = new ConversationAdapter()
export default conversationAdapter