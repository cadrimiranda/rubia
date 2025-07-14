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
import { messageApi } from "../api/services/messageApi";
import { customerAdapter } from "../adapters/customerAdapter";
import { conversationAdapter } from "../adapters/conversationAdapter";
import { getMessagesForDonor } from "../mocks/data";
import { mockCampaigns } from "../mocks/campaigns";
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

  // Carregar conversas da API real
  const loadConversations = React.useCallback(async (status?: ChatStatus) => {
    try {
      setIsLoading(true);
      setError(null);

      const statusToLoad = status || currentStatus;
      
      console.log(`🔄 Carregando conversas para status: ${statusToLoad}...`);

      // Mapear status do frontend para backend
      const backendStatus = conversationAdapter.mapStatusToBackend(statusToLoad);
      
      // Buscar conversas da API
      const response = await conversationApi.getByStatus(backendStatus as any);
      console.log(`📊 API retornou ${response.content.length} conversas`);
      
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
          unread: statusToLoad === 'aguardando' ? 0 : 1,
          status: 'offline' as const,
          bloodType: 'Não informado',
          phone: conv.customerPhone || '',
          email: '',
          lastDonation: 'Sem registro',
          totalDonations: 0,
          address: '',
          birthDate: '',
          weight: 0,
          height: 0,
          hasActiveConversation: true,
          conversationStatus: conv.status,
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(conv.customerName || conv.customerPhone || 'C')}&background=random&size=150`
        };
      });

      console.log(`✅ Carregadas ${conversationsAsDonors.length} conversas para status ${statusToLoad}`);
      setDonors(conversationsAsDonors);
      
    } catch (err) {
      console.error('❌ Erro ao carregar conversas da API:', err);
      setError('Erro ao carregar conversas. Tente novamente.');
      setDonors([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentStatus, conversationAdapter, conversationApi]);

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
    loadConversations(newStatus);
    updateState({ selectedDonor: null }); // Limpar seleção ao trocar status
  }, [loadConversations, updateState]);

  // Função para trocar de campanha
  const handleCampaignChange = React.useCallback((campaign: Campaign | null) => {
    setSelectedCampaign(campaign);
    updateState({ selectedDonor: null, selectedCampaign: campaign }); // Limpar seleção ao trocar campanha
    loadConversations(currentStatus);
  }, [loadConversations, updateState, currentStatus]);

  // Função para trocar modo de visualização
  const handleViewModeChange = React.useCallback((mode: ViewMode) => {
    updateState({ viewMode: mode });
  }, [updateState]);

  // Função para trocar status de uma conversa específica
  const handleConversationStatusChange = React.useCallback(async (donorId: string, newStatus: ChatStatus) => {
    console.log(`🔄 Mudando status da conversa ${donorId} para: ${newStatus}`);
    
    // Encontrar o donor atual
    const donor = donors.find(d => d.id === donorId);
    if (!donor) return;

    // Verificar se tem conversationId
    const conversationId = donor.conversationId || donor.id;
    
    try {
      // Chamar API para mudar status no backend
      const backendStatus = conversationAdapter.mapStatusToBackend(newStatus);
      console.log(`📡 Chamando API para mudar status da conversa ${conversationId} para: ${backendStatus}`);
      
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
      
      console.log(`✅ Conversa de ${donor.name} movida para ${statusLabels[newStatus]} no backend`);
      
      // Se mudou para o status atual, recarregar para mostrar na lista
      if (newStatus === currentStatus) {
        setTimeout(() => {
          loadConversations(currentStatus);
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
    
    // Carregar mensagens (fallback para mock apenas se necessário)
    const donorMessages = getMessagesForDonor(donor.id);
    
    updateState({
      selectedDonor: donor,
      showNewChatModal: false,
      showDonorInfo: false,
      messages: donorMessages,
    });
  }, [updateState]);

  // Função reutilizável para carregar dados completos do customer e abrir modal (DRY)
  const handleOpenDonorProfile = React.useCallback(async (donor: Donor) => {
    try {
      console.log('📋 Carregando dados completos do customer:', donor.id);
      
      // Buscar dados completos do customer na API
      const customerData = await customerApi.getById(donor.id);
      console.log('📋 Dados do customer recebidos:', customerData);
      
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

  const handleContextMenuAction = React.useCallback(async (action: string, donorId: string) => {
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

  const handleSendMessage = async () => {
    console.log('🔍 handleSendMessage called with:', {
      messageInput: state.messageInput,
      messageInputTrimmed: state.messageInput.trim(),
      messageInputLength: state.messageInput.length,
      attachments: state.attachments,
      selectedDonor: state.selectedDonor,
      selectedDonorId: state.selectedDonor?.id,
      conversationId: state.selectedDonor?.conversationId,
      isCreatingConversation
    });

    if (!state.messageInput.trim() && state.attachments.length === 0) {
      console.log('❌ Mensagem vazia - early return');
      return;
    }

    if (!state.selectedDonor) {
      console.log('❌ Nenhum donor selecionado - early return');
      return;
    }

    if (isCreatingConversation) {
      console.log('❌ Já criando conversa - early return');
      return;
    }

    let conversationId = state.selectedDonor.conversationId;
    
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
        conversationId = newConversation.id;

        // Atualizar o donor para marcar que agora tem conversa ativa
        const updatedDonor = {
          ...state.selectedDonor,
          conversationId: newConversation.id,
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

    if (!conversationId) {
      console.error('❌ ID da conversa não encontrado');
      return;
    }

    // Armazenar conteúdo antes de limpar o input
    const messageContent = state.messageInput;

    // Criar mensagem temporária para UI otimista
    const tempMessage: Message = {
      id: `temp-${Date.now()}`,
      senderId: "ai",
      content: messageContent,
      timestamp: getCurrentTimestamp(),
      isAI: true,
      attachments: state.attachments.length > 0 ? [...state.attachments] : undefined,
    };

    // Atualizar UI imediatamente
    updateState({
      messages: [...state.messages, tempMessage],
      messageInput: "",
      attachments: [],
    });

    try {
      console.log('📤 Enviando mensagem para conversa:', conversationId);
      
      // Enviar mensagem para a API
      const sentMessage = await messageApi.send(conversationId, {
        content: messageContent,
        senderId: null, // TODO: Adicionar ID do usuário atual
        messageType: 'TEXT'
      });

      console.log('✅ Mensagem enviada com sucesso:', sentMessage.id);

      // Substituir mensagem temporária pela real
      updateState({
        messages: state.messages.map(msg => 
          msg.id === tempMessage.id 
            ? {
                ...msg,
                id: sentMessage.id,
                timestamp: sentMessage.createdAt || msg.timestamp
              }
            : msg
        )
      });

    } catch (error) {
      console.error('❌ Erro ao enviar mensagem:', error);
      
      // Remover mensagem temporária em caso de erro
      updateState({
        messages: state.messages.filter(msg => msg.id !== tempMessage.id)
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
