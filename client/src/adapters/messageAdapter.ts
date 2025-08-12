import type { MessageDTO, SenderType, MessageType as BackendMessageType, MessageStatus as BackendMessageStatus } from '../api/types'
import type { Message, MessageStatus } from '../types'

class MessageAdapter {
  /**
   * Converte MessageDTO do backend para Message do frontend
   */
  toMessage(dto: MessageDTO): Message {
    return {
      id: dto.id,
      content: dto.content,
      timestamp: new Date(dto.createdAt),
      senderId: this.mapSenderId(dto),
      messageType: this.mapMessageType(dto.messageType),
      status: this.mapStatus(dto.status),
      isFromUser: this.isFromUser(dto.senderType),
      mediaUrl: dto.mediaUrl,
      mimeType: dto.mimeType,
      externalMessageId: dto.externalMessageId,
      isAiGenerated: dto.isAiGenerated || false,
      aiConfidence: dto.aiConfidence ? Number(dto.aiConfidence) : undefined,
      deliveredAt: dto.deliveredAt ? new Date(dto.deliveredAt) : undefined,
      readAt: dto.readAt ? new Date(dto.readAt) : undefined
    }
  }

  /**
   * Determina o senderId baseado no tipo de remetente
   */
  private mapSenderId(dto: MessageDTO): string {
    // Se tem senderId (agente), usa ele
    if (dto.senderId) {
      return dto.senderId
    }
    
    // Se é do customer, usa o conversationId como referência
    if (dto.senderType === 'CUSTOMER') {
      return dto.conversationId
    }
    
    // Para AI e SYSTEM, usa um ID padrão
    return dto.senderType.toLowerCase()
  }

  /**
   * Mapeia tipo de mensagem do backend para frontend
   */
  private mapMessageType(backendType: BackendMessageType): 'text' | 'image' | 'file' | 'audio' {
    const typeMap: Record<BackendMessageType, 'text' | 'image' | 'file' | 'audio'> = {
      'TEXT': 'text',
      'IMAGE': 'image',
      'AUDIO': 'audio',
      'FILE': 'file',
      'LOCATION': 'text', // Tratamos location como text
      'CONTACT': 'text'   // Tratamos contact como text
    }
    
    return typeMap[backendType] || 'text'
  }

  /**
   * Mapeia status do backend para frontend
   */
  private mapStatus(backendStatus: BackendMessageStatus): MessageStatus {
    const statusMap: Record<BackendMessageStatus, MessageStatus> = {
      'DRAFT': 'DRAFT' as MessageStatus,
      'SENDING': 'sending',
      'SENT': 'sent',
      'DELIVERED': 'delivered',
      'READ': 'read',
      'FAILED': 'sent' // Mapeamos failed para sent no frontend por enquanto
    }
    
    return statusMap[backendStatus] || 'sent'
  }

  /**
   * Determina se a mensagem é do usuário (agente)
   */
  private isFromUser(senderType: SenderType): boolean {
    return senderType === 'AGENT'
  }

  /**
   * Mapeia status do frontend para backend
   */
  mapStatusToBackend(frontendStatus: MessageStatus): BackendMessageStatus {
    const statusMap: Record<MessageStatus, BackendMessageStatus> = {
      'sending': 'SENDING',
      'sent': 'SENT',
      'delivered': 'DELIVERED',
      'read': 'READ'
    }
    
    return statusMap[frontendStatus] || 'SENT'
  }

  /**
   * Mapeia tipo de mensagem do frontend para backend
   */
  mapMessageTypeToBackend(frontendType: 'text' | 'image' | 'file' | 'audio'): BackendMessageType {
    const typeMap: Record<'text' | 'image' | 'file' | 'audio', BackendMessageType> = {
      'text': 'TEXT',
      'image': 'IMAGE',
      'audio': 'AUDIO',
      'file': 'FILE'
    }
    
    return typeMap[frontendType] || 'TEXT'
  }

  /**
   * Converte array de MessageDTO para array de Message
   */
  toMessageArray(dtos: MessageDTO[]): Message[] {
    if (!dtos || !Array.isArray(dtos)) {
      console.warn('⚠️ MessageAdapter.toMessageArray: dtos is not a valid array:', dtos);
      return [];
    }
    return dtos.map(dto => this.toMessage(dto))
  }

  /**
   * Cria um request DTO para enviar nova mensagem
   */
  toCreateRequest(content: string, messageType?: 'text' | 'image' | 'file' | 'audio', mediaUrl?: string, isAiGenerated?: boolean) {
    return {
      content,
      messageType: messageType ? this.mapMessageTypeToBackend(messageType) : 'TEXT',
      mediaUrl,
      isAiGenerated: isAiGenerated || false
    }
  }

  /**
   * Cria uma mensagem temporária para otimistic updates
   */
  createTempMessage(content: string, messageType: 'text' | 'image' | 'file' | 'audio' = 'text', mediaUrl?: string): Message {
    return {
      id: `temp-${Date.now()}-${Math.random()}`,
      content,
      timestamp: new Date(),
      senderId: 'agent', // Assumimos que é sempre do agente
      messageType,
      status: 'sending',
      isFromUser: true,
      mediaUrl,
      isAiGenerated: false
    }
  }

  /**
   * Atualiza uma mensagem existente com dados do DTO
   */
  updateMessage(existingMessage: Message, dto: MessageDTO): Message {
    return {
      ...existingMessage,
      content: dto.content,
      status: this.mapStatus(dto.status),
      timestamp: new Date(dto.createdAt),
      mediaUrl: dto.mediaUrl,
      externalMessageId: dto.externalMessageId,
      isAiGenerated: dto.isAiGenerated || false,
      aiConfidence: dto.aiConfidence ? Number(dto.aiConfidence) : undefined,
      deliveredAt: dto.deliveredAt ? new Date(dto.deliveredAt) : undefined,
      readAt: dto.readAt ? new Date(dto.readAt) : undefined
    }
  }

  /**
   * Verifica se uma mensagem é temporária (otimistic update)
   */
  isTempMessage(message: Message): boolean {
    return message.id.startsWith('temp-')
  }

  /**
   * Substitui uma mensagem temporária por uma real
   */
  replaceTempMessage(messages: Message[], tempId: string, newMessage: Message): Message[] {
    return messages.map(msg => 
      msg.id === tempId ? newMessage : msg
    )
  }

  /**
   * Remove uma mensagem temporária (em caso de erro)
   */
  removeTempMessage(messages: Message[], tempId: string): Message[] {
    return messages.filter(msg => msg.id !== tempId)
  }

  /**
   * Ordena mensagens por timestamp
   */
  sortMessages(messages: Message[]): Message[] {
    return [...messages].sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())
  }

  /**
   * Mescla mensagens locais com mensagens do servidor
   */
  mergeMessages(localMessages: Message[], serverMessages: Message[]): Message[] {
    const serverDtos = serverMessages
    const tempMessages = localMessages.filter(msg => this.isTempMessage(msg))
    
    // Combina mensagens do servidor com temporárias
    const allMessages = [...serverDtos, ...tempMessages]
    
    // Remove duplicatas baseadas no ID
    const uniqueMessages = allMessages.filter((msg, index, arr) => 
      arr.findIndex(m => m.id === msg.id) === index
    )
    
    return this.sortMessages(uniqueMessages)
  }

  /**
   * Calcula contagem de mensagens não lidas
   */
  calculateUnreadCount(messages: Message[]): number {
    return messages.filter(msg => 
      !msg.isFromUser && msg.status !== 'read'
    ).length
  }

  /**
   * Marca mensagens como lidas
   */
  markMessagesAsRead(messages: Message[]): Message[] {
    return messages.map(msg => ({
      ...msg,
      status: msg.isFromUser ? msg.status : 'read'
    }))
  }

  /**
   * Converte Message (nova interface) para Message (interface legada de types/types.ts)
   */
  toLegacyMessage(message: Message): any {
    return {
      id: message.id,
      senderId: message.senderId, 
      content: message.content,
      timestamp: message.timestamp.toISOString(),
      isAI: !message.isFromUser, // Inverte a lógica
      messageType: message.messageType,
      mediaUrl: message.mediaUrl,
      mimeType: message.mimeType,
      audioDuration: message.audioDuration,
      attachments: [],
      media: [],
      campaignId: undefined
    }
  }

  /**
   * Converte array de Messages para o formato legado
   */
  toLegacyMessageArray(messages: Message[]): any[] {
    return messages.map(msg => this.toLegacyMessage(msg))
  }
}

export const messageAdapter = new MessageAdapter()
export default messageAdapter