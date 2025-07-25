import { webSocketClient } from './client'
import { webSocketEventHandlers } from './eventHandlers'
import { useAuthStore } from '../store/useAuthStore'

class WebSocketManager {
  private isInitialized = false
  private reconnectOnAuth = true

  async initialize() {
    if (this.isInitialized) return

    console.log('🚀 Initializing WebSocket Manager...')

    // Verificar se usuário está autenticado
    const isAuthenticated = useAuthStore.getState().isAuthenticated
    if (!isAuthenticated) return

    try {
      // Conectar ao WebSocket
      await webSocketClient.connect()
      
      // Marcar como inicializado
      this.isInitialized = true
      
      console.log('WebSocket Manager inicializado com sucesso')
    } catch (error) {
      console.error('Erro ao inicializar WebSocket Manager:', error)
    }
  }

  disconnect() {
    if (!this.isInitialized) return

    try {
      webSocketClient.disconnect()
      this.isInitialized = false
      console.log('WebSocket Manager desconectado')
    } catch (error) {
      console.error('Erro ao desconectar WebSocket Manager:', error)
    }
  }

  // Reconectar quando usuário fizer login
  async reconnectOnLogin() {
    if (this.reconnectOnAuth) {
      await this.initialize()
    }
  }

  // Desconectar quando usuário fizer logout
  disconnectOnLogout() {
    this.disconnect()
  }

  // Verificar se está conectado
  isConnected(): boolean {
    return this.isInitialized && webSocketClient.isConnected()
  }

  // Métodos de conveniência
  sendTyping(conversationId: string) {
    if (this.isConnected()) {
      webSocketEventHandlers.sendTyping(conversationId)
    }
  }

  stopTyping(conversationId: string) {
    if (this.isConnected()) {
      webSocketEventHandlers.stopTyping(conversationId)
    }
  }

  sendUserStatus(isOnline: boolean) {
    if (this.isConnected()) {
      webSocketEventHandlers.sendUserStatus(isOnline)
    }
  }

  destroy() {
    this.disconnect()
    webSocketEventHandlers.destroy()
  }
}

// Instância singleton
export const webSocketManager = new WebSocketManager()

// Hook para facilitar o uso
export const useWebSocket = () => {
  return {
    initialize: () => webSocketManager.initialize(),
    disconnect: () => webSocketManager.disconnect(),
    isConnected: () => webSocketManager.isConnected(),
    sendTyping: (conversationId: string) => webSocketManager.sendTyping(conversationId),
    stopTyping: (conversationId: string) => webSocketManager.stopTyping(conversationId),
    sendUserStatus: (isOnline: boolean) => webSocketManager.sendUserStatus(isOnline)
  }
}

export default webSocketManager