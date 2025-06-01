import { create } from 'zustand'
import type { Chat, ChatStore, ChatStatus, Message, Tag } from '../types'

export const useChatStore = create<ChatStore>((set, get) => ({
  chats: [],
  activeChat: null,
  currentStatus: 'entrada',
  searchQuery: '',
  isLoading: false,

  setActiveChat: (chat: Chat | null) => {
    set({ activeChat: chat })
    if (chat && chat.unreadCount > 0) {
      get().markAsRead(chat.id)
    }
  },

  setCurrentStatus: (status: ChatStatus) => {
    set({ currentStatus: status, activeChat: null })
  },

  setSearchQuery: (query: string) => {
    set({ searchQuery: query })
  },

  sendMessage: (chatId: string, content: string) => {
    const state = get()
    const chat = state.chats.find(c => c.id === chatId)
    if (!chat) return

    const newMessage: Message = {
      id: Date.now().toString(),
      content,
      timestamp: new Date(),
      senderId: 'agent',
      messageType: 'text',
      status: 'sent',
      isFromUser: true
    }

    const updatedChats = state.chats.map(c => {
      if (c.id === chatId) {
        return {
          ...c,
          messages: [...c.messages, newMessage],
          lastMessage: newMessage,
          updatedAt: new Date()
        }
      }
      return c
    })

    set({ 
      chats: updatedChats,
      activeChat: state.activeChat?.id === chatId 
        ? { ...state.activeChat, messages: [...state.activeChat.messages, newMessage], lastMessage: newMessage }
        : state.activeChat
    })
  },

  markAsRead: (chatId: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        const updatedMessages = chat.messages.map(msg => ({
          ...msg,
          status: msg.isFromUser ? 'read' as const : msg.status
        }))
        return {
          ...chat,
          messages: updatedMessages,
          unreadCount: 0
        }
      }
      return chat
    })

    set({ 
      chats: updatedChats,
      activeChat: state.activeChat?.id === chatId 
        ? { ...state.activeChat, unreadCount: 0 }
        : state.activeChat
    })
  },

  addTag: (chatId: string, tag: Tag) => {
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

    set({ 
      chats: updatedChats,
      activeChat: state.activeChat?.id === chatId 
        ? { ...state.activeChat, tags: [...(state.activeChat.tags || []), tag] }
        : state.activeChat
    })
  },

  removeTag: (chatId: string, tagId: string) => {
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

    set({ 
      chats: updatedChats,
      activeChat: state.activeChat?.id === chatId 
        ? { ...state.activeChat, tags: state.activeChat.tags.filter(tag => tag.id !== tagId) }
        : state.activeChat
    })
  },

  transferChat: (chatId: string, agentName: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        return {
          ...chat,
          assignedAgent: agentName,
          status: 'esperando' as ChatStatus
        }
      }
      return chat
    })

    set({ chats: updatedChats })
  },

  blockContact: (chatId: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        return {
          ...chat,
          status: 'finalizados' as ChatStatus
        }
      }
      return chat
    })

    set({ chats: updatedChats, activeChat: null })
  },

  finalizeChat: (chatId: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        return {
          ...chat,
          status: 'finalizados' as ChatStatus
        }
      }
      return chat
    })

    set({ chats: updatedChats })
  },

  pinChat: (chatId: string) => {
    const state = get()
    const updatedChats = state.chats.map(chat => {
      if (chat.id === chatId) {
        return {
          ...chat,
          isPinned: !chat.isPinned
        }
      }
      return chat
    })

    set({ chats: updatedChats })
  },

  getFilteredChats: () => {
    const state = get()
    const { chats, currentStatus, searchQuery } = state
    
    let filtered = chats.filter(chat => chat.status === currentStatus)
    
    if (searchQuery.trim()) {
      filtered = filtered.filter(chat => 
        chat.contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        chat.contact.phone?.includes(searchQuery) ||
        chat.lastMessage?.content.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }
    
    return filtered.sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    })
  }
}))