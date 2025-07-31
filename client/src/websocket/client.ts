import { authService } from '../auth/authService'

export interface WebSocketMessage {
  type: string
  data: unknown
  timestamp?: string
  id?: string
}

export interface WebSocketEventHandlers {
  onMessage: (message: WebSocketMessage) => void
  onConnect: () => void
  onDisconnect: () => void
  onError: (error: Event) => void
  onReconnecting: () => void
  onReconnected: () => void
}

export type WebSocketEventType = 
  | 'NEW_MESSAGE'
  | 'MESSAGE_STATUS_UPDATED' 
  | 'CONVERSATION_ASSIGNED'
  | 'CONVERSATION_STATUS_CHANGED'
  | 'USER_TYPING'
  | 'USER_STOP_TYPING'
  | 'USER_ONLINE'
  | 'USER_OFFLINE'
  | 'CONVERSATION_UPDATED'
  | 'PING'
  | 'PONG'

class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private heartbeatInterval: number | null = null
  private reconnectTimeout: number | null = null
  private isManuallyDisconnected = false
  private eventHandlers: Partial<WebSocketEventHandlers> = {}

  constructor(url?: string) {
    this.url = url || import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'
  }

  /**
   * Conecta ao WebSocket
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        // Verificar se já está conectado
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          resolve()
          return
        }

        // Obter token de autenticação
        const token = authService.getAccessToken()
        if (!token) {
          reject(new Error('Token de autenticação não encontrado'))
          return
        }

        // Criar conexão WebSocket com token
        const wsUrl = `${this.url}?token=${encodeURIComponent(token)}`
        this.ws = new WebSocket(wsUrl)

        this.ws.onopen = () => {
          this.reconnectAttempts = 0
          this.isManuallyDisconnected = false
          this.startHeartbeat()
          this.eventHandlers.onConnect?.()
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('Erro ao parsear mensagem WebSocket:', error)
          }
        }

        this.ws.onclose = (event) => {
          this.stopHeartbeat()
          this.eventHandlers.onDisconnect?.()

          // Tentar reconectar se não foi desconexão manual
          if (!this.isManuallyDisconnected && this.shouldReconnect(event.code)) {
            this.attemptReconnect()
          }
        }

        this.ws.onerror = (error) => {
          console.error('Erro no WebSocket:', error)
          this.eventHandlers.onError?.(error)
          reject(error)
        }

      } catch (error) {
        console.error('Erro ao conectar WebSocket:', error)
        reject(error)
      }
    })
  }

  /**
   * Desconecta do WebSocket
   */
  disconnect(): void {
    this.isManuallyDisconnected = true
    this.stopHeartbeat()
    
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }

    if (this.ws) {
      this.ws.close(1000, 'Desconexão manual')
      this.ws = null
    }
  }

  /**
   * Envia mensagem pelo WebSocket
   */
  send(type: WebSocketEventType, data: unknown): void {
    if (!this.isConnected()) {
      console.warn('WebSocket não conectado, tentando enviar:', type, data)
      return
    }

    const message: WebSocketMessage = {
      type,
      data,
      timestamp: new Date().toISOString(),
      id: `${Date.now()}-${Math.random()}`
    }

    try {
      this.ws!.send(JSON.stringify(message))
    } catch (error) {
      console.error('Erro ao enviar mensagem WebSocket:', error)
    }
  }

  /**
   * Verifica se está conectado
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }

  /**
   * Registra event handlers
   */
  on(handlers: Partial<WebSocketEventHandlers>): void {
    this.eventHandlers = { ...this.eventHandlers, ...handlers }
  }

  /**
   * Remove event handlers
   */
  off(event?: keyof WebSocketEventHandlers): void {
    if (event) {
      delete this.eventHandlers[event]
    } else {
      this.eventHandlers = {}
    }
  }

  /**
   * Métodos de conveniência para eventos específicos
   */
  sendTyping(conversationId: string): void {
    this.send('USER_TYPING', { conversationId })
  }

  stopTyping(conversationId: string): void {
    this.send('USER_STOP_TYPING', { conversationId })
  }

  sendUserStatus(isOnline: boolean): void {
    this.send(isOnline ? 'USER_ONLINE' : 'USER_OFFLINE', { isOnline })
  }

  /**
   * Métodos privados
   */
  private handleMessage(message: WebSocketMessage): void {
    // Lidar com mensagens de sistema
    if (message.type === 'PING') {
      this.send('PONG', { timestamp: new Date().toISOString() })
      return
    }

    if (message.type === 'PONG') {
      // Heartbeat response recebido
      return
    }

    // Delegar para handlers registrados
    this.eventHandlers.onMessage?.(message)

    // Dispatch event global para outros sistemas
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent(`ws:${message.type.toLowerCase()}`, {
        detail: message
      }))
    }
  }

  private shouldReconnect(code: number): boolean {
    // Não reconectar para códigos específicos
    const noReconnectCodes = [
      1000, // Normal closure
      1001, // Going away
      1005, // No status received
      4000, // Custom: Authentication failed
      4001, // Custom: Invalid token
      4002, // Custom: User blocked
    ]
    
    return !noReconnectCodes.includes(code)
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Máximo de tentativas de reconexão atingido')
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1) // Exponential backoff

    
    this.eventHandlers.onReconnecting?.()

    this.reconnectTimeout = window.setTimeout(async () => {
      try {
        await this.connect()
        this.eventHandlers.onReconnected?.()
      } catch (error) {
        console.error('Falha na reconexão:', error)
        this.attemptReconnect()
      }
    }, delay)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    
    // Enviar ping a cada 30 segundos
    this.heartbeatInterval = window.setInterval(() => {
      if (this.isConnected()) {
        this.send('PING', { timestamp: new Date().toISOString() })
      }
    }, 30000)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  /**
   * Cleanup ao destruir
   */
  destroy(): void {
    this.disconnect()
    this.off()
  }
}

// Instância singleton
export const webSocketClient = new WebSocketClient()
export default webSocketClient