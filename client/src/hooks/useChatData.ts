import { useEffect } from 'react'
import { useChatStore } from '../store/useChatStore'

/**
 * Hook para inicializar e gerenciar dados do chat
 */
export const useChatData = () => {
  const store = useChatStore()
  
  // Carrega conversas iniciais quando o componente monta
  useEffect(() => {
    const initializeData = async () => {
      try {
        await store.loadConversations(store.currentStatus, 0)
      } catch (error) {
        console.error('Erro ao inicializar dados do chat:', error)
      }
    }
    
    initializeData()
  }, [store])
  
  // Recarrega quando muda o status
  useEffect(() => {
    const loadStatusData = async () => {
      await store.loadConversations(store.currentStatus, 0)
    }
    
    loadStatusData()
  }, [store.currentStatus, store])
  
  return {
    // Estado
    chats: store.getFilteredChats(),
    activeChat: store.activeChat,
    currentStatus: store.currentStatus,
    searchQuery: store.searchQuery,
    
    // Loading states
    isLoading: store.isLoading,
    isLoadingMessages: store.isLoadingMessages,
    isSending: store.isSending,
    
    // Paginação
    hasMore: store.hasMore,
    totalChats: store.totalChats,
    
    // Erro
    error: store.error,
    
    // Ações
    setActiveChat: store.setActiveChat,
    setCurrentStatus: store.setCurrentStatus,
    setSearchQuery: store.setSearchQuery,
    sendMessage: store.sendMessage,
    markAsRead: store.markAsRead,
    pinConversation: store.pinConversation,
    changeStatus: store.changeStatus,
    blockCustomer: store.blockCustomer,
    assignToAgent: store.assignToAgent,
    addTag: store.addTag,
    removeTag: store.removeTag,
    
    // Utilitários
    refreshConversations: store.refreshConversations,
    clearError: store.clearError,
    loadMoreConversations: () => store.loadConversations(store.currentStatus, store.currentPage + 1),
    loadMoreMessages: (chatId: string) => store.loadMessages(chatId, store.messagesCache[chatId]?.page + 1 || 1)
  }
}

/**
 * Hook específico para gerenciar mensagens de uma conversa
 */
export const useChatMessages = (chatId?: string) => {
  const store = useChatStore()
  
  const messages = chatId ? store.messagesCache[chatId]?.messages || [] : []
  const hasMoreMessages = chatId ? store.messagesCache[chatId]?.hasMore || false : false
  
  useEffect(() => {
    if (chatId && !store.messagesCache[chatId]) {
      store.loadMessages(chatId, 0)
    }
  }, [chatId, store])
  
  return {
    messages,
    hasMoreMessages,
    isLoadingMessages: store.isLoadingMessages,
    loadMoreMessages: () => chatId && store.loadMessages(chatId, store.messagesCache[chatId]?.page + 1 || 1),
    sendMessage: (content: string) => chatId && store.sendMessage(chatId, content),
    markAsRead: () => chatId && store.markAsRead(chatId)
  }
}

/**
 * Hook para gerenciar busca
 */
export const useSearch = () => {
  const store = useChatStore()
  
  return {
    searchQuery: store.searchQuery,
    setSearchQuery: store.setSearchQuery,
    searchConversations: store.searchConversations,
    searchMessages: store.searchMessages,
    isLoading: store.isLoading
  }
}

/**
 * Hook para gerenciar paginação
 */
export const usePagination = () => {
  const store = useChatStore()
  
  return {
    currentPage: store.currentPage,
    hasMore: store.hasMore,
    totalChats: store.totalChats,
    isLoading: store.isLoading,
    loadMore: () => store.loadConversations(store.currentStatus, store.currentPage + 1),
    refresh: store.refreshConversations
  }
}

/**
 * Hook para gerenciar erros
 */
export const useErrorHandling = () => {
  const store = useChatStore()
  
  useEffect(() => {
    if (store.error) {
      // Pode integrar com sistema de notificações aqui
      console.error('Chat error:', store.error)
      
      // Auto-clear erro após 5 segundos
      const timer = setTimeout(() => {
        store.clearError()
      }, 5000)
      
      return () => clearTimeout(timer)
    }
  }, [store.error, store])
  
  return {
    error: store.error,
    clearError: store.clearError
  }
}