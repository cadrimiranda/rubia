import type { ConversationDTO, MessageDTO } from '../api/types'
import type { Chat, ChatStatus } from '../types'


class ConversationAdapter {
  /**
   * Converte ConversationDTO do backend para Chat do frontend
   */
  toChat(dto: ConversationDTO): Chat {
    return {
      id: dto.id,
      contact: {
        id: dto.customerId,
        name: dto.customerName || this.formatPhoneAsName(dto.customerPhone || ''),
        avatar: this.generateAvatarUrl(dto.customerName || dto.customerPhone || ''),
        isOnline: false,
        phone: dto.customerPhone || ''
      },
      messages: dto.lastMessage ? [this.convertMessage(dto.lastMessage)] : [],
      lastMessage: dto.lastMessage ? this.convertMessage(dto.lastMessage) : undefined,
      unreadCount: this.calculateUnreadCount(),
      isPinned: dto.isPinned || false,
      status: this.mapStatus(dto.status),
      assignedAgent: dto.assignedUserName,
      tags: [],
      priority: dto.priority || 0,
      channel: dto.channel || 'WHATSAPP',
      closedAt: dto.closedAt ? new Date(dto.closedAt) : undefined,
      createdAt: new Date(dto.createdAt),
      updatedAt: new Date(dto.updatedAt)
    }
  }

  // Método removido pois não estava sendo usado

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
      'ENTRADA': 'ativos',
      'ESPERANDO': 'aguardando', 
      'FINALIZADOS': 'inativo'
    }
    
    return statusMap[backendStatus] || 'ativos'
  }

  /**
   * Mapeia status do frontend para backend
   */
  mapStatusToBackend(frontendStatus: ChatStatus): string {
    const statusMap: Record<ChatStatus, string> = {
      'ativos': 'ENTRADA',
      'aguardando': 'ESPERANDO', 
      'inativo': 'FINALIZADOS',
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
    if (!dtos || !Array.isArray(dtos)) {
      console.warn('conversationAdapter.toChatArray: dtos is not a valid array:', dtos)
      return []
    }
    return dtos.map(dto => this.toChat(dto))
  }

  /**
   * Atualiza um chat existente com dados do DTO
   */
  updateChat(existingChat: Chat, dto: ConversationDTO): Chat {
    return {
      ...existingChat,
      contact: {
        id: dto.customerId,
        name: dto.customerName || this.formatPhoneAsName(dto.customerPhone || ''),
        avatar: this.generateAvatarUrl(dto.customerName || dto.customerPhone || ''),
        isOnline: false,
        phone: dto.customerPhone || ''
      },
      isPinned: dto.isPinned || false,
      status: this.mapStatus(dto.status),
      assignedAgent: dto.assignedUserName,
      updatedAt: new Date(dto.updatedAt),
      // Manter mensagens existentes se não houver novas
      lastMessage: dto.lastMessage ? this.convertMessage(dto.lastMessage) : existingChat.lastMessage
    }
  }

  /**
   * Cria um request DTO para criar nova conversa
   */
  toCreateRequest(customerId: string, channel: 'WHATSAPP' = 'WHATSAPP', departmentId?: string, priority?: number) {
    return {
      customerId,
      departmentId,
      channel,
      priority: priority || 1
    }
  }

  /**
   * Cria um request DTO para atualizar conversa
   */
  toUpdateRequest(data: {
    assignedUserId?: string
    status?: ChatStatus
    isPinned?: boolean
    priority?: number
  }) {
    return {
      assignedUserId: data.assignedUserId,
      status: data.status ? this.mapStatusToBackend(data.status) : undefined,
      isPinned: data.isPinned,
      priority: data.priority
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
      chat.assignedAgent !== dto.assignedUserName
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