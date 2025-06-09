import { create } from 'zustand'
import type { Chat, ChatStatus, Message, Tag } from '../types'
import type { ConversationStatus } from '../api/types'
import { conversationApi, messageApi, customerApi } from '../api'
import { conversationAdapter, messageAdapter } from '../adapters'
import { MessageValidator } from '../utils/validation'

interface ChatStoreState {
  // Estado principal
  chats: Chat[]
  activeChat: Chat | null
  currentStatus: ChatStatus
  searchQuery: string
  
  // Estados de carregamento
  isLoading: boolean
  isLoadingMessages: boolean
  isSending: boolean
  
  // Paginação
  currentPage: number
  hasMore: boolean
  totalChats: number
  
  // Erros
  error: string | null
  
  // Cache de mensagens por conversa
  messagesCache: Record<string, {
    messages: Message[]
    page: number
    hasMore: boolean
  }>
  
  // Estados de tempo real
  typingUsers: Record<string, {
    userId: string
    userName: string
    timestamp: number
  }[]>
  onlineUsers: Set<string>
  activeConversationId: string | null
}

interface ChatStoreActions {
  // Navegação
  setActiveChat: (chat: Chat | null) => Promise<void>
  setCurrentStatus: (status: ChatStatus) => void
  setSearchQuery: (query: string) => void
  
  // Carregamento de dados
  loadConversations: (status?: ChatStatus, page?: number) => Promise<void>
  loadMessages: (chatId: string, page?: number) => Promise<void>
  refreshConversations: () => Promise<void>
  
  // Envio de mensagens
  sendMessage: (chatId: string, content: string) => Promise<void>
  
  // Ações de conversa
  markAsRead: (chatId: string) => Promise<void>
  assignToAgent: (chatId: string, agentId: string) => Promise<void>
  changeStatus: (chatId: string, status: ChatStatus) => Promise<void>
  pinConversation: (chatId: string) => Promise<void>
  blockCustomer: (chatId: string) => Promise<void>
  
  // Tags
  addTag: (chatId: string, tag: Tag) => Promise<void>
  removeTag: (chatId: string, tagId: string) => Promise<void>
  
  // Busca
  searchConversations: (query: string) => Promise<void>
  searchMessages: (query: string) => Promise<void>
  
  // WebSocket/Tempo real
  addMessage: (conversationId: string, message: Message) => void
  updateMessageStatus: (messageId: string, status: string) => void
  updateConversationLastMessage: (conversationId: string, message: Message) => void
  updateConversationAgent: (conversationId: string, agentId: string, agentName: string) => void
  updateConversationStatus: (conversationId: string, status: ChatStatus) => void
  moveConversationToStatus: (conversationId: string, status: ChatStatus) => void
  updateConversation: (conversationId: string, updates: Record<string, unknown>) => void
  setUserTyping: (conversationId: string, userId: string, userName: string, isTyping: boolean) => void
  setUserOnlineStatus: (userId: string, isOnline: boolean) => void
  setActiveConversation: (conversationId: string | null) => void
  getTypingUsers: (conversationId: string) => string[]
  
  // Utilitários
  getFilteredChats: () => Chat[]
  clearError: () => void
  clearCache: () => void
}

export const useChatStore = create<ChatStoreState & ChatStoreActions>((set, get) => ({
  // Estado inicial
  chats: [],
  activeChat: null,
  currentStatus: 'entrada',
  searchQuery: '',
  isLoading: false,
  isLoadingMessages: false,
  isSending: false,
  currentPage: 0,
  hasMore: true,
  totalChats: 0,
  error: null,
  messagesCache: {},
  typingUsers: {},
  onlineUsers: new Set(),
  activeConversationId: null,

  // Navegação
  setActiveChat: async (chat: Chat | null) => {
    set({ activeChat: chat })
    
    if (chat) {
      // Carregar mensagens da conversa
      await get().loadMessages(chat.id)
      
      // Marcar como lida se tiver mensagens não lidas
      if (chat.unreadCount > 0) {
        await get().markAsRead(chat.id)
      }
    }
  },

  setCurrentStatus: (status: ChatStatus) => {
    set({ 
      currentStatus: status, 
      activeChat: null,
      currentPage: 0,
      hasMore: true,
      chats: []
    })
    
    // Carrega conversas do novo status
    get().loadConversations(status)
  },

  setSearchQuery: (query: string) => {
    set({ searchQuery: query })
    
    // Se a query estiver vazia, recarrega conversas normais
    if (!query.trim()) {
      get().loadConversations(get().currentStatus)
    } else {
      // Faz busca com debounce
      const timeoutId = setTimeout(() => {
        get().searchConversations(query)
      }, 300)
      
      return () => clearTimeout(timeoutId)
    }
  },

  // Carregamento de conversas
  loadConversations: async (status?: ChatStatus, page = 0) => {
    const state = get()
    const targetStatus = status || state.currentStatus
    
    try {
      set({ isLoading: true, error: null })
      
      const response = await conversationApi.getByStatus(
        conversationAdapter.mapStatusToBackend(targetStatus) as ConversationStatus,
        page,
        20
      )
      
      
      const newChats = conversationAdapter.toChatArray(response?.content || [])
      
      set({
        chats: page === 0 ? newChats : [...state.chats, ...newChats],
        currentPage: page,
        hasMore: !response.last,
        totalChats: response.totalElements,
        isLoading: false
      })
      
    } catch (error) {
      console.error('Erro ao carregar conversas:', error)
      
      // Dispatch event para toast notification
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('chat:error', {
          detail: {
            title: 'Erro ao carregar conversas',
            description: error instanceof Error ? error.message : 'Erro desconhecido'
          }
        }))
      }
      
      set({ 
        error: 'Erro ao carregar conversas', 
        isLoading: false 
      })
    }
  },

  // Carregamento de mensagens
  loadMessages: async (chatId: string, page = 0) => {
    const state = get()
    const cached = state.messagesCache[chatId]
    
    try {
      set({ isLoadingMessages: true, error: null })
      
      const response = await messageApi.getByConversation(chatId, page, 50)
      const newMessages = messageAdapter.toMessageArray(response.content)
      
      // Mescla com mensagens em cache (incluindo temporárias)
      const existingMessages = cached?.messages || []
      const mergedMessages = messageAdapter.mergeMessages(existingMessages, newMessages)
      
      // Atualiza cache
      set({
        messagesCache: {
          ...state.messagesCache,
          [chatId]: {
            messages: mergedMessages,
            page,
            hasMore: !response.last
          }
        },
        isLoadingMessages: false
      })
      
      // Atualiza chat ativo se for o mesmo
      if (state.activeChat?.id === chatId) {
        set({
          activeChat: {
            ...state.activeChat,
            messages: mergedMessages
          }
        })
      }
      
    } catch (error) {
      console.error('Erro ao carregar mensagens:', error)
      set({ 
        error: 'Erro ao carregar mensagens', 
        isLoadingMessages: false 
      })
    }
  },

  // Refresh das conversas
  refreshConversations: async () => {
    const state = get()
    set({ currentPage: 0, hasMore: true, chats: [] })
    await get().loadConversations(state.currentStatus, 0)
  },

  // Envio de mensagens com optimistic updates
  sendMessage: async (chatId: string, content: string) => {
    const validation = MessageValidator.validate(content)
    if (!validation.valid) {
      set({ error: validation.error })
      return
    }
    
    const state = get()
    const chat = state.chats.find(c => c.id === chatId)
    if (!chat) return
    
    try {
      set({ isSending: true, error: null })
      
      // Optimistic update - criar mensagem temporária
      const tempMessage = messageAdapter.createTempMessage(content)
      
      // Adiciona mensagem temporária ao cache e chat ativo
      const cached = state.messagesCache[chatId]
      const updatedMessages = [...(cached?.messages || []), tempMessage]
      
      set({
        messagesCache: {
          ...state.messagesCache,
          [chatId]: {
            ...cached,
            messages: updatedMessages
          }
        }
      })
      
      // Atualiza chat ativo
      if (state.activeChat?.id === chatId) {
        set({
          activeChat: {
            ...state.activeChat,
            messages: updatedMessages,
            lastMessage: tempMessage,
            updatedAt: new Date()
          }
        })
      }
      
      // Atualiza lista de chats
      const updatedChats = state.chats.map(c => 
        c.id === chatId 
          ? { ...c, lastMessage: tempMessage, updatedAt: new Date() }
          : c
      )
      set({ chats: updatedChats })
      
      // Envia mensagem para API
      const sentMessage = await messageApi.send(
        chatId, 
        messageAdapter.toCreateRequest(content)
      )
      
      const realMessage = messageAdapter.toMessage(sentMessage)
      
      // Substitui mensagem temporária pela real
      const finalMessages = messageAdapter.replaceTempMessage(
        updatedMessages, 
        tempMessage.id, 
        realMessage
      )
      
      // Atualiza cache final
      set({
        messagesCache: {
          ...state.messagesCache,
          [chatId]: {
            ...cached,
            messages: finalMessages
          }
        },
        isSending: false
      })
      
      // Atualiza chat ativo final
      if (state.activeChat?.id === chatId) {
        set({
          activeChat: {
            ...state.activeChat,
            messages: finalMessages,
            lastMessage: realMessage
          }
        })
      }
      
    } catch (error) {
      console.error('Erro ao enviar mensagem:', error)
      
      // Remove mensagem temporária em caso de erro
      const state = get()
      const cached = state.messagesCache[chatId]
      const tempMessage = cached?.messages.find(m => messageAdapter.isTempMessage(m))
      
      if (tempMessage) {
        const cleanedMessages = messageAdapter.removeTempMessage(
          cached.messages, 
          tempMessage.id
        )
        
        set({
          messagesCache: {
            ...state.messagesCache,
            [chatId]: {
              ...cached,
              messages: cleanedMessages
            }
          },
          error: 'Erro ao enviar mensagem',
          isSending: false
        })
      }
    }
  },

  // Marcar como lida
  markAsRead: async (chatId: string) => {
    const state = get()
    const cached = state.messagesCache[chatId]
    
    if (!cached) return
    
    try {
      // Marca mensagens como lidas no cache local
      const readMessages = messageAdapter.markMessagesAsRead(cached.messages)
      
      set({
        messagesCache: {
          ...state.messagesCache,
          [chatId]: {
            ...cached,
            messages: readMessages
          }
        }
      })
      
      // Atualiza chat na lista
      const updatedChats = state.chats.map(chat => 
        chat.id === chatId 
          ? { ...chat, unreadCount: 0 }
          : chat
      )
      set({ chats: updatedChats })
      
      // Atualiza chat ativo
      if (state.activeChat?.id === chatId) {
        set({
          activeChat: {
            ...state.activeChat,
            messages: readMessages,
            unreadCount: 0
          }
        })
      }
      
      // Marca como lida no servidor
      await messageApi.markConversationAsRead(chatId)
      
    } catch (error) {
      console.error('Erro ao marcar como lida:', error)
    }
  },

  // Atribuir agente
  assignToAgent: async (chatId: string, agentId: string) => {
    try {
      await conversationApi.assignToUser(chatId, agentId)
      
      // Atualiza chat local
      const state = get()
      const updatedChats = state.chats.map(chat => 
        chat.id === chatId 
          ? { ...chat, assignedAgent: 'Agente', status: 'esperando' as ChatStatus }
          : chat
      )
      set({ chats: updatedChats })
      
    } catch (error) {
      console.error('Erro ao atribuir agente:', error)
      set({ error: 'Erro ao atribuir agente' })
    }
  },

  // Mudar status da conversa
  changeStatus: async (chatId: string, status: ChatStatus) => {
    try {
      await conversationApi.changeStatus(
        chatId, 
        conversationAdapter.mapStatusToBackend(status) as ConversationStatus
      )
      
      // Remove chat da lista atual (será carregado na aba correta)
      const state = get()
      const updatedChats = state.chats.filter(chat => chat.id !== chatId)
      set({ chats: updatedChats })
      
      // Limpa chat ativo se for o mesmo
      if (state.activeChat?.id === chatId) {
        set({ activeChat: null })
      }
      
    } catch (error) {
      console.error('Erro ao mudar status:', error)
      set({ error: 'Erro ao mudar status da conversa' })
    }
  },

  // Fixar conversa
  pinConversation: async (chatId: string) => {
    const state = get()
    const chat = state.chats.find(c => c.id === chatId)
    if (!chat) return
    
    try {
      if (chat.isPinned) {
        await conversationApi.unpin(chatId)
      } else {
        await conversationApi.pin(chatId)
      }
      
      // Atualiza chat local
      const updatedChats = state.chats.map(c => 
        c.id === chatId 
          ? { ...c, isPinned: !c.isPinned }
          : c
      )
      set({ chats: updatedChats })
      
    } catch (error) {
      console.error('Erro ao fixar conversa:', error)
      set({ error: 'Erro ao fixar conversa' })
    }
  },

  // Bloquear cliente
  blockCustomer: async (chatId: string) => {
    const state = get()
    const chat = state.chats.find(c => c.id === chatId)
    if (!chat) return
    
    try {
      await customerApi.block(chat.contact.id)
      await get().changeStatus(chatId, 'finalizados')
      
    } catch (error) {
      console.error('Erro ao bloquear cliente:', error)
      set({ error: 'Erro ao bloquear cliente' })
    }
  },

  // Adicionar tag (placeholder - será implementado quando tags estiverem no backend)
  addTag: async (chatId: string, tag: Tag) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId && !chat.tags.find(t => t.id === tag.id)) {
        return {
          ...chat,
          tags: [...chat.tags, tag]
        }
      }
      return chat
    })
    
    set({ chats: updatedChats })
  },

  // Remover tag (placeholder)
  removeTag: async (chatId: string, tagId: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        return {
          ...chat,
          tags: chat.tags.filter(tag => tag.id !== tagId)
        }
      }
      return chat
    })
    
    set({ chats: updatedChats })
  },

  // Busca de conversas
  searchConversations: async (query: string) => {
    if (!query.trim()) {
      await get().loadConversations(get().currentStatus, 0)
      return
    }
    
    try {
      set({ isLoading: true, error: null })
      
      const response = await conversationApi.search(query, {
        status: conversationAdapter.mapStatusToBackend(get().currentStatus) as ConversationStatus
      })
      
      const searchResults = conversationAdapter.toChatArray(response.content)
      
      set({
        chats: searchResults,
        currentPage: 0,
        hasMore: !response.last,
        totalChats: response.totalElements,
        isLoading: false
      })
      
    } catch (error) {
      console.error('Erro na busca:', error)
      set({ error: 'Erro na busca', isLoading: false })
    }
  },

  // Busca de mensagens (placeholder)
  searchMessages: async (query: string) => {
    try {
      const response = await messageApi.search(query)
      console.log('Resultados da busca de mensagens:', response)
      // TODO: implementar exibição dos resultados
      
    } catch (error) {
      console.error('Erro na busca de mensagens:', error)
      set({ error: 'Erro na busca de mensagens' })
    }
  },

  // Filtrar chats
  getFilteredChats: () => {
    const state = get()
    const { chats, currentStatus, searchQuery } = state
    
    // Adicionar verificação de segurança
    if (!Array.isArray(chats)) {
      console.warn('chats is not an array:', chats)
      return []
    }
    
    let filtered = chats.filter(chat => chat.status === currentStatus)
    
    if (searchQuery.trim()) {
      filtered = filtered.filter(chat => 
        chat.contact?.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        chat.contact?.phone?.includes(searchQuery) ||
        (chat.lastMessage?.content && chat.lastMessage.content.toLowerCase().includes(searchQuery.toLowerCase()))
      )
    }
    
    return filtered.sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    })
  },

  // WebSocket/Tempo real
  addMessage: (conversationId: string, message: Message) => {
    const state = get()
    const cached = state.messagesCache[conversationId]
    
    if (cached) {
      const updatedMessages = [...cached.messages, message]
      
      set({
        messagesCache: {
          ...state.messagesCache,
          [conversationId]: {
            ...cached,
            messages: updatedMessages
          }
        }
      })
      
      // Atualiza chat ativo se for o mesmo
      if (state.activeChat?.id === conversationId) {
        set({
          activeChat: {
            ...state.activeChat,
            messages: updatedMessages
          }
        })
      }
    }
  },

  updateMessageStatus: (messageId: string, status: string) => {
    const state = get()
    const updatedCache = { ...state.messagesCache }
    
    Object.keys(updatedCache).forEach(conversationId => {
      const cache = updatedCache[conversationId]
      const messageIndex = cache.messages.findIndex(m => m.id === messageId)
      
      if (messageIndex !== -1) {
        cache.messages[messageIndex] = {
          ...cache.messages[messageIndex],
          status: status as Message['status']
        }
        
        // Atualiza chat ativo se necessário
        if (state.activeChat?.id === conversationId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: [...cache.messages]
            }
          })
        }
      }
    })
    
    set({ messagesCache: updatedCache })
  },

  updateConversationLastMessage: (conversationId: string, message: Message) => {
    const state = get()
    const updatedChats = state.chats.map(chat => 
      chat.id === conversationId 
        ? { ...chat, lastMessage: message, updatedAt: new Date() }
        : chat
    )
    
    set({ chats: updatedChats })
    
    if (state.activeChat?.id === conversationId) {
      set({
        activeChat: {
          ...state.activeChat,
          lastMessage: message,
          updatedAt: new Date()
        }
      })
    }
  },

  updateConversationAgent: (conversationId: string, _agentId: string, agentName: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => 
      chat.id === conversationId 
        ? { ...chat, assignedAgent: agentName }
        : chat
    )
    
    set({ chats: updatedChats })
    
    if (state.activeChat?.id === conversationId) {
      set({
        activeChat: {
          ...state.activeChat,
          assignedAgent: agentName
        }
      })
    }
  },

  updateConversationStatus: (conversationId: string, status: ChatStatus) => {
    const state = get()
    const updatedChats = state.chats.map(chat => 
      chat.id === conversationId 
        ? { ...chat, status }
        : chat
    )
    
    set({ chats: updatedChats })
    
    if (state.activeChat?.id === conversationId) {
      set({
        activeChat: {
          ...state.activeChat,
          status
        }
      })
    }
  },

  moveConversationToStatus: (conversationId: string, status: ChatStatus) => {
    const state = get()
    
    // Se a conversa não está no status atual, remove da lista
    if (state.currentStatus !== status) {
      const updatedChats = state.chats.filter(chat => chat.id !== conversationId)
      set({ chats: updatedChats })
      
      // Limpa chat ativo se for o mesmo
      if (state.activeChat?.id === conversationId) {
        set({ activeChat: null })
      }
    }
  },

  updateConversation: (conversationId: string, updates: Record<string, unknown>) => {
    const state = get()
    const updatedChats = state.chats.map(chat => 
      chat.id === conversationId 
        ? { ...chat, ...updates }
        : chat
    )
    
    set({ chats: updatedChats })
    
    if (state.activeChat?.id === conversationId) {
      set({
        activeChat: {
          ...state.activeChat,
          ...updates
        }
      })
    }
  },

  setUserTyping: (conversationId: string, userId: string, userName: string, isTyping: boolean) => {
    const state = get()
    const currentTyping = state.typingUsers[conversationId] || []
    
    if (isTyping) {
      // Adiciona usuário digitando
      const existingIndex = currentTyping.findIndex(u => u.userId === userId)
      const updatedTyping = existingIndex !== -1 
        ? currentTyping.map((u, i) => i === existingIndex ? { ...u, timestamp: Date.now() } : u)
        : [...currentTyping, { userId, userName, timestamp: Date.now() }]
      
      set({
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: updatedTyping
        }
      })
    } else {
      // Remove usuário digitando
      const updatedTyping = currentTyping.filter(u => u.userId !== userId)
      
      set({
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: updatedTyping
        }
      })
    }
  },

  setUserOnlineStatus: (userId: string, isOnline: boolean) => {
    const state = get()
    const updatedOnlineUsers = new Set(state.onlineUsers)
    
    if (isOnline) {
      updatedOnlineUsers.add(userId)
    } else {
      updatedOnlineUsers.delete(userId)
    }
    
    set({ onlineUsers: updatedOnlineUsers })
  },

  setActiveConversation: (conversationId: string | null) => {
    set({ activeConversationId: conversationId })
    
    if (conversationId) {
      const state = get()
      const chat = state.chats.find(c => c.id === conversationId)
      if (chat) {
        get().setActiveChat(chat)
      }
    } else {
      get().setActiveChat(null)
    }
  },

  getTypingUsers: (conversationId: string) => {
    const state = get()
    const typingInConversation = state.typingUsers[conversationId] || []
    
    // Remove usuários que pararam de digitar há mais de 5 segundos
    const now = Date.now()
    const activeTyping = typingInConversation.filter(u => 
      now - u.timestamp < 5000
    )
    
    // Atualiza estado se houver mudanças
    if (activeTyping.length !== typingInConversation.length) {
      set({
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: activeTyping
        }
      })
    }
    
    return activeTyping.map(u => u.userName)
  },

  // Utilitários
  clearError: () => set({ error: null }),
  
  clearCache: () => set({ messagesCache: {}, typingUsers: {}, onlineUsers: new Set() }),

  // Métodos legados para compatibilidade (serão removidos)
  transferChat: async () => {
    // Implementar via assignToAgent
    console.warn('transferChat is deprecated, use assignToAgent instead')
  },

  blockContact: async (chatId: string) => {
    await get().blockCustomer(chatId)
  },

  finalizeChat: async (chatId: string) => {
    await get().changeStatus(chatId, 'finalizados')
  },

  pinChat: async (chatId: string) => {
    await get().pinConversation(chatId)
  }
}))