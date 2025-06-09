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
        console.log('Initializing chat data...')
        // Carrega conversas iniciais do status atual
        await store.loadConversations(store.currentStatus, 0)
        console.log('Conversations loaded successfully')
        
        // Inicializa WebSocket para tempo real (desabilitado temporariamente)
        console.log('WebSocket initialization skipped (temporarily disabled)')
        // await webSocketManager.initialize()
      } catch (error) {
        console.error('Erro ao inicializar dados do chat:', error)
        // Não propagar o erro - apenas registrar
        // Deixar que a UI mostre estado vazio ao invés de erro
      }
    }
    
    initializeData()
    
    // Cleanup ao desmontar
    return () => {
      // webSocketManager.disconnect() // Desabilitado temporariamente
    }
  }, []) // Remover dependência do store para evitar loop infinito
  
  return <>{children}</>
}

export default ChatDataProvider