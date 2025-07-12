import React, { useState, useEffect } from "react";
import { Heart } from "lucide-react";
import type { Donor, Message, FileAttachment, ChatState, ViewMode } from "../types/types";
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
import { customerAdapter } from "../adapters/customerAdapter";
import { conversationAdapter } from "../adapters/conversationAdapter";
import type { ConversationDTO } from "../api/types";
import { getMessagesForDonor } from "../mocks/data";
import { getDonorsByCampaignAndStatus, getMessagesByCampaign, getAllDonorsByStatus } from "../mocks/campaignData";
import { mockCampaigns } from "../mocks/campaigns";
import { getAllCampaignConversations, getContactsByCampaign } from "../mocks/campaignMock";
// import { conversationAdapter } from "../adapters/conversationAdapter";
import type { ChatStatus } from "../types/index";
import type { Campaign } from "../types/types";

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
  const [campaigns, setCampaigns] = useState<Campaign[]>(mockCampaigns);
  const [selectedCampaign, setSelectedCampaign] = useState<Campaign | null>(null);

  // Recarregar campanhas periodicamente para pegar novas campanhas criadas
  useEffect(() => {
    const reloadCampaigns = () => {
      console.log('🔄 Recarregando lista de campanhas...')
      setCampaigns([...mockCampaigns])
    }

    // Recarregar campanhas a cada 5 segundos
    const interval = setInterval(reloadCampaigns, 5000)
    
    return () => clearInterval(interval)
  }, []);

  // Converter conversas de campanha em Donors para compatibilidade
  const convertCampaignConversationsToDonors = React.useCallback((campaignId?: string, status?: ChatStatus): Donor[] => {
    const campaignConversations = getAllCampaignConversations()
    console.log('📞 Conversas de campanha encontradas:', campaignConversations.length)
    
    if (campaignConversations.length === 0) {
      return []
    }

    // Filtrar por campanha se especificada
    let filteredConversations = campaignConversations
    if (campaignId) {
      const campaignContacts = getContactsByCampaign(campaignId)
      const campaignConversationIds = campaignContacts
        .filter(c => c.conversation)
        .map(c => c.conversation!.id)
      
      filteredConversations = campaignConversations.filter(conv => 
        campaignConversationIds.includes(conv.id)
      )
    }

    // Filtrar por status se especificado
    if (status) {
      filteredConversations = filteredConversations.filter(conv => {
        switch (status) {
          case 'ativos':
            return conv.status === 'ENTRADA'
          case 'aguardando':
            return conv.status === 'ESPERANDO'
          case 'inativo':
            return conv.status === 'FINALIZADOS'
          default:
            return true
        }
      })
    }

    // Converter para Donors
    return filteredConversations.map(conv => {
      const customer = conv.customer!
      return {
        id: customer.id,
        name: customer.name || 'Cliente',
        avatar: customer.profileUrl || '',
        lastMessage: conv.lastMessage?.content || 'Mensagem inicial enviada',
        timestamp: new Date(conv.updatedAt).toLocaleTimeString('pt-BR', {
          hour: '2-digit',
          minute: '2-digit'
        }),
        unread: conv.status === 'ESPERANDO' ? 1 : 0,
        status: 'offline' as const,
        bloodType: 'N/I',
        phone: customer.phone,
        email: '',
        lastDonation: 'Sem registro',
        totalDonations: 0,
        address: '',
        birthDate: '',
        weight: 0,
        height: 0,
        hasActiveConversation: true,
        conversationStatus: conv.status,
        campaignId: findCampaignIdForConversation(conv.id)
      }
    })
  }, []);

  // Encontrar ID da campanha para uma conversa
  const findCampaignIdForConversation = (conversationId: string): string | undefined => {
    for (const campaign of campaigns) {
      const contacts = getContactsByCampaign(campaign.id)
      if (contacts.some(c => c.conversation?.id === conversationId)) {
        return campaign.id
      }
    }
    return undefined
  };

  const updateState = React.useCallback((updates: Partial<ChatState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  }, []);

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

  // Carregar conversas usando dados mock
  const loadConversations = React.useCallback(async (status?: ChatStatus, campaignId?: string) => {
    try {
      setIsLoading(true);
      setError(null);

      const statusToLoad = status || currentStatus;
      const campaignToLoad = campaignId || selectedCampaign?.id;
      
      console.log(`🔄 Carregando conversas para status: ${statusToLoad}, campanha: ${campaignToLoad || 'todas'}...`);

      // Simular delay da API
      await new Promise(resolve => setTimeout(resolve, 500));

      // Primeiro tentar carregar conversas de campanha (novas)
      let campaignDonors = convertCampaignConversationsToDonors(campaignToLoad, statusToLoad);
      
      // Se não há conversas de campanha, usar dados mock antigos
      let mockDonors: Donor[];
      if (campaignDonors.length > 0) {
        console.log(`✅ Usando ${campaignDonors.length} conversas de campanha`);
        mockDonors = campaignDonors;
      } else {
        console.log('📚 Usando dados mock legacy');
        if (campaignToLoad) {
          mockDonors = getDonorsByCampaignAndStatus(campaignToLoad, statusToLoad);
        } else {
          mockDonors = getAllDonorsByStatus(statusToLoad);
        }
      }

      console.log(`✅ Carregadas ${mockDonors.length} conversas para status ${statusToLoad} e campanha ${campaignToLoad || 'todas'}`);

      setDonors(mockDonors);
    } catch (err) {
      console.error('❌ Erro ao carregar conversas:', err);
      setError('Erro ao carregar conversas. Tente novamente.');
    } finally {
      setIsLoading(false);
    }
  }, [currentStatus, selectedCampaign, convertCampaignConversationsToDonors]);

  // Carregar todos os contatos (customers) da API
  const loadAllContacts = React.useCallback(async () => {
    try {
      setIsLoadingContacts(true);
      console.log('👥 Carregando todos os contatos...');

      // Buscar todos os customers
      const customersResponse = await customerApi.getAll({ size: 200 });
      console.log('👥 Carregando customers - recebidos:', Array.isArray(customersResponse) ? customersResponse.length : 0);
      
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
          bloodType: "N/I",
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

      console.log(`✅ Carregados ${contactsAsDonors.length} contatos para modal`);
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
    loadConversations(newStatus, selectedCampaign?.id);
    updateState({ selectedDonor: null }); // Limpar seleção ao trocar status
  }, [loadConversations, updateState, selectedCampaign]);

  // Função para trocar de campanha
  const handleCampaignChange = React.useCallback((campaign: Campaign | null) => {
    setSelectedCampaign(campaign);
    updateState({ selectedDonor: null, selectedCampaign: campaign }); // Limpar seleção ao trocar campanha
    loadConversations(currentStatus, campaign?.id);
  }, [loadConversations, updateState, currentStatus]);

  // Função para trocar modo de visualização
  const handleViewModeChange = React.useCallback((mode: ViewMode) => {
    updateState({ viewMode: mode });
  }, [updateState]);

  // Função para trocar status de uma conversa específica
  const handleConversationStatusChange = React.useCallback((donorId: string, newStatus: ChatStatus) => {
    console.log(`🔄 Mudando status da conversa ${donorId} para: ${newStatus}`);
    
    // Encontrar o donor atual
    const donor = donors.find(d => d.id === donorId);
    if (!donor) return;

    // Remover da lista atual
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
    
    console.log(`✅ Conversa de ${donor.name} movida para ${statusLabels[newStatus]}`);
    
    // Se mudou para o status atual, recarregar para mostrar na lista
    if (newStatus === currentStatus) {
      setTimeout(() => {
        loadConversations(currentStatus);
      }, 100);
    }
  }, [donors, state.selectedDonor, updateState, currentStatus, loadConversations]);

  // Função para lidar com agendamento
  const handleSchedule = React.useCallback((scheduleData: { type: string; date: string; time: string; notes: string }) => {
    const donor = state.scheduleTarget;
    if (!donor) return;

    console.log(`📅 Agendamento criado para ${donor.name}:`, scheduleData);
    
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

    console.log(`✅ Agendamento confirmado para ${donor.name}`);
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
    console.log('🚀 BloodCenterChat montado - carregando dados...');
    loadConversations();
  }, [loadConversations]);

  // Função para abrir modal e carregar contatos
  const handleOpenNewChatModal = React.useCallback(() => {
    updateState({ showNewChatModal: true });
    loadAllContacts(); // Carrega todos os contatos quando abre o modal
  }, [updateState, loadAllContacts]);

  const filteredAvailableContacts = allContacts.filter((contact) =>
    contact.name.toLowerCase().includes(state.newChatSearch.toLowerCase())
  );

  const handleDonorSelect = React.useCallback((donor: Donor) => {
    console.log('👤 Selecionando donor:', donor.name, 'campanha:', donor.campaignId);
    
    // Carregar mensagens específicas do doador e campanha
    let donorMessages;
    if (donor.campaignId) {
      donorMessages = getMessagesByCampaign(donor.id, donor.campaignId);
    } else {
      donorMessages = getMessagesForDonor(donor.id);
    }
    
    updateState({
      selectedDonor: donor,
      showNewChatModal: false,
      showDonorInfo: false,
      messages: donorMessages,
    });
  }, [updateState]);

  // Função para carregar dados completos do customer e abrir modal
  const handleDonorInfoClick = React.useCallback(async () => {
    if (!state.selectedDonor) return;
    
    try {
      console.log('📋 Carregando dados completos do customer:', state.selectedDonor.id);
      
      // Buscar dados completos do customer na API
      const customerData = await customerApi.getById(state.selectedDonor.id);
      console.log('📋 Dados do customer recebidos:', customerData);
      
      // Atualizar o donor selecionado com os dados completos
      const updatedDonor = customerAdapter.updateDonorWithCustomerData(state.selectedDonor, customerData);
      
      updateState({
        selectedDonor: updatedDonor,
        showDonorInfo: true
      });
    } catch (error) {
      console.error('❌ Erro ao carregar dados do customer:', error);
      // Abrir modal mesmo com erro, mostrando dados que já temos
      updateState({ showDonorInfo: true });
    }
  }, [state.selectedDonor, updateState]);

  const createDonorFromContact = (contactData: NewContactData): Donor => {
    return {
      id: Date.now().toString(),
      name: contactData.name,
      lastMessage: "",
      timestamp: "",
      unread: 0,
      status: "offline",
      bloodType: "N/I",
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
    console.log('📝 Criando novo contato:', contactData.name);
    
    // Se o donor foi criado via API, usar ele diretamente
    if (contactData.donor) {
      // Adicionar à lista de donors (conversas ativas) se tiver lastMessage
      if (contactData.donor.lastMessage) {
        setDonors((prev) => [...prev, contactData.donor!]);
      }
      
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

  const handleContextMenuAction = (action: string, donorId: string) => {
    console.log(`🎯 Ação: ${action} para donor: ${donorId}`);
    
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
        // Abrir modal de informações do doador
        updateState({
          selectedDonor: donor,
          showDonorInfo: true
        });
        break;

      case 'archive-conversation':
        // Mostrar confirmação para arquivar
        updateState({
          showConfirmationModal: true,
          confirmationData: {
            title: 'Arquivar Conversa',
            message: `Tem certeza que deseja arquivar a conversa com ${donor.name}? Esta ação pode ser desfeita posteriormente.`,
            type: 'warning',
            confirmText: 'Arquivar',
            onConfirm: () => {
              // Arquivar conversa (remover da lista atual)
              setDonors(prev => prev.filter(d => d.id !== donorId));
              
              // Se é a conversa selecionada, limpar seleção
              if (state.selectedDonor?.id === donorId) {
                updateState({ selectedDonor: null, messages: [] });
              }
              
              console.log(`📁 Conversa de ${donor.name} arquivada`);
              
              // Fechar modal
              updateState({
                showConfirmationModal: false,
                confirmationData: null
              });
            }
          }
        });
        break;

      case 'block-contact':
        // Mostrar confirmação para bloquear
        updateState({
          showConfirmationModal: true,
          confirmationData: {
            title: 'Bloquear Contato',
            message: `Tem certeza que deseja bloquear ${donor.name}? Esta pessoa não receberá mais mensagens e será movida para a lista de inativos.`,
            type: 'danger',
            confirmText: 'Bloquear',
            onConfirm: () => {
              // Bloquear contato (mover para inativo e remover)
              setDonors(prev => prev.filter(d => d.id !== donorId));
              
              // Se é a conversa selecionada, limpar seleção
              if (state.selectedDonor?.id === donorId) {
                updateState({ selectedDonor: null, messages: [] });
              }
              
              console.log(`🚫 Contato ${donor.name} bloqueado`);
              
              // Fechar modal
              updateState({
                showConfirmationModal: false,
                confirmationData: null
              });
            }
          }
        });
        break;

      default:
        console.warn('Ação não implementada:', action);
    }

    // Fechar menu de contexto
    updateState({
      contextMenu: { show: false, x: 0, y: 0, donorId: "" },
    });
  };

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

  const handleSendMessage = async () => {
    if (
      (!state.messageInput.trim() && state.attachments.length === 0) ||
      !state.selectedDonor ||
      isCreatingConversation
    )
      return;

    // Verificar se é a primeira mensagem de um novo contato
    const isFirstMessage = !state.selectedDonor.hasActiveConversation && !state.selectedDonor.lastMessage;

    if (isFirstMessage) {
      setIsCreatingConversation(true);
      
      try {
        console.log('🚀 Criando conversa para:', state.selectedDonor.name);
        
        // Criar conversa para este cliente usando o adapter
        const createRequest = conversationAdapter.toCreateRequest(
          state.selectedDonor.id,
          'WEB_CHAT'
        );
        
        const newConversation = await conversationApi.create(createRequest);
        console.log('✅ Conversa criada:', newConversation.id);

        // Atualizar o donor para marcar que agora tem conversa ativa
        const updatedDonor = {
          ...state.selectedDonor,
          hasActiveConversation: true,
          lastMessage: state.messageInput.trim() || "Anexo enviado",
          timestamp: getCurrentTimestamp(),
        };

        // Atualizar lista de donors
        setDonors(prev => prev.map(d => 
          d.id === state.selectedDonor?.id ? updatedDonor : d
        ));

        // Atualizar selectedDonor
        updateState({ selectedDonor: updatedDonor });

        console.log('✅ Conversa criada para novo contato:', updatedDonor.name);
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

    const newMessage: Message = {
      id: Date.now().toString(),
      senderId: "ai",
      content: state.messageInput,
      timestamp: getCurrentTimestamp(),
      isAI: true,
      attachments:
        state.attachments.length > 0 ? [...state.attachments] : undefined,
    };

    updateState({
      messages: [...state.messages, newMessage],
      messageInput: "",
      attachments: [],
    });
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

  if (state.showConfiguration) {
    return (
      <ConfigurationPage
        onBack={() => updateState({ showConfiguration: false })}
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
        onRetry={() => loadConversations()}
        onConfigClick={() => updateState({ showConfiguration: true })}
        currentStatus={currentStatus}
        onStatusChange={handleStatusChange}
        campaigns={campaigns}
        selectedCampaign={selectedCampaign}
        onCampaignChange={handleCampaignChange}
        viewMode={state.viewMode}
        onViewModeChange={handleViewModeChange}
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
              messages={state.messages}
              isDragging={state.isDragging}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            />

            <MessageInput
              messageInput={state.messageInput}
              attachments={state.attachments}
              onMessageChange={(value) => updateState({ messageInput: value })}
              onSendMessage={handleSendMessage}
              onFileUpload={handleFileUpload}
              onRemoveAttachment={(id) =>
                updateState({
                  attachments: state.attachments.filter((att) => att.id !== id),
                })
              }
              onKeyPress={handleKeyPress}
              onEnhanceMessage={handleEnhanceMessage}
              isLoading={isCreatingConversation}
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
    </div>
  );
};
