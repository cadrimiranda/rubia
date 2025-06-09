import { webSocketClient } from './client'
import type { WebSocketMessage } from './client'
import { useChatStore } from '../store/useChatStore'
import type { Message, ChatStatus } from '../types'

export interface WebSocketEventData {
  NEW_MESSAGE: {
    message: Message
    conversationId: string
  }
  MESSAGE_STATUS_UPDATED: {
    messageId: string
    status: string
  }
  CONVERSATION_ASSIGNED: {
    conversationId: string
    agentId: string
    agentName: string
  }
  CONVERSATION_STATUS_CHANGED: {
    conversationId: string
    status: ChatStatus
    updatedBy: string
  }
  USER_TYPING: {
    conversationId: string
    userId: string
    userName: string
    isTyping: boolean
  }
  USER_STOP_TYPING: {
    conversationId: string
    userId: string
  }
  USER_ONLINE: {
    userId: string
    userName: string
  }
  USER_OFFLINE: {
    userId: string
    userName: string
  }
  CONVERSATION_UPDATED: {
    conversationId: string
    updates: Record<string, unknown>
  }
}

class WebSocketEventHandlers {
  private store = useChatStore.getState()

  constructor() {
    // Registrar handlers no WebSocket client
    webSocketClient.on({
      onMessage: this.handleMessage.bind(this),
      onConnect: this.handleConnect.bind(this),
      onDisconnect: this.handleDisconnect.bind(this),
      onError: this.handleError.bind(this),
      onReconnecting: this.handleReconnecting.bind(this),
      onReconnected: this.handleReconnected.bind(this)
    })
  }

  private handleMessage = (message: WebSocketMessage) => {
    switch (message.type) {
      case 'NEW_MESSAGE':
        this.handleNewMessage(message.data as WebSocketEventData['NEW_MESSAGE'])
        break
        
      case 'MESSAGE_STATUS_UPDATED':
        this.handleMessageStatusUpdate(message.data as WebSocketEventData['MESSAGE_STATUS_UPDATED'])
        break
        
      case 'CONVERSATION_ASSIGNED':
        this.handleConversationAssigned(message.data as WebSocketEventData['CONVERSATION_ASSIGNED'])
        break
        
      case 'CONVERSATION_STATUS_CHANGED':
        this.handleConversationStatusChanged(message.data as WebSocketEventData['CONVERSATION_STATUS_CHANGED'])
        break
        
      case 'USER_TYPING':
        this.handleUserTyping(message.data as WebSocketEventData['USER_TYPING'])
        break
        
      case 'USER_STOP_TYPING':
        this.handleUserStopTyping(message.data as WebSocketEventData['USER_STOP_TYPING'])
        break
        
      case 'USER_ONLINE':
        this.handleUserOnline(message.data as WebSocketEventData['USER_ONLINE'])
        break
        
      case 'USER_OFFLINE':
        this.handleUserOffline(message.data as WebSocketEventData['USER_OFFLINE'])
        break
        
      case 'CONVERSATION_UPDATED':
        this.handleConversationUpdated(message.data as WebSocketEventData['CONVERSATION_UPDATED'])
        break
        
      default:
        console.log('Evento WebSocket não tratado:', message.type, message.data)
    }
  }

  private handleNewMessage = (data: WebSocketEventData['NEW_MESSAGE']) => {
    const { message, conversationId } = data
    
    // Adicionar mensagem à conversa
    this.store.addMessage(conversationId, message)
    
    // Atualizar última mensagem da conversa
    this.store.updateConversationLastMessage(conversationId, message)
    
    // Se a conversa não estiver ativa, mostrar notificação
    const { activeConversationId } = useChatStore.getState()
    if (activeConversationId !== conversationId) {
      // Criar evento personalizado para notificação
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('chat:notification', {
          detail: {
            type: 'info',
            message: 'Nova mensagem',
            description: `${(message as Record<string, unknown>).senderName || 'Usuário'}: ${message.content.substring(0, 50)}${message.content.length > 50 ? '...' : ''}`,
            duration: 5,
            onClick: () => {
              this.store.setActiveConversation(conversationId)
            }
          }
        }))
      }
    }
    
    // Reproduzir som de notificação (se habilitado)
    this.playNotificationSound()
  }

  private handleMessageStatusUpdate = (data: WebSocketEventData['MESSAGE_STATUS_UPDATED']) => {
    const { messageId, status } = data
    this.store.updateMessageStatus(messageId, status)
  }

  private handleConversationAssigned = (data: WebSocketEventData['CONVERSATION_ASSIGNED']) => {
    const { conversationId, agentId, agentName } = data
    
    this.store.updateConversationAgent(conversationId, agentId, agentName)
    
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'success',
          message: 'Conversa atribuída',
          description: `Conversa atribuída para ${agentName}`,
          duration: 3
        }
      }))
    }
  }

  private handleConversationStatusChanged = (data: WebSocketEventData['CONVERSATION_STATUS_CHANGED']) => {
    const { conversationId, status, updatedBy } = data
    
    this.store.updateConversationStatus(conversationId, status)
    
    // Mover conversa para a categoria correta
    this.store.moveConversationToStatus(conversationId, status)
    
    const statusText = this.getStatusText(status)
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'info',
          message: 'Status da conversa alterado',
          description: `Conversa movida para ${statusText} por ${updatedBy}`,
          duration: 3
        }
      }))
    }
  }

  private handleUserTyping = (data: WebSocketEventData['USER_TYPING']) => {
    const { conversationId, userId, userName } = data
    this.store.setUserTyping(conversationId, userId, userName, true)
  }

  private handleUserStopTyping = (data: WebSocketEventData['USER_STOP_TYPING']) => {
    const { conversationId, userId } = data
    this.store.setUserTyping(conversationId, userId, '', false)
  }

  private handleUserOnline = (data: WebSocketEventData['USER_ONLINE']) => {
    const { userId, userName } = data
    this.store.setUserOnlineStatus(userId, true)
    
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'info',
          message: 'Usuário online',
          description: `${userName} entrou online`,
          duration: 2
        }
      }))
    }
  }

  private handleUserOffline = (data: WebSocketEventData['USER_OFFLINE']) => {
    const { userId, userName } = data
    this.store.setUserOnlineStatus(userId, false)
    
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'info',
          message: 'Usuário offline',
          description: `${userName} saiu offline`,
          duration: 2
        }
      }))
    }
  }

  private handleConversationUpdated = (data: WebSocketEventData['CONVERSATION_UPDATED']) => {
    const { conversationId, updates } = data
    this.store.updateConversation(conversationId, updates)
  }

  private handleConnect = () => {
    console.log('WebSocket conectado')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'success',
          message: 'Conectado',
          description: 'Conectado ao servidor em tempo real',
          duration: 2
        }
      }))
    }
  }

  private handleDisconnect = () => {
    console.log('WebSocket desconectado')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'warning',
          message: 'Desconectado',
          description: 'Conexão em tempo real perdida',
          duration: 3
        }
      }))
    }
  }

  private handleError = () => {
    console.error('Erro no WebSocket')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'error',
          message: 'Erro de conexão',
          description: 'Erro na conexão em tempo real',
          duration: 5
        }
      }))
    }
  }

  private handleReconnecting = () => {
    console.log('Tentando reconectar...')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'info',
          message: 'Reconectando...',
          description: 'Tentando restabelecer conexão',
          duration: 2
        }
      }))
    }
  }

  private handleReconnected = () => {
    console.log('Reconectado com sucesso')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('chat:notification', {
        detail: {
          type: 'success',
          message: 'Reconectado',
          description: 'Conexão em tempo real restabelecida',
          duration: 3
        }
      }))
    }
  }

  private getStatusText = (status: ChatStatus): string => {
    const statusMap: Record<ChatStatus, string> = {
      entrada: 'Entrada',
      esperando: 'Esperando',
      finalizados: 'Finalizados'
    }
    return statusMap[status] || status
  }

  private playNotificationSound = () => {
    try {
      // Verificar se áudio está habilitado nas configurações
      const audioEnabled = localStorage.getItem('notifications-audio') !== 'false'
      if (!audioEnabled) return

      // Reproduzir som de notificação
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.3
      audio.play().catch(() => {
        // Ignorar erros de reprodução (ex: política de autoplay)
      })
    } catch {
      // Ignorar erros de áudio
    }
  }

  // Métodos públicos para controle
  public sendTyping(conversationId: string) {
    webSocketClient.sendTyping(conversationId)
  }

  public stopTyping(conversationId: string) {
    webSocketClient.stopTyping(conversationId)
  }

  public sendUserStatus(isOnline: boolean) {
    webSocketClient.sendUserStatus(isOnline)
  }

  public destroy() {
    webSocketClient.off()
  }
}

// Instância singleton
export const webSocketEventHandlers = new WebSocketEventHandlers()
export default webSocketEventHandlers