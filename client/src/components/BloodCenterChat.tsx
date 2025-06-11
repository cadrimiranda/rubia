import React, { useState, useEffect } from "react";
import { Heart } from "lucide-react";
import type { Donor, Message, FileAttachment, ChatState } from "../types/types";
import { getCurrentTimestamp } from "../utils";
import { DonorSidebar } from "./DonorSidebar";
import { ChatHeader } from "./ChatHeader";
import { MessageList } from "./MessageList";
import { MessageInput } from "./MessageInput";
import { ContextMenu as ContextMenuComponent } from "./ContextMenu";
import { NewChatModal } from "./NewChatModal";
import { DonorInfoModal } from "./DonorInfoModal";
import { conversationApi } from "../api/services/conversationApi";
import { customerApi } from "../api/services/customerApi";
import { customerAdapter } from "../adapters/customerAdapter";
import type { ConversationDTO } from "../api/types";

interface NewContactData {
  name: string;
  phone: string;
  donor?: Donor;
}

export const BloodCenterChat: React.FC = () => {
  const [state, setState] = useState<ChatState>({
    selectedDonor: null,
    messages: [],
    attachments: [],
    searchTerm: "",
    messageInput: "",
    showNewChatModal: false,
    showDonorInfo: false,
    newChatSearch: "",
    isDragging: false,
    contextMenu: { show: false, x: 0, y: 0, donorId: "" },
  });

  const [donors, setDonors] = useState<Donor[]>([]); // Contatos com conversas ativas
  const [allContacts, setAllContacts] = useState<Donor[]>([]); // TODOS os contatos
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingContacts, setIsLoadingContacts] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateState = React.useCallback((updates: Partial<ChatState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  }, []);

  // Converter ConversationDTO em Donor
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

  // Carregar conversas da API
  const loadConversations = React.useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);

      console.log('🔄 Carregando conversas...');

      // Buscar conversas de todas as categorias
      const [entradaResponse, esperandoResponse, finalizadosResponse] = await Promise.all([
        conversationApi.getByStatus('ENTRADA', 0, 50),
        conversationApi.getByStatus('ESPERANDO', 0, 50),
        conversationApi.getByStatus('FINALIZADOS', 0, 50)
      ]);

      // Combinar todas as conversas
      const allConversations = [
        ...entradaResponse.content,
        ...esperandoResponse.content,
        ...finalizadosResponse.content
      ];

      console.log(`✅ Carregadas ${allConversations.length} conversas`);

      // Converter para Donors
      const donorsFromConversations = allConversations.map(convertConversationToDonor);

      setDonors(donorsFromConversations);
    } catch (err) {
      console.error('❌ Erro ao carregar conversas:', err);
      setError('Erro ao carregar conversas. Tente novamente.');
    } finally {
      setIsLoading(false);
    }
  }, [convertConversationToDonor]);

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
    console.log('👤 Selecionando donor:', donor.name);
    updateState({
      selectedDonor: donor,
      showNewChatModal: false,
      showDonorInfo: false,
      messages: [], // Por enquanto sempre vazio até implementar busca de mensagens
    });
  }, [updateState]);

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
    console.log(`Action: ${action} for donor: ${donorId}`);
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

  const handleSendMessage = async () => {
    if (
      (!state.messageInput.trim() && state.attachments.length === 0) ||
      !state.selectedDonor
    )
      return;

    // Verificar se é a primeira mensagem de um novo contato
    const isFirstMessage = !state.selectedDonor.hasActiveConversation && !state.selectedDonor.lastMessage;

    if (isFirstMessage) {
      try {
        // Criar conversa para este cliente
        await conversationApi.create({
          customerId: state.selectedDonor.id,
          channel: 'WEB'
        });

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
        // Continuar enviando a mensagem mesmo se falhar
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
  }, [state.contextMenu.show]);

  return (
    <div className="flex h-screen bg-white">
      <ContextMenuComponent
        contextMenu={state.contextMenu}
        onAction={handleContextMenuAction}
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
        onRetry={loadConversations}
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

      <div className="flex-1 flex flex-col">
        {state.selectedDonor ? (
          <>
            <ChatHeader
              donor={state.selectedDonor}
              onDonorInfoClick={() => updateState({ showDonorInfo: true })}
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
