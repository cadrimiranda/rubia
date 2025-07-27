import React, { useState, useEffect, useRef } from "react";
import { Heart } from "lucide-react";
import type { Donor, Message, FileAttachment, ChatState, ViewMode, PendingMedia, ConversationMedia } from "../types/types";
import { getCurrentTimestamp } from "../utils";
import { DonorSidebar } from "./DonorSidebar";
import { ChatHeader } from "./ChatHeader";
import { MessageList } from "./MessageList";
import { MessageInput } from "./MessageInput";
import { ContextMenu as ContextMenuComponent } from "./ContextMenu";
import { NewChatModal } from "./NewChatModal";
import { DonorInfoModal } from "./DonorInfoModal";
import { ConfigurationPage } from "./ConfigurationPage";
import { ScheduleModal } from "./ScheduleModal";
import { ConfirmationModal } from "./ConfirmationModal";
import { MessageEnhancerModal } from "./MessageEnhancerModal";
import { conversationApi } from "../api/services/conversationApi";
import { customerApi } from "../api/services/customerApi";
import { messageApi } from "../api/services/messageApi";
import { mediaApi } from "../api/services/mediaApi";
import { customerAdapter } from "../adapters/customerAdapter";
import { conversationAdapter } from "../adapters/conversationAdapter";
import { getMessagesForDonor } from "../mocks/data";
import { campaignApi } from "../api/services/campaignApi";
import type { ChatStatus } from "../types/index";
import type { Campaign } from "../types/types";
import type { ConversationStatus } from "../api/types";
import { useWebSocket } from "../hooks/useWebSocket";
import { authService } from "../auth/authService";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";
import { WhatsAppConnectionMonitor } from "./WhatsAppConnectionMonitor";

interface NewContactData {
  name: string;
  phone: string;
  donor?: Donor;
}

export const BloodCenterChat: React.FC = () => {

  const [state, setState] = useState<ChatState>({
    selectedDonor: null,
    selectedCampaign: null,
    messages: [],
    attachments: [],
    media: [],
    pendingMedia: [],
    searchTerm: "",
    messageInput: "",
    showNewChatModal: false,
    showDonorInfo: false,
    newChatSearch: "",
    isDragging: false,
    contextMenu: { show: false, x: 0, y: 0, donorId: "" },
    showConfiguration: false,
    showScheduleModal: false,
    scheduleTarget: null,
    showConfirmationModal: false,
    confirmationData: null,
    viewMode: 'full',
  });

  const [showMessageEnhancer, setShowMessageEnhancer] = useState(false);

  const [donors, setDonors] = useState<Donor[]>([]); // Contatos com conversas ativas
  const [allContacts, setAllContacts] = useState<Donor[]>([]); // TODOS os contatos
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingContacts, setIsLoadingContacts] = useState(false);
  const [isCreatingConversation, setIsCreatingConversation] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentStatus, setCurrentStatus] = useState<ChatStatus>('ativos');
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [, setIsCampaignsLoading] = useState(true);
  const [selectedCampaign, setSelectedCampaign] = useState<Campaign | null>(null);
  const [currentDraftMessage, setCurrentDraftMessage] = useState<any>(null);
  
  // Estados para paginação infinita
  const [hasMorePages, setHasMorePages] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  
  // Refs para controlar paginação e evitar dependências circulares
  const currentPageRef = useRef(0);
  const loadConversationsRef = useRef<(status?: ChatStatus, reset?: boolean) => Promise<void>>();

  // WebSocket para atualizações em tempo real
  const webSocket = useWebSocket();
  
  // Chat store para mensagens em tempo real
  const { messagesCache, activeChat } = useChatStore();
  
  // Combinar mensagens locais (enviadas) com mensagens do WebSocket (recebidas)
  const activeMessages = React.useMemo(() => {
    const conversationId = state.selectedDonor?.conversationId;
    
    // Mensagens locais (enviadas pelo usuário + carregadas da API inicialmente)
    const localMessages = state.messages;
    
    // Mensagens do WebSocket (apenas de outros usuários)
    const webSocketMessages = conversationId && messagesCache[conversationId] 
      ? messagesCache[conversationId].messages.map(msg => ({
          id: msg.id,
          senderId: msg.senderId || "unknown",
          content: msg.content,
          timestamp: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit'
          }) : "",
          isAI: msg.isFromUser === false, // false = recebida do cliente (esquerda azul), true = enviada por mim/sistema (direita branco)
          attachments: msg.attachments,
          media: msg.media
        }))
      : [];
    
    // Combinar e remover duplicatas (apenas por ID exato)
    const allMessages = [...localMessages];
    webSocketMessages.forEach(wsMsg => {
      if (!allMessages.some(localMsg => localMsg.id === wsMsg.id)) {
        allMessages.push(wsMsg);
      }
    });
    
    // Ordenar por timestamp
    const sortedMessages = allMessages.sort((a, b) => {
      const timeA = new Date(`1970-01-01 ${a.timestamp || '00:00'}`).getTime();
      const timeB = new Date(`1970-01-01 ${b.timestamp || '00:00'}`).getTime();
      return timeA - timeB;
    });
    
    
    return sortedMessages;
  }, [state.selectedDonor, state.messages, messagesCache]);
  
  useEffect(() => {
    if (webSocket.isConnected) {
      // WebSocket connected
    } else if (authService.isAuthenticated()) {
      webSocket.connect();
    }
  }, [webSocket.isConnected]);

  // Carregar campanhas ativas da API
  const loadCampaigns = React.useCallback(async () => {
    try {
      setIsCampaignsLoading(true);
      const companyId = localStorage.getItem('auth_company_id');
      if (!companyId) {
        console.warn('Company ID não encontrado');
        return;
      }

      const activeCampaigns = await campaignApi.getActiveCampaigns(companyId);
      setCampaigns(activeCampaigns);
    } catch (error) {
      console.error('❌ Erro ao carregar campanhas:', error);
      setCampaigns([]);
    } finally {
      setIsCampaignsLoading(false);
    }
  }, []);

  // Carregar campanhas no mount
  useEffect(() => {
    loadCampaigns();
  }, [loadCampaigns]);


  const updateState = React.useCallback((updates: Partial<ChatState>) => {
    if (updates.messages !== undefined) {
    }
    setState((prev) => ({ ...prev, ...updates }));
  }, [state.messages]);

  // Converter ConversationDTO em Donor (desabilitado para usar mock data)
  /*
  const convertConversationToDonor = React.useCallback((conversation: ConversationDTO): Donor => {
    const customer = conversation.customer;
    const user = customer ? customerAdapter.toUser(customer) : {
      id: conversation.customerId,
      name: 'Cliente Desconhecido',
      avatar: '',
      isOnline: false,
      phone: ''
    };

    return {
      id: conversation.id,
      name: user.name,
      lastMessage: conversation.lastMessage?.content || "",
      timestamp: conversation.lastMessage?.createdAt ? 
        new Date(conversation.lastMessage.createdAt).toLocaleTimeString('pt-BR', { 
          hour: '2-digit', 
          minute: '2-digit' 
        }) : "",
      unread: 0, // TODO: implementar contagem real de não lidas
      status: "offline" as const,
      bloodType: "N/I", // Valor padrão para contatos sem dados médicos
      phone: user.phone || '',
      email: "",
      lastDonation: "Sem registro",
      totalDonations: 0,
      address: "",
      birthDate: "",
      weight: 0,
      height: 0,
    };
  }, []);
  */

  // Carregar conversas da API real
  const loadConversations = React.useCallback(async (status?: ChatStatus, reset = true) => {
    try {
      const statusToLoad = status || currentStatus;
      let pageToLoad: number;
      
      if (reset) {
        setIsLoading(true);
        setHasMorePages(true);
        currentPageRef.current = 0;
        pageToLoad = 0;
      } else {
        setIsLoadingMore(true);
        pageToLoad = currentPageRef.current + 1;
      }
      setError(null);

      // Mapear status do frontend para backend
      const backendStatus = conversationAdapter.mapStatusToBackend(statusToLoad);
      
      // Buscar conversas da API com paginação
      const response = await conversationApi.getByStatus(backendStatus as ConversationStatus, pageToLoad, 20);
      
      // Converter ConversationDTO para formato Donor (compatibilidade)
      const conversationsAsDonors = response.content.map(conv => {
        return {
          id: conv.customerId, // Usar customerId para buscar dados do customer
          conversationId: conv.id, // Incluir o ID da conversa
          name: conv.customerName || conv.customerPhone || 'Cliente',
          lastMessage: conv.lastMessage?.content || '',
          timestamp: conv.lastMessage?.createdAt ? 
            new Date(conv.lastMessage.createdAt).toLocaleTimeString('pt-BR', {
              hour: '2-digit',
              minute: '2-digit'
            }) : '',
          unread: 0, // TODO: Implementar contagem real de mensagens não lidas
          status: 'offline' as const,
          bloodType: conv.customerBloodType || 'Não informado',
          phone: conv.customerPhone || '',
          email: '',
          lastDonation: conv.customerLastDonationDate || 'Sem registro',
          totalDonations: 0, // TODO: Implementar contagem real
          address: '',
          birthDate: conv.customerBirthDate || '',
          weight: conv.customerWeight || 0,
          height: conv.customerHeight || 0,
          hasActiveConversation: true,
          conversationStatus: conv.status,
          campaignId: conv.campaignId, // Incluir o campaignId da conversa
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(conv.customerName || conv.customerPhone || 'C')}&background=random&size=150`
        };
      });

      // Atualizar estado da paginação
      currentPageRef.current = pageToLoad;
      const hasMore = pageToLoad + 1 < response.page.totalPages;
      setHasMorePages(hasMore);
      
      if (!hasMore) {
      }

      if (reset) {
        setDonors(conversationsAsDonors);
      } else {
        setDonors(prevDonors => [...prevDonors, ...conversationsAsDonors]);
      }
      
    } catch (err) {
      console.error('❌ Erro ao carregar conversas da API:', err);
      setError('Erro ao carregar conversas. Tente novamente.');
      setDonors([]);
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  }, [currentStatus]);

  // Atualizar ref
  loadConversationsRef.current = loadConversations;

  // Função simplificada para evitar dependências circulares
  const callLoadConversations = React.useCallback((status?: ChatStatus, reset = true) => {
    loadConversationsRef.current?.(status, reset);
  }, []);

  // Função para carregar mais conversas (scroll infinito)
  const loadMoreConversations = React.useCallback(async () => {
    if (!hasMorePages || isLoadingMore) return;
    
    await loadConversations(currentStatus, false);
  }, [currentStatus, hasMorePages, isLoadingMore, loadConversations]);

  // Carregar todos os contatos (customers) da API
  const loadAllContacts = React.useCallback(async () => {
    try {
      setIsLoadingContacts(true);

      // Buscar todos os customers
      const customersResponse = await customerApi.getAll({ size: 200 });
      
      // A API retorna array direto de customers
      let customers = [];
      if (customersResponse && Array.isArray(customersResponse)) {
        customers = customersResponse;
      } else if (customersResponse && customersResponse.content && Array.isArray(customersResponse.content)) {
        // Fallback para formato paginado
        customers = customersResponse.content;
      } else {
        console.warn('⚠️ Resposta da API customers não tem formato conhecido:', customersResponse);
        setAllContacts([]);
        return;
      }
      
      // Converter customers para Donors
      const contactsAsDonors = customers.map(customer => {
        const user = customerAdapter.toUser(customer);
        return {
          id: customer.id,
          name: user.name,
          lastMessage: "", // Contatos não têm lastMessage por padrão
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "Não informado",
          phone: user.phone || '',
          email: "",
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
        };
      });

      setAllContacts(contactsAsDonors);
    } catch (err) {
      console.error('❌ Erro ao carregar contatos:', err);
    } finally {
      setIsLoadingContacts(false);
    }
  }, []);

  // Função para trocar de status da aba
  const handleStatusChange = React.useCallback((newStatus: ChatStatus) => {
    setCurrentStatus(newStatus);
    callLoadConversations(newStatus);
    updateState({ selectedDonor: null }); // Limpar seleção ao trocar status
    setCurrentDraftMessage(null); // Limpar draft ao trocar status
  }, [callLoadConversations, updateState]);

  // Função para trocar de campanha
  const handleCampaignChange = React.useCallback((campaign: Campaign | null) => {
    setSelectedCampaign(campaign);
    updateState({ selectedDonor: null, selectedCampaign: campaign }); // Limpar seleção ao trocar campanha
    callLoadConversations(currentStatus);
  }, [callLoadConversations, updateState, currentStatus]);

  // Função para trocar modo de visualização
  const handleViewModeChange = React.useCallback((mode: ViewMode) => {
    updateState({ viewMode: mode });
  }, [updateState]);

  // Função para trocar status de uma conversa específica
  const handleConversationStatusChange = React.useCallback(async (donorId: string, newStatus: ChatStatus) => {
    
    // Encontrar o donor atual
    const donor = donors.find(d => d.id === donorId);
    if (!donor) return;

    // Verificar se tem conversationId
    const conversationId = donor.conversationId || donor.id;
    
    try {
      // Chamar API para mudar status no backend
      const backendStatus = conversationAdapter.mapStatusToBackend(newStatus);
      
      await conversationApi.changeStatus(conversationId, backendStatus as any);
      
      // Remover da lista atual após sucesso na API
      setDonors(prev => prev.filter(d => d.id !== donorId));
      
      // Se o donor selecionado foi movido, limpar seleção
      if (state.selectedDonor?.id === donorId) {
        updateState({ selectedDonor: null, messages: [] });
      }

      // Mostrar feedback
      const statusLabels: Record<ChatStatus, string> = {
        ativos: 'Ativo',
        aguardando: 'Aguardando', 
        inativo: 'Inativo',
        entrada: 'Entrada',
        esperando: 'Esperando',
        finalizados: 'Finalizados'
      };
      
      
      // Se mudou para o status atual, recarregar para mostrar na lista
      if (newStatus === currentStatus) {
        setTimeout(() => {
          callLoadConversations(currentStatus);
        }, 100);
      }
      
    } catch (error) {
      console.error('❌ Erro ao mudar status da conversa:', error);
      
      // Mostrar modal de erro
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: 'Erro ao Alterar Status',
          message: `Não foi possível alterar o status da conversa de ${donor.name}. Deseja tentar novamente?`,
          type: 'warning',
          confirmText: 'Tentar Novamente',
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null
            });
            // Tentar novamente
            handleConversationStatusChange(donorId, newStatus);
          }
        }
      });
    }
  }, [donors, state.selectedDonor, updateState, currentStatus, loadConversations, conversationAdapter, conversationApi]);

  // Função para lidar com agendamento
  const handleSchedule = React.useCallback((scheduleData: { type: string; date: string; time: string; notes: string }) => {
    const donor = state.scheduleTarget;
    if (!donor) return;

    
    const typeLabels = {
      doacao: 'doação',
      triagem: 'triagem médica',
      retorno: 'consulta de retorno',
      orientacao: 'orientação'
    };

    // Adicionar mensagem de agendamento à conversa
    const agendamentoMessage = {
      id: `schedule_${Date.now()}`,
      senderId: "ai",
      content: `Perfeito! Agendei sua ${typeLabels[scheduleData.type as keyof typeof typeLabels] || 'doação'} para ${new Date(scheduleData.date).toLocaleDateString('pt-BR')} às ${scheduleData.time}. Confirma presença? 📅${scheduleData.notes ? `\n\nObservações: ${scheduleData.notes}` : ''}`,
      timestamp: getCurrentTimestamp(),
      isAI: true,
    };

    // Se é a conversa ativa, adicionar a mensagem
    if (state.selectedDonor?.id === donor.id) {
      updateState({
        messages: [...state.messages, agendamentoMessage]
      });
    }

    // Fechar modal
    updateState({
      showScheduleModal: false,
      scheduleTarget: null
    });

  }, [state.scheduleTarget, state.selectedDonor, state.messages, updateState]);

  // Função para agendamento direto via botão do header
  const handleDirectSchedule = React.useCallback((donorId: string) => {
    const donor = donors.find(d => d.id === donorId) || allContacts.find(c => c.id === donorId);
    if (donor) {
      updateState({
        showScheduleModal: true,
        scheduleTarget: donor
      });
    }
  }, [donors, allContacts, updateState]);

  // Carregar dados ao montar componente
  useEffect(() => {
    callLoadConversations();
  }, [callLoadConversations]);

  // Cleanup URLs de preview ao desmontar o componente
  useEffect(() => {
    return () => {
      state.pendingMedia.forEach(media => {
        if (media.previewUrl) {
          URL.revokeObjectURL(media.previewUrl);
        }
      });
    };
  }, [state.pendingMedia]);

  // Função para abrir modal e carregar contatos
  const handleOpenNewChatModal = React.useCallback(() => {
    updateState({ showNewChatModal: true });
    loadAllContacts(); // Carrega todos os contatos quando abre o modal
  }, [updateState, loadAllContacts]);

  const filteredAvailableContacts = allContacts.filter((contact) =>
    contact.name.toLowerCase().includes(state.newChatSearch.toLowerCase())
  );

  const handleDonorSelect = React.useCallback(async (donor: Donor) => {
    
    // Carregar mensagens da API primeiro
    let donorMessages: Message[] = [];
    let draftMessage: any = null;
    
    try {
      if (donor.conversationId) {
        const messagesResponse = await messageApi.getByConversation(donor.conversationId);
        
        // A API retorna array direto, não objeto com content
        const allMessages = Array.isArray(messagesResponse) ? messagesResponse : 
                          (messagesResponse && messagesResponse.content) ? messagesResponse.content : [];
        
        if (allMessages.length > 0) {
          // Filtrar mensagens por status e buscar DRAFT
          const draftMessages = allMessages.filter(msg => msg.status === 'DRAFT');
          
          // Se encontrou mensagem DRAFT, pegar a mais recente
          if (draftMessages.length > 0) {
            draftMessage = draftMessages.sort((a, b) => 
              new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
            )[0];
          }
          
          // Converter todas as mensagens exceto DRAFT para exibição
          const filteredMessages = allMessages.filter(msg => msg.status !== 'DRAFT');
          
          // Log das mensagens antes da ordenação
          
          // Ordenar por data de criação (mais antigas primeiro)
          const sortedMessages = filteredMessages.sort((a, b) => {
            const dateA = new Date(a.createdAt || 0);
            const dateB = new Date(b.createdAt || 0);
            const timeA = dateA.getTime();
            const timeB = dateB.getTime();
            
            
            return timeA - timeB;
          });
          
          // Log das mensagens após ordenação
          
          donorMessages = sortedMessages.map(msg => ({
            id: msg.id,
            senderId: msg.senderId || "unknown",
            content: msg.content,
            timestamp: msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('pt-BR', {
              hour: '2-digit',
              minute: '2-digit'
            }) : "",
            isAI: msg.senderType === 'CUSTOMER', // true = recebida do cliente (esquerda azul), false = enviada por mim/sistema (direita branco)
            attachments: msg.mediaUrl ? [{
              id: `media_${msg.id}`,
              name: msg.mediaUrl.split('/').pop() || 'arquivo',
              size: 0,
              type: 'application/octet-stream',
              url: msg.mediaUrl
            }] : undefined,
          }));
          
        }
      }
    } catch (error) {
      console.error('❌ Erro ao carregar mensagens da API:', error);
    }
    
    // Preservar mensagens temporárias (otimistas) mesmo em conversas diferentes
    const tempMessages = state.messages.filter(msg => msg.id.startsWith('temp-'));
    const isReloadingSameConversation = state.selectedDonor?.conversationId === donor.conversationId;
    
    let messagesToUse: Message[];
    if (isReloadingSameConversation) {
      // Mesma conversa: manter todas as mensagens locais - MAS ORDENAR ELAS!
      
      // Ordenar mensagens locais por timestamp (que é string HH:MM)
      const sortedLocalMessages = [...state.messages].sort((a, b) => {
        // Função para converter HH:MM em minutos
        const parseTime = (timeStr: string) => {
          if (!timeStr) return 0;
          const [hours, minutes] = timeStr.split(':').map(Number);
          return (hours * 60) + minutes;
        };
        
        // Para mensagens temporárias, usar timestamp atual
        if (a.id.startsWith('temp-') && b.id.startsWith('temp-')) {
          return parseTime(a.timestamp) - parseTime(b.timestamp);
        }
        // Se uma é temporária, ela vai por último (mais recente)
        if (a.id.startsWith('temp-')) return 1;
        if (b.id.startsWith('temp-')) return -1;
        // Para mensagens reais, usar timestamp
        return parseTime(a.timestamp) - parseTime(b.timestamp);
      });
      
      
      messagesToUse = sortedLocalMessages;
    } else {
      // Conversa diferente: usar mensagens da API + preservar temporárias da conversa atual
      const relevantTempMessages = tempMessages.filter(msg => 
        // Verificar se a mensagem temporária é da conversa que estamos carregando
        donor.conversationId && msg.senderId // Se tem conversationId e senderId
      );
      messagesToUse = [...donorMessages, ...relevantTempMessages];
    }
    
    // FORCE FINAL SORT - Garantir ordem cronológica final
    const parseTimeToMinutes = (timeStr: string) => {
      if (!timeStr) return 0;
      const [hours, minutes] = timeStr.split(':').map(Number);
      return (hours * 60) + minutes;
    };
    
    // Ordenar mensagens cronologicamente (mais antigas primeiro)
    messagesToUse.sort((a, b) => {
      const timeA = parseTimeToMinutes(a.timestamp);
      const timeB = parseTimeToMinutes(b.timestamp);
      return timeA - timeB;
    });
    
    

    // Atualizar estado com mensagens e mensagem DRAFT no input se encontrada
    updateState({
      selectedDonor: donor,
      showNewChatModal: false,
      showDonorInfo: false,
      messages: messagesToUse,
      messageInput: draftMessage?.content || "", // Colocar DRAFT no input se existir
    });
    
    // Definir conversa ativa no store para receber mensagens WebSocket
    if (donor.conversationId) {
      const store = useChatStore.getState();
      store.setActiveConversation(donor.conversationId);
      
      // Inicializar cache de mensagens se não existir
      if (!store.messagesCache[donor.conversationId] && donorMessages.length > 0) {
        store.messagesCache[donor.conversationId] = {
          messages: donorMessages.map(msg => ({
            id: msg.id,
            content: msg.content,
            senderId: msg.senderId,
            timestamp: msg.timestamp,
            isFromUser: !msg.isAI,
            attachments: msg.attachments,
            media: msg.media
          })),
          page: 0,
          hasMore: false
        };
      }
    }
    
    // Armazenar referência da mensagem DRAFT para o MessageInput
    setCurrentDraftMessage(draftMessage);
    if (draftMessage) {
    }
  }, [updateState]);

  // Função reutilizável para carregar dados completos do customer e abrir modal (DRY)
  const handleOpenDonorProfile = React.useCallback(async (donor: Donor) => {
    try {
      
      // Buscar dados completos do customer na API
      const customerData = await customerApi.getById(donor.id);
      
      // Atualizar o donor com os dados completos
      const updatedDonor = customerAdapter.updateDonorWithCustomerData(donor, customerData);
      
      updateState({
        selectedDonor: updatedDonor,
        showDonorInfo: true
      });
    } catch (error) {
      console.error('❌ Erro ao carregar dados do customer:', error);
      // Abrir modal mesmo com erro, mostrando dados que já temos
      updateState({
        selectedDonor: donor,
        showDonorInfo: true
      });
    }
  }, [updateState]);

  // Função para carregar dados completos do customer e abrir modal (ChatHeader)
  const handleDonorInfoClick = React.useCallback(async () => {
    if (!state.selectedDonor) return;
    await handleOpenDonorProfile(state.selectedDonor);
  }, [state.selectedDonor, handleOpenDonorProfile]);

  const createDonorFromContact = (contactData: NewContactData): Donor => {
    return {
      id: Date.now().toString(),
      name: contactData.name,
      lastMessage: "",
      timestamp: "",
      unread: 0,
      status: "offline",
      bloodType: "Não informado",
      phone: contactData.phone,
      email: "",
      lastDonation: "Sem registro",
      totalDonations: 0,
      address: "",
      birthDate: "",
      weight: 0,
      height: 0,
    };
  };

  const handleNewContactCreate = React.useCallback(async (contactData: NewContactData) => {
    
    // Se o donor foi criado via API, usar ele diretamente
    if (contactData.donor) {
      // Adicionar à lista de donors (conversas ativas) sempre que tiver uma conversa criada
      // Mesmo sem lastMessage, para que apareça na sidebar imediatamente
      setDonors((prev) => {
        const exists = prev.some(d => d.id === contactData.donor!.id);
        if (exists) return prev;
        return [...prev, contactData.donor!];
      });
      
      // Sempre adicionar à lista de todos os contatos
      setAllContacts((prev) => {
        // Verificar se já existe para evitar duplicatas
        const exists = prev.some(contact => contact.id === contactData.donor!.id);
        if (exists) return prev;
        return [...prev, contactData.donor!];
      });
      
      return; // handleDonorSelect já foi chamado no NewChatModal
    }
    
    // Fallback para criação local (compatibilidade)
    const newDonor = createDonorFromContact(contactData);
    setDonors((prev) => [...prev, newDonor]);
    setAllContacts((prev) => [...prev, newDonor]);
    handleDonorSelect(newDonor);
  }, [handleDonorSelect]);

  const handleContextMenu = (e: React.MouseEvent, donorId: string) => {
    e.preventDefault();
    updateState({
      contextMenu: {
        show: true,
        x: e.clientX,
        y: e.clientY,
        donorId,
      },
    });
  };

  const handleContextMenuAction = React.useCallback(async (action: string, donorId: string) => {
    
    // Encontrar o donor
    const donor = donors.find(d => d.id === donorId) || allContacts.find(c => c.id === donorId);
    if (!donor) {
      console.error('Donor não encontrado:', donorId);
      updateState({
        contextMenu: { show: false, x: 0, y: 0, donorId: "" },
      });
      return;
    }

    switch (action) {
      case 'view-conversation':
        // Selecionar a conversa (igual ao clique normal)
        handleDonorSelect(donor);
        break;

      case 'schedule-donation':
        // Abrir modal de agendamento
        updateState({
          showScheduleModal: true,
          scheduleTarget: donor
        });
        break;

      case 'view-profile':
        // Reutiliza a mesma função do ChatHeader (DRY)
        await handleOpenDonorProfile(donor);
        break;

      default:
        console.warn('Ação não implementada:', action);
    }

    // Fechar menu de contexto
    updateState({
      contextMenu: { show: false, x: 0, y: 0, donorId: "" },
    });
  }, [donors, allContacts, handleDonorSelect, handleOpenDonorProfile, updateState, state.selectedDonor, setDonors]);

  const handleFileUpload = (files: FileList | null) => {
    if (!files) return;

    const newAttachments: FileAttachment[] = Array.from(files).map((file) => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
    }));

    updateState({
      attachments: [...state.attachments, ...newAttachments],
    });
  };

  const handleEnhanceMessage = () => {
    setShowMessageEnhancer(true);
  };

  const handleApplyEnhancedMessage = (enhancedMessage: string) => {
    updateState({ messageInput: enhancedMessage });
    setShowMessageEnhancer(false);
  };

  const handleMediaSelected = (file: File) => {
    // Validar arquivo
    const validation = mediaApi.validateFile(file);
    if (!validation.valid) {
      handleMediaError(validation.error || 'Arquivo inválido');
      return;
    }

    // Determinar tipo de mídia
    let mediaType: PendingMedia['mediaType'] = 'OTHER';
    if (file.type.startsWith('image/')) mediaType = 'IMAGE';
    else if (file.type.startsWith('video/')) mediaType = 'VIDEO';
    else if (file.type.startsWith('audio/')) mediaType = 'AUDIO';
    else if (file.type.includes('pdf') || file.type.includes('document') || file.type.includes('text')) mediaType = 'DOCUMENT';

    // Criar preview local
    const previewUrl = file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined;

    const pendingMedia: PendingMedia = {
      id: `pending-${Date.now()}-${Math.random()}`,
      file,
      mediaType,
      mimeType: file.type,
      originalFileName: file.name,
      fileSizeBytes: file.size,
      previewUrl,
    };

    updateState({
      pendingMedia: [...state.pendingMedia, pendingMedia],
    });
  };

  const handleRemovePendingMedia = (mediaId: string) => {
    const mediaToRemove = state.pendingMedia.find(m => m.id === mediaId);
    if (mediaToRemove?.previewUrl) {
      URL.revokeObjectURL(mediaToRemove.previewUrl);
    }
    
    updateState({
      pendingMedia: state.pendingMedia.filter(m => m.id !== mediaId),
    });
  };

  const handleMediaError = (error: string) => {
    updateState({
      showConfirmationModal: true,
      confirmationData: {
        title: 'Erro no Upload',
        message: error,
        type: 'warning',
        confirmText: 'OK',
        onConfirm: () => {
          updateState({
            showConfirmationModal: false,
            confirmationData: null
          });
        }
      }
    });
  };

  const handleSendMessage = async () => {

    if (!state.messageInput.trim() && state.attachments.length === 0 && state.pendingMedia.length === 0) {
      return;
    }

    if (!state.selectedDonor) {
      return;
    }

    if (isCreatingConversation) {
      return;
    }

    let conversationId = state.selectedDonor.conversationId;
    
    // Verificar se é a primeira mensagem de um novo contato
    const isFirstMessage = !state.selectedDonor.hasActiveConversation && !state.selectedDonor.lastMessage;

    if (isFirstMessage) {
      setIsCreatingConversation(true);
      
      try {
        
        // Criar conversa para este cliente usando o adapter
        const createRequest = conversationAdapter.toCreateRequest(
          state.selectedDonor.id,
          'WHATSAPP'
        );
        
        const newConversation = await conversationApi.create(createRequest);
        conversationId = newConversation.id;

        // Atualizar o donor para marcar que agora tem conversa ativa
        const updatedDonor = {
          ...state.selectedDonor,
          conversationId: newConversation.id,
          hasActiveConversation: true,
          lastMessage: messageContent.trim() || "Anexo enviado", // Usar messageContent armazenado
          timestamp: getCurrentTimestamp(),
        };

        // Atualizar lista de donors
        setDonors(prev => {
          const existingDonorIndex = prev.findIndex(d => d.id === state.selectedDonor?.id);
          let updated;
          
          if (existingDonorIndex >= 0) {
            // Doador existe, atualizar
            updated = prev.map(d => 
              d.id === state.selectedDonor?.id ? updatedDonor : d
            );
          } else {
            // Doador não existe, adicionar
            updated = [...prev, updatedDonor];
          }
          
          return updated;
        });

        // Atualizar selectedDonor
        updateState({ selectedDonor: updatedDonor });

      } catch (error) {
        console.error('❌ Erro ao criar conversa:', error);
        
        // Mostrar feedback de erro ao usuário
        updateState({
          showConfirmationModal: true,
          confirmationData: {
            title: 'Erro ao Criar Conversa',
            message: 'Não foi possível criar a conversa. Deseja tentar novamente?',
            type: 'warning',
            confirmText: 'Tentar Novamente',
            onConfirm: () => {
              updateState({
                showConfirmationModal: false,
                confirmationData: null
              });
              // Tentar novamente após fechar o modal
              setTimeout(() => handleSendMessage(), 100);
            }
          }
        });
        
        setIsCreatingConversation(false);
        return; // Não enviar a mensagem se falhou ao criar conversa
      } finally {
        setIsCreatingConversation(false);
      }
    }

    if (!conversationId) {
      console.error('❌ ID da conversa não encontrado');
      return;
    }

    // Armazenar conteúdo antes de limpar o input
    const messageContent = state.messageInput;
    
    // Se há mensagem DRAFT, atualizar status para SENT
    if (currentDraftMessage) {
      try {
        await messageApi.updateMessageStatus(currentDraftMessage.id, "SENT");
        setCurrentDraftMessage(null); // Limpar draft após envio
      } catch (error) {
        console.error('❌ Erro ao atualizar status da mensagem DRAFT:', error);
      }
    }

    // Criar mensagem temporária para UI otimista
    const currentUser = useAuthStore.getState().user;
    const tempMessage: Message = {
      id: `temp-${Date.now()}`,
      senderId: currentUser?.id || "user",
      content: messageContent,
      timestamp: getCurrentTimestamp(),
      isAI: false,
      attachments: state.attachments.length > 0 ? [...state.attachments] : undefined,
      // Converter pendingMedia para um formato de preview
      media: state.pendingMedia.length > 0 ? state.pendingMedia.map(pm => ({
        id: pm.id,
        conversationId: conversationId || '',
        fileUrl: pm.previewUrl || 'uploading...',
        mediaType: pm.mediaType,
        mimeType: pm.mimeType,
        originalFileName: pm.originalFileName,
        fileSizeBytes: pm.fileSizeBytes,
        uploadedAt: new Date().toISOString(),
      })) : undefined,
    };

    // Armazenar pendingMedia para upload
    const mediaToUpload = [...state.pendingMedia];

    // Atualizar UI imediatamente (optimistic update)
    
    const newMessages = [...state.messages, tempMessage];
    
    updateState({
      messages: newMessages,
      messageInput: "",
      attachments: [],
      pendingMedia: [],
    });
    

    try {
      
      // Upload das mídias pendentes usando Z-API
      let zapiResult: any = null;
      if (mediaToUpload.length > 0) {
        
        // Por enquanto, fazer upload apenas do primeiro arquivo
        const firstPendingMedia = mediaToUpload[0];
        const donorPhone = state.selectedDonor.phone;
        
        if (!donorPhone) {
          throw new Error('Número de telefone do doador não encontrado');
        }

        try {
          // Usar upload direto para Z-API
          zapiResult = await mediaApi.uploadForZApi(
            firstPendingMedia.file,
            donorPhone,
            messageContent || undefined
          );
          
        } catch (uploadError) {
          console.error('❌ Erro no upload de mídia Z-API:', uploadError);
          throw new Error('Falha no upload da mídia via Z-API');
        }
      }
      
      // Se teve upload de mídia via Z-API, a mensagem já foi enviada
      if (zapiResult) {
        
        // Atualizar UI para mostrar sucesso usando setState callback
        setState(currentState => {
          
          return {
            ...currentState,
            messages: currentState.messages.map(msg => 
              msg.id === tempMessage.id 
                ? {
                    ...msg,
                    id: zapiResult.messageId,
                    timestamp: getCurrentTimestamp()
                  }
                : msg
            )
          };
        });

        // Atualizar lastMessage do doador na sidebar para mensagens com mídia
        if (state.selectedDonor) {
          const updatedDonor = {
            ...state.selectedDonor,
            lastMessage: messageContent || "Anexo enviado",
            timestamp: getCurrentTimestamp(),
          };

          setDonors(prev => {
            const updated = prev.map(d => 
              d.id === state.selectedDonor?.id ? updatedDonor : d
            );
            return updated;
          });

          updateState({ selectedDonor: updatedDonor });
        }
        
        return; // Não precisa enviar mensagem separada
      }
      
      // Enviar mensagem de texto normal se não há mídia
      
      const sentMessage = await messageApi.send(conversationId, {
        content: messageContent,
        messageType: 'TEXT'
      });


      // Substituir mensagem temporária pela real - usando setState callback para ter estado atualizado
      setState(currentState => {
        const tempExists = currentState.messages.some(msg => msg.id === tempMessage.id);
        
        if (!tempExists) {
          return {
            ...currentState,
            messages: [...currentState.messages, {
              ...tempMessage,
              id: sentMessage.id,
              timestamp: sentMessage.createdAt ? 
                new Date(sentMessage.createdAt).toLocaleTimeString('pt-BR', {
                  hour: '2-digit',
                  minute: '2-digit'
                }) : tempMessage.timestamp
            }]
          };
        }
        
        return {
          ...currentState,
          messages: currentState.messages.map(msg => 
            msg.id === tempMessage.id 
              ? {
                  ...msg,
                  id: sentMessage.id,
                  timestamp: sentMessage.createdAt ? 
                    new Date(sentMessage.createdAt).toLocaleTimeString('pt-BR', {
                      hour: '2-digit',
                      minute: '2-digit'
                    }) : msg.timestamp
                }
              : msg
          )
        };
      });

      // Atualizar lastMessage do doador na sidebar para mensagens não iniciais
      if (state.selectedDonor && !isFirstMessage) {
        const updatedDonor = {
          ...state.selectedDonor,
          lastMessage: messageContent,
          timestamp: sentMessage.createdAt ? 
            new Date(sentMessage.createdAt).toLocaleTimeString('pt-BR', {
              hour: '2-digit',
              minute: '2-digit'
            }) : getCurrentTimestamp(),
        };

        setDonors(prev => {
          const updated = prev.map(d => 
            d.id === state.selectedDonor?.id ? updatedDonor : d
          );
          return updated;
        });

        updateState({ selectedDonor: updatedDonor });
      }

    } catch (error) {
      console.error('❌ Erro ao enviar mensagem:', error);
      
      // Limpar URLs de preview em caso de erro
      mediaToUpload.forEach(media => {
        if (media.previewUrl) {
          URL.revokeObjectURL(media.previewUrl);
        }
      });
      
      // Remover mensagem temporária em caso de erro
      updateState({
        messages: state.messages.filter(msg => msg.id !== tempMessage.id),
        // Restaurar pendingMedia em caso de erro para o usuário tentar novamente
        pendingMedia: mediaToUpload,
      });
      
      // Mostrar feedback de erro
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: 'Erro ao Enviar Mensagem',
          message: 'Não foi possível enviar a mensagem. Deseja tentar novamente?',
          type: 'warning',
          confirmText: 'Tentar Novamente',
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
              messageInput: messageContent
            });
          }
        }
      });
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    updateState({ isDragging: true });
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    updateState({ isDragging: false });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    updateState({ isDragging: false });
    handleFileUpload(e.dataTransfer.files);
  };

  useEffect(() => {
    const handleClickOutside = () => {
      if (state.contextMenu.show) {
        updateState({
          contextMenu: { show: false, x: 0, y: 0, donorId: "" },
        });
      }
    };

    if (state.contextMenu.show) {
      document.addEventListener("click", handleClickOutside);
      return () => document.removeEventListener("click", handleClickOutside);
    }
  }, [state.contextMenu.show, updateState]);

  // Recarregar dados quando voltar da configuração
  const [wasInConfiguration, setWasInConfiguration] = useState(false);
  
  useEffect(() => {
    if (state.showConfiguration) {
      setWasInConfiguration(true);
    } else if (wasInConfiguration) {
      callLoadConversations();
      loadCampaigns();
      setWasInConfiguration(false);
    }
  }, [state.showConfiguration, callLoadConversations, loadCampaigns, wasInConfiguration]);

  if (state.showConfiguration) {
    return (
      <ConfigurationPage
        onBack={() => {
          updateState({ showConfiguration: false });
        }}
      />
    );
  }

  return (
    <div className="flex h-screen bg-white">
      <ContextMenuComponent
        contextMenu={state.contextMenu}
        onAction={handleContextMenuAction}
        currentStatus={currentStatus}
        onStatusChange={handleConversationStatusChange}
      />

      <DonorSidebar
        donors={donors}
        selectedDonor={state.selectedDonor}
        searchTerm={state.searchTerm}
        onSearchChange={(term) => updateState({ searchTerm: term })}
        onDonorSelect={handleDonorSelect}
        onNewChat={handleOpenNewChatModal}
        onContextMenu={handleContextMenu}
        isLoading={isLoading}
        error={error}
        onRetry={() => callLoadConversations()}
        onConfigClick={() => updateState({ showConfiguration: true })}
        currentStatus={currentStatus}
        onStatusChange={handleStatusChange}
        campaigns={campaigns}
        selectedCampaign={selectedCampaign}
        onCampaignChange={handleCampaignChange}
        viewMode={state.viewMode}
        onViewModeChange={handleViewModeChange}
        hasMorePages={hasMorePages}
        isLoadingMore={isLoadingMore}
        onLoadMore={loadMoreConversations}
      />

      <NewChatModal
        show={state.showNewChatModal}
        searchTerm={state.newChatSearch}
        availableDonors={filteredAvailableContacts}
        onClose={() => updateState({ showNewChatModal: false })}
        onSearchChange={(term) => updateState({ newChatSearch: term })}
        onDonorSelect={handleDonorSelect}
        onNewContactCreate={handleNewContactCreate}
        isLoadingContacts={isLoadingContacts}
      />

      <DonorInfoModal
        show={state.showDonorInfo}
        donor={state.selectedDonor}
        onClose={() => updateState({ showDonorInfo: false })}
      />

      <ScheduleModal
        show={state.showScheduleModal}
        donor={state.scheduleTarget}
        onClose={() => updateState({ showScheduleModal: false, scheduleTarget: null })}
        onSchedule={handleSchedule}
      />

      {state.confirmationData && (
        <ConfirmationModal
          show={state.showConfirmationModal}
          title={state.confirmationData.title}
          message={state.confirmationData.message}
          type={state.confirmationData.type}
          confirmText={state.confirmationData.confirmText}
          onConfirm={state.confirmationData.onConfirm}
          onCancel={() => updateState({ 
            showConfirmationModal: false, 
            confirmationData: null 
          })}
        />
      )}

      <MessageEnhancerModal
        show={showMessageEnhancer}
        originalMessage={state.messageInput}
        onClose={() => setShowMessageEnhancer(false)}
        onApply={handleApplyEnhancedMessage}
      />

      <div className="flex-1 flex flex-col">
        {state.selectedDonor ? (
          <>
            <ChatHeader
              donor={state.selectedDonor}
              onDonorInfoClick={handleDonorInfoClick}
              currentStatus={currentStatus}
              onStatusChange={handleConversationStatusChange}
              onScheduleClick={handleDirectSchedule}
            />

            <MessageList
              messages={activeMessages}
              isDragging={state.isDragging}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            />

            <MessageInput
              messageInput={state.messageInput}
              attachments={state.attachments}
              pendingMedia={state.pendingMedia}
              conversationId={state.selectedDonor?.conversationId}
              draftMessage={currentDraftMessage}
              onMessageChange={(value) => updateState({ messageInput: value })}
              onSendMessage={handleSendMessage}
              onFileUpload={handleFileUpload}
              onRemoveAttachment={(id) =>
                updateState({
                  attachments: state.attachments.filter((att) => att.id !== id),
                })
              }
              onMediaSelected={handleMediaSelected}
              onRemovePendingMedia={handleRemovePendingMedia}
              onKeyPress={handleKeyPress}
              onEnhanceMessage={handleEnhanceMessage}
              onError={handleMediaError}
            />
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <Heart className="w-16 h-16 text-gray-300 mb-4 mx-auto" />
              <h3 className="text-xl text-gray-400 mb-2 font-medium">
                Selecione um doador
              </h3>
              <p className="text-gray-400 m-0">
                Escolha uma conversa existente ou inicie uma nova
              </p>
            </div>
          </div>
        )}
      </div>
      
      {/* Monitor de conexão WhatsApp */}
      <WhatsAppConnectionMonitor checkInterval={180000} /> {/* 3 minutos */}
    </div>
  );
};
