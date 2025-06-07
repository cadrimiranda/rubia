import { useEffect } from 'react'
import { useChatStore } from '../store/useChatStore'
import { webSocketManager } from '../websocket'

interface ChatDataProviderProps {
  children: React.ReactNode
}

/**
 * Provider que inicializa os dados do chat carregando do backend
 */
export const ChatDataProvider: React.FC<ChatDataProviderProps> = ({ children }) => {
  const store = useChatStore()
  
  useEffect(() => {
    const initializeData = async () => {
      try {
        // Carrega conversas iniciais do status atual
        await store.loadConversations(store.currentStatus, 0)
        
        // Inicializa WebSocket para tempo real
        await webSocketManager.initialize()
      } catch (error) {
        console.error('Erro ao inicializar dados do chat:', error)
        // Define erro no store para exibir ao usuário
        store.clearError()
      }
    }
    
    initializeData()
    
    // Cleanup ao desmontar
    return () => {
      webSocketManager.disconnect()
    }
  }, [store])
  
  return <>{children}</>
}

export default ChatDataProvider