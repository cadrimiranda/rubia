import { create } from "zustand";
import type { Chat, ChatStatus, Message, Tag } from "../types";
import type { ConversationStatus } from "../api/types";
import { conversationApi, messageApi, customerApi } from "../api";
import { conversationAdapter, messageAdapter } from "../adapters";
import { MessageValidator } from "../utils/validation";
import {
  getAllCampaignConversations,
  getCampaignConversationMessages,
} from "../mocks/campaignMock";
// import { mockDonors } from '../mocks/data'

interface ChatStoreState {
  // Estado principal
  chats: Chat[];
  activeChat: Chat | null;
  currentStatus: ChatStatus;
  searchQuery: string;

  // Estados de carregamento
  isLoading: boolean;
  isLoadingMessages: boolean;
  isSending: boolean;

  // Paginação
  currentPage: number;
  hasMore: boolean;
  totalChats: number;

  // Erros
  error: string | null;

  // Cache de mensagens por conversa
  messagesCache: Record<
    string,
    {
      messages: Message[];
      page: number;
      hasMore: boolean;
    }
  >;

  // Estados de tempo real
  typingUsers: Record<
    string,
    {
      userId: string;
      userName: string;
      timestamp: number;
    }[]
  >;
  onlineUsers: Set<string>;
  activeConversationId: string | null;

  unreadCount: Record<string, number>;
}

interface ChatStoreActions {
  createUnreadCount: (conversationCounters: Record<string, number>) => void;
  updateUnreadCount: (conversationId: string, unreadCount: number) => void;
  updateUnreadCountBulk: (conversationCounters: Record<string, number>) => void;

  // Navegação
  setActiveChat: (chat: Chat | null) => Promise<void>;
  setCurrentStatus: (status: ChatStatus) => void;
  setSearchQuery: (query: string) => void;

  // Carregamento de dados
  loadConversations: (status?: ChatStatus, page?: number) => Promise<void>;
  loadMessages: (chatId: string, page?: number) => Promise<void>;
  refreshConversations: () => Promise<void>;

  // Envio de mensagens
  sendMessage: (chatId: string, content: string) => Promise<void>;

  // Ações de conversa
  markAsRead: (chatId: string) => Promise<void>;
  assignToAgent: (chatId: string, agentId: string) => Promise<void>;
  changeStatus: (chatId: string, status: ChatStatus) => Promise<void>;
  pinConversation: (chatId: string) => Promise<void>;
  blockCustomer: (chatId: string) => Promise<void>;

  // Tags
  addTag: (chatId: string, tag: Tag) => Promise<void>;
  removeTag: (chatId: string, tagId: string) => Promise<void>;

  // Busca
  searchConversations: (query: string) => Promise<void>;
  searchMessages: (query: string) => Promise<void>;

  // WebSocket/Tempo real
  addMessage: (conversationId: string, message: Message) => void;
  updateMessageStatus: (messageId: string, status: string) => void;
  updateConversationLastMessage: (
    conversationId: string,
    message: Message
  ) => void;
  updateConversationAgent: (
    conversationId: string,
    agentId: string,
    agentName: string
  ) => void;
  updateConversationStatus: (
    conversationId: string,
    status: ChatStatus
  ) => void;
  moveConversationToStatus: (
    conversationId: string,
    status: ChatStatus
  ) => void;
  updateConversation: (
    conversationId: string,
    updates: Record<string, unknown>
  ) => void;
  setUserTyping: (
    conversationId: string,
    userId: string,
    userName: string,
    isTyping: boolean
  ) => void;
  setUserOnlineStatus: (userId: string, isOnline: boolean) => void;
  setActiveConversation: (conversationId: string | null) => void;
  getTypingUsers: (conversationId: string) => string[];

  // Utilitários
  getFilteredChats: () => Chat[];
  clearError: () => void;
  clearCache: () => void;
}

export const useChatStore = create<ChatStoreState & ChatStoreActions>(
  (set, get) => ({
    // Estado inicial
    chats: [],
    activeChat: null,
    currentStatus: "ativos",
    searchQuery: "",
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
    unreadCount: {},

    createUnreadCount: (conversationCounters: Record<string, number>) => {
      set({
        unreadCount: conversationCounters,
      });
    },
    updateUnreadCount: (conversationId: string, counter: number) => {
      const unreadCount = get().unreadCount;
      unreadCount[conversationId] = counter;

      set({
        unreadCount,
      });
    },
    updateUnreadCountBulk: (conversationCounters: Record<string, number>) => {
      const unreadCount = get().unreadCount;

      set({
        unreadCount: {
          ...unreadCount,
          ...conversationCounters,
        },
      });
    },

    // Navegação
    setActiveChat: async (chat: Chat | null) => {
      set({ activeChat: chat });

      if (chat) {
        // Carregar mensagens da conversa
        await get().loadMessages(chat.id);

        // Marcar como lida se tiver mensagens não lidas
        if (chat.unreadCount > 0) {
          await get().markAsRead(chat.id);
        }
      }
    },

    setCurrentStatus: (status: ChatStatus) => {
      set({
        currentStatus: status,
        activeChat: null,
        currentPage: 0,
        hasMore: true,
        chats: [],
      });

      // Carrega conversas do novo status
      get().loadConversations(status);
    },

    setSearchQuery: (query: string) => {
      set({ searchQuery: query });

      // Se a query estiver vazia, recarrega conversas normais
      if (!query.trim()) {
        get().loadConversations(get().currentStatus);
      } else {
        // Faz busca com debounce
        const timeoutId = setTimeout(() => {
          get().searchConversations(query);
        }, 300);

        return () => clearTimeout(timeoutId);
      }
    },

    // Carregamento de conversas
    loadConversations: async (status?: ChatStatus, page = 0) => {
      const state = get();
      const targetStatus = status || state.currentStatus;

      set({ isLoading: true, error: null });

      // Tentar carregar conversas mock primeiro (campanhas)
      try {
        const mockConversations = getAllCampaignConversations();

        if (mockConversations.length > 0) {
          // Filtrar conversas por status
          const filteredConversations = mockConversations.filter((conv) => {
            if (targetStatus === "ativos") return conv.status === "ENTRADA";
            if (targetStatus === "aguardando")
              return conv.status === "ESPERANDO";
            if (targetStatus === "inativo")
              return conv.status === "FINALIZADOS";
            return true;
          });

          const mockChats = conversationAdapter.toChatArray(
            filteredConversations
          );

          set({
            chats: page === 0 ? mockChats : [...state.chats, ...mockChats],
            currentPage: page,
            hasMore: false,
            totalChats: mockChats.length,
            isLoading: false,
            error: null,
          });

          return;
        }
      } catch (mockError) {
        console.warn("Erro ao carregar conversas de campanha:", mockError);
      }

      // Tentar carregar da API se não há conversas mock
      try {
        const response = await conversationApi.getByStatus(
          conversationAdapter.mapStatusToBackend(
            targetStatus
          ) as ConversationStatus,
          page,
          20
        );

        const newChats = conversationAdapter.toChatArray(
          response?.content || []
        );

        set({
          chats: page === 0 ? newChats : [...state.chats, ...newChats],
          currentPage: page,
          hasMore: !response.last,
          totalChats: response.totalElements,
          isLoading: false,
        });
      } catch (error) {
        console.error("Erro ao carregar conversas da API:", error);

        // Sem conversas mock e API falhou
        set({
          chats: page === 0 ? [] : state.chats,
          currentPage: page,
          hasMore: false,
          totalChats: 0,
          isLoading: false,
          error: null,
        });
      }
    },

    // Carregamento de mensagens
    loadMessages: async (chatId: string, page = 0) => {
      const state = get();
      const cached = state.messagesCache[chatId];

      try {
        set({ isLoadingMessages: true, error: null });

        const response = await messageApi.getByConversation(chatId, page, 50);
        console.log("📨 Response da API de mensagens:", response);
        console.log("📨 Response.content:", response?.content);

        const newMessages = messageAdapter.toMessageArray(
          response?.content || response || []
        );

        // Mescla com mensagens em cache (incluindo temporárias)
        const existingMessages = cached?.messages || [];
        const mergedMessages = messageAdapter.mergeMessages(
          existingMessages,
          newMessages
        );

        // Atualiza cache
        set({
          messagesCache: {
            ...state.messagesCache,
            [chatId]: {
              messages: mergedMessages,
              page,
              hasMore: !response.last,
            },
          },
          isLoadingMessages: false,
        });

        // Atualiza chat ativo se for o mesmo
        if (state.activeChat?.id === chatId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: mergedMessages,
            },
          });
        }
      } catch (error) {
        console.error("Erro ao carregar mensagens:", error);

        // Fallback para mensagens de campanha mock
        try {
          const mockMessages = getCampaignConversationMessages(chatId);

          if (mockMessages.length > 0) {
            const convertedMessages =
              messageAdapter.toMessageArray(mockMessages);

            set({
              messagesCache: {
                ...state.messagesCache,
                [chatId]: {
                  messages: convertedMessages,
                  page: 0,
                  hasMore: false,
                },
              },
              isLoadingMessages: false,
            });

            // Atualizar chat ativo se for o mesmo
            if (state.activeChat?.id === chatId) {
              set({
                activeChat: {
                  ...state.activeChat,
                  messages: convertedMessages,
                },
              });
            }
          } else {
            set({
              error: "Nenhuma mensagem encontrada",
              isLoadingMessages: false,
            });
          }
        } catch (mockError) {
          console.error("Erro ao carregar mensagens mock:", mockError);
          set({
            error: "Erro ao carregar mensagens",
            isLoadingMessages: false,
          });
        }
      }
    },

    // Refresh das conversas
    refreshConversations: async () => {
      const state = get();
      set({ currentPage: 0, hasMore: true, chats: [] });
      await get().loadConversations(state.currentStatus, 0);
    },

    // Envio de mensagens com optimistic updates
    sendMessage: async (chatId: string, content: string) => {
      const validation = MessageValidator.validate(content);
      if (!validation.valid) {
        set({ error: validation.error });
        return;
      }

      const state = get();
      const chat = state.chats.find((c) => c.id === chatId);
      if (!chat) return;

      try {
        set({ isSending: true, error: null });

        // Optimistic update - criar mensagem temporária
        const tempMessage = messageAdapter.createTempMessage(content);

        // Adiciona mensagem temporária ao cache e chat ativo
        const cached = state.messagesCache[chatId];
        const updatedMessages = [...(cached?.messages || []), tempMessage];

        set({
          messagesCache: {
            ...state.messagesCache,
            [chatId]: {
              ...cached,
              messages: updatedMessages,
            },
          },
        });

        // Atualiza chat ativo
        if (state.activeChat?.id === chatId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: updatedMessages,
              lastMessage: tempMessage,
              updatedAt: new Date(),
            },
          });
        }

        // Atualiza lista de chats
        const updatedChats = state.chats.map((c) =>
          c.id === chatId
            ? { ...c, lastMessage: tempMessage, updatedAt: new Date() }
            : c
        );
        set({ chats: updatedChats });

        // Envia mensagem via Z-API diretamente
        if (!chat.contact?.phone) {
          throw new Error("Telefone do contato não encontrado");
        }

        const result = await messageApi.sendText(chat.contact.phone, content);

        if (!result.success) {
          throw new Error(result.error || "Falha ao enviar mensagem");
        }

        // A mensagem será recebida via webhook, então não precisamos processar aqui
        // Apenas removemos o loading e mantemos a mensagem temporária até o webhook chegar
        set({ isSending: false });
      } catch (error) {
        console.error("Erro ao enviar mensagem:", error);

        // Remove mensagem temporária em caso de erro
        const state = get();
        const cached = state.messagesCache[chatId];
        const tempMessage = cached?.messages.find((m) =>
          messageAdapter.isTempMessage(m)
        );

        if (tempMessage) {
          const cleanedMessages = messageAdapter.removeTempMessage(
            cached.messages,
            tempMessage.id
          );

          set({
            messagesCache: {
              ...state.messagesCache,
              [chatId]: {
                ...cached,
                messages: cleanedMessages,
              },
            },
            error: "Erro ao enviar mensagem",
            isSending: false,
          });
        }
      }
    },

    // Marcar como lida
    markAsRead: async (chatId: string) => {
      const state = get();
      const cached = state.messagesCache[chatId];

      if (!cached) return;

      try {
        // Marca mensagens como lidas no cache local
        const readMessages = messageAdapter.markMessagesAsRead(cached.messages);

        set({
          messagesCache: {
            ...state.messagesCache,
            [chatId]: {
              ...cached,
              messages: readMessages,
            },
          },
        });

        // Atualiza chat na lista
        const updatedChats = state.chats.map((chat) =>
          chat.id === chatId ? { ...chat, unreadCount: 0 } : chat
        );
        set({ chats: updatedChats });

        // Atualiza chat ativo
        if (state.activeChat?.id === chatId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: readMessages,
              unreadCount: 0,
            },
          });
        }

        // Marca como lida no servidor
        await messageApi.markConversationAsRead(chatId);
      } catch (error) {
        console.error("Erro ao marcar como lida:", error);
      }
    },

    // Atribuir agente
    assignToAgent: async (chatId: string, agentId: string) => {
      try {
        await conversationApi.assignToUser(chatId, agentId);

        // Atualiza chat local
        const state = get();
        const updatedChats = state.chats.map((chat) =>
          chat.id === chatId
            ? {
                ...chat,
                assignedAgent: "Agente",
                status: "esperando" as ChatStatus,
              }
            : chat
        );
        set({ chats: updatedChats });
      } catch (error) {
        console.error("Erro ao atribuir agente:", error);
        set({ error: "Erro ao atribuir agente" });
      }
    },

    // Mudar status da conversa
    changeStatus: async (chatId: string, status: ChatStatus) => {
      try {
        await conversationApi.changeStatus(
          chatId,
          conversationAdapter.mapStatusToBackend(status) as ConversationStatus
        );

        // Remove chat da lista atual (será carregado na aba correta)
        const state = get();
        const updatedChats = state.chats.filter((chat) => chat.id !== chatId);
        set({ chats: updatedChats });

        // Limpa chat ativo se for o mesmo
        if (state.activeChat?.id === chatId) {
          set({ activeChat: null });
        }
      } catch (error) {
        console.error("Erro ao mudar status:", error);
        set({ error: "Erro ao mudar status da conversa" });
      }
    },

    // Fixar conversa
    pinConversation: async (chatId: string) => {
      const state = get();
      const chat = state.chats.find((c) => c.id === chatId);
      if (!chat) return;

      try {
        if (chat.isPinned) {
          await (conversationApi as any).unpin(chatId);
        } else {
          await (conversationApi as any).pin(chatId);
        }

        // Atualiza chat local
        const updatedChats = state.chats.map((c) =>
          c.id === chatId ? { ...c, isPinned: !c.isPinned } : c
        );
        set({ chats: updatedChats });
      } catch (error) {
        console.error("Erro ao fixar conversa:", error);
        set({ error: "Erro ao fixar conversa" });
      }
    },

    // Bloquear cliente
    blockCustomer: async (chatId: string) => {
      const state = get();
      const chat = state.chats.find((c) => c.id === chatId);
      if (!chat) return;

      try {
        await customerApi.block(chat.contact.id);
        await get().changeStatus(chatId, "inativo");
      } catch (error) {
        console.error("Erro ao bloquear cliente:", error);
        set({ error: "Erro ao bloquear cliente" });
      }
    },

    // Adicionar tag (placeholder - será implementado quando tags estiverem no backend)
    addTag: async (chatId: string, tag: Tag) => {
      const state = get();
      const updatedChats = state.chats.map((chat) => {
        if (chat.id === chatId && !chat.tags.find((t) => t.id === tag.id)) {
          return {
            ...chat,
            tags: [...chat.tags, tag],
          };
        }
        return chat;
      });

      set({ chats: updatedChats });
    },

    // Remover tag (placeholder)
    removeTag: async (chatId: string, tagId: string) => {
      const state = get();
      const updatedChats = state.chats.map((chat) => {
        if (chat.id === chatId) {
          return {
            ...chat,
            tags: chat.tags.filter((tag) => tag.id !== tagId),
          };
        }
        return chat;
      });

      set({ chats: updatedChats });
    },

    // Busca de conversas
    searchConversations: async (query: string) => {
      if (!query.trim()) {
        await get().loadConversations(get().currentStatus, 0);
        return;
      }

      try {
        set({ isLoading: true, error: null });

        const response = await conversationApi.search(query, {
          status: conversationAdapter.mapStatusToBackend(
            get().currentStatus
          ) as ConversationStatus,
        });

        const searchResults = conversationAdapter.toChatArray(response.content);

        set({
          chats: searchResults,
          currentPage: 0,
          hasMore: !response.last,
          totalChats: response.totalElements,
          isLoading: false,
        });
      } catch (error) {
        console.error("Erro na busca:", error);
        set({ error: "Erro na busca", isLoading: false });
      }
    },

    // Busca de mensagens (placeholder)
    searchMessages: async (query: string) => {
      try {
        const response = await messageApi.search(query);
        console.log("Search results:", response);
        // TODO: implementar exibição dos resultados
      } catch (error) {
        console.error("Erro na busca de mensagens:", error);
        set({ error: "Erro na busca de mensagens" });
      }
    },

    // Filtrar chats
    getFilteredChats: () => {
      const state = get();
      const { chats, currentStatus, searchQuery } = state;

      // Adicionar verificação de segurança
      if (!Array.isArray(chats)) {
        console.warn("chats is not an array:", chats);
        return [];
      }

      let filtered = chats.filter((chat) => chat.status === currentStatus);

      if (searchQuery.trim()) {
        filtered = filtered.filter(
          (chat) =>
            chat.contact?.name
              ?.toLowerCase()
              .includes(searchQuery.toLowerCase()) ||
            chat.contact?.phone?.includes(searchQuery) ||
            (chat.lastMessage?.content &&
              chat.lastMessage.content
                .toLowerCase()
                .includes(searchQuery.toLowerCase()))
        );
      }

      return filtered.sort((a, b) => {
        if (a.isPinned && !b.isPinned) return -1;
        if (!a.isPinned && b.isPinned) return 1;
        return (
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        );
      });
    },

    // WebSocket/Tempo real
    addMessage: (conversationId: string, message: Message) => {
      const state = get();
      const cached = state.messagesCache[conversationId];

      if (cached) {
        // Adicionar mensagem e ordenar por timestamp para manter ordem cronológica
        const allMessages = [...cached.messages, message];
        const sortedMessages = allMessages.sort((a, b) => {
          // Converter timestamp HH:MM para minutos para comparação numérica
          const parseTime = (timeStr: string) => {
            if (!timeStr) return 0;
            const [hours, minutes] = timeStr.split(":").map(Number);
            return hours * 60 + minutes;
          };

          const timeA = parseTime(String(a.timestamp || "00:00"));
          const timeB = parseTime(String(b.timestamp || "00:00"));

          return timeA - timeB;
        });

        set({
          messagesCache: {
            ...state.messagesCache,
            [conversationId]: {
              ...cached,
              messages: sortedMessages,
            },
          },
        });

        // Atualiza chat ativo se for o mesmo
        if (state.activeChat?.id === conversationId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: sortedMessages,
            },
          });
        }
      } else {
        // Se não há cache, cria um novo com a mensagem
        set({
          messagesCache: {
            ...state.messagesCache,
            [conversationId]: {
              messages: [message],
              page: 1,
              hasMore: false,
            },
          },
        });

        // Se é o chat ativo, também atualiza
        if (state.activeChat?.id === conversationId) {
          set({
            activeChat: {
              ...state.activeChat,
              messages: [message],
            },
          });
        }
      }

      // Trigger conversation reordering by updating the last message
      get().updateConversationLastMessage(conversationId, message);
    },

    updateMessageStatus: (messageId: string, status: string) => {
      const state = get();
      const updatedCache = { ...state.messagesCache };

      Object.keys(updatedCache).forEach((conversationId) => {
        const cache = updatedCache[conversationId];
        const messageIndex = cache.messages.findIndex(
          (m) => m.id === messageId
        );

        if (messageIndex !== -1) {
          cache.messages[messageIndex] = {
            ...cache.messages[messageIndex],
            status: status as Message["status"],
          };

          // Atualiza chat ativo se necessário
          if (state.activeChat?.id === conversationId) {
            set({
              activeChat: {
                ...state.activeChat,
                messages: [...cache.messages],
              },
            });
          }
        }
      });

      set({ messagesCache: updatedCache });
    },

    updateConversationLastMessage: (
      conversationId: string,
      message: Message
    ) => {
      const state = get();
      const targetChat = state.chats.find((chat) => chat.id === conversationId);

      if (targetChat) {
        // Remove the conversation from its current position
        const otherChats = state.chats.filter(
          (chat) => chat.id !== conversationId
        );

        // Update the conversation with new message and timestamp
        const updatedChat = {
          ...targetChat,
          lastMessage: message,
          updatedAt: new Date(),
          unreadCount:
            state.activeChat?.id === conversationId
              ? targetChat.unreadCount
              : (targetChat.unreadCount || 0) + 1,
        };

        // Place the updated conversation at the beginning of the array
        const reorderedChats = [updatedChat, ...otherChats];

        set({ chats: reorderedChats });

        if (state.activeChat?.id === conversationId) {
          set({
            activeChat: {
              ...state.activeChat,
              lastMessage: message,
              updatedAt: new Date(),
            },
          });
        }
      }
    },

    updateConversationAgent: (
      conversationId: string,
      _agentId: string,
      agentName: string
    ) => {
      const state = get();
      const updatedChats = state.chats.map((chat) =>
        chat.id === conversationId
          ? { ...chat, assignedAgent: agentName }
          : chat
      );

      set({ chats: updatedChats });

      if (state.activeChat?.id === conversationId) {
        set({
          activeChat: {
            ...state.activeChat,
            assignedAgent: agentName,
          },
        });
      }
    },

    updateConversationStatus: (conversationId: string, status: ChatStatus) => {
      const state = get();
      const updatedChats = state.chats.map((chat) =>
        chat.id === conversationId ? { ...chat, status } : chat
      );

      set({ chats: updatedChats });

      if (state.activeChat?.id === conversationId) {
        set({
          activeChat: {
            ...state.activeChat,
            status,
          },
        });
      }
    },

    moveConversationToStatus: (conversationId: string, status: ChatStatus) => {
      const state = get();

      // Se a conversa não está no status atual, remove da lista
      if (state.currentStatus !== status) {
        const updatedChats = state.chats.filter(
          (chat) => chat.id !== conversationId
        );
        set({ chats: updatedChats });

        // Limpa chat ativo se for o mesmo
        if (state.activeChat?.id === conversationId) {
          set({ activeChat: null });
        }
      }
    },

    updateConversation: (
      conversationId: string,
      updates: Record<string, unknown>
    ) => {
      const state = get();
      const updatedChats = state.chats.map((chat) =>
        chat.id === conversationId ? { ...chat, ...updates } : chat
      );

      set({ chats: updatedChats });

      if (state.activeChat?.id === conversationId) {
        set({
          activeChat: {
            ...state.activeChat,
            ...updates,
          },
        });
      }
    },

    setUserTyping: (
      conversationId: string,
      userId: string,
      userName: string,
      isTyping: boolean
    ) => {
      const state = get();
      const currentTyping = state.typingUsers[conversationId] || [];

      if (isTyping) {
        // Adiciona usuário digitando
        const existingIndex = currentTyping.findIndex(
          (u) => u.userId === userId
        );
        const updatedTyping =
          existingIndex !== -1
            ? currentTyping.map((u, i) =>
                i === existingIndex ? { ...u, timestamp: Date.now() } : u
              )
            : [...currentTyping, { userId, userName, timestamp: Date.now() }];

        set({
          typingUsers: {
            ...state.typingUsers,
            [conversationId]: updatedTyping,
          },
        });
      } else {
        // Remove usuário digitando
        const updatedTyping = currentTyping.filter((u) => u.userId !== userId);

        set({
          typingUsers: {
            ...state.typingUsers,
            [conversationId]: updatedTyping,
          },
        });
      }
    },

    setUserOnlineStatus: (userId: string, isOnline: boolean) => {
      const state = get();
      const updatedOnlineUsers = new Set(state.onlineUsers);

      if (isOnline) {
        updatedOnlineUsers.add(userId);
      } else {
        updatedOnlineUsers.delete(userId);
      }

      set({ onlineUsers: updatedOnlineUsers });
    },

    setActiveConversation: (conversationId: string | null) => {
      set({ activeConversationId: conversationId });

      if (conversationId) {
        const state = get();
        const chat = state.chats.find((c) => c.id === conversationId);
        if (chat) {
          get().setActiveChat(chat);
        }
      } else {
        get().setActiveChat(null);
      }
    },

    getTypingUsers: (conversationId: string) => {
      const state = get();
      const typingInConversation = state.typingUsers[conversationId] || [];

      // Remove usuários que pararam de digitar há mais de 5 segundos
      const now = Date.now();
      const activeTyping = typingInConversation.filter(
        (u) => now - u.timestamp < 5000
      );

      // Atualiza estado se houver mudanças
      if (activeTyping.length !== typingInConversation.length) {
        set({
          typingUsers: {
            ...state.typingUsers,
            [conversationId]: activeTyping,
          },
        });
      }

      return activeTyping.map((u) => u.userName);
    },

    // Utilitários
    clearError: () => set({ error: null }),

    clearCache: () =>
      set({ messagesCache: {}, typingUsers: {}, onlineUsers: new Set() }),

    // Métodos legados para compatibilidade (serão removidos)
    transferChat: async () => {
      // Implementar via assignToAgent
      console.warn("transferChat is deprecated, use assignToAgent instead");
    },

    blockContact: async (chatId: string) => {
      await get().blockCustomer(chatId);
    },

    finalizeChat: async (chatId: string) => {
      await get().changeStatus(chatId, "finalizados");
    },

    pinChat: async (chatId: string) => {
      await get().pinConversation(chatId);
    },
  })
);
