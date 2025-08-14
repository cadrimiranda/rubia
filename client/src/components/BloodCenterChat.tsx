import React, { useState, useEffect, useRef } from "react";
import { Heart } from "lucide-react";
import type {
  Donor,
  Message,
  FileAttachment,
  ChatState,
  ViewMode,
  PendingMedia,
} from "../types/types";
import { getCurrentTimestamp } from "../utils";
import { DonorSidebar } from "./DonorSidebar";
import { ChatHeader } from "./ChatHeader";
import { MessageList } from "./MessageList";
import { MessageInput } from "./MessageInput/index";
import { AudioErrorBoundary } from "./AudioErrorBoundary";
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
import { campaignApi } from "../api/services/campaignApi";
import type { ChatStatus } from "../types/index";
import type { Campaign } from "../types/types";
import type { ConversationStatus, MessageDTO } from "../api/types";
import { useWebSocket } from "../hooks/useWebSocket";
import { authService } from "../auth/authService";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";
import { WhatsAppConnectionMonitor } from "./WhatsAppConnectionMonitor";
import { Modal, Alert, Button } from "antd";
import { QrcodeOutlined, ExclamationCircleOutlined } from "@ant-design/icons";
import { whatsappSetupApi } from "../api/services/whatsappSetupApi";
import ZApiActivation from "./ZApiActivation";

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
    viewMode: "full",
  });

  const [showMessageEnhancer, setShowMessageEnhancer] = useState(false);
  const [showDisconnectionModal, setShowDisconnectionModal] = useState(false);
  const [disconnectedInstance, setDisconnectedInstance] = useState<{
    instanceId: string;
    phoneNumber: string;
    error: string;
  } | null>(null);

  const [donors, setDonors] = useState<Donor[]>([]); // Contatos com conversas ativas
  const [allContacts, setAllContacts] = useState<Donor[]>([]); // TODOS os contatos
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingContacts, setIsLoadingContacts] = useState(false);
  const [isCreatingConversation, setIsCreatingConversation] = useState(false);
  const [isAudioSending, setIsAudioSending] = useState(false);
  const [isEnhancing, setIsEnhancing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentStatus, setCurrentStatus] = useState<ChatStatus>("ativos");
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [, setIsCampaignsLoading] = useState(true);
  const [selectedCampaign, setSelectedCampaign] = useState<Campaign | null>(
    null
  );
  const [currentDraftMessage, setCurrentDraftMessage] =
    useState<MessageDTO | null>(null);
  const [lastUserMessage, setLastUserMessage] = useState<string>("");

  // Estados para paginação infinita
  const [hasMorePages, setHasMorePages] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // Refs para controlar paginação e evitar dependências circulares
  const currentPageRef = useRef(0);
  const loadConversationsRef = useRef<
    (reset?: boolean, status?: ChatStatus) => Promise<void>
  >(async () => {});

  // WebSocket para atualizações em tempo real
  const webSocket = useWebSocket();

  // Listener para desconexões de instância WhatsApp
  React.useEffect(() => {
    // Configurar callback para quando uma instância é desconectada
    webSocket.onInstanceDisconnected(
      (instanceId: string, phoneNumber: string, error: string) => {
        // Se modal já está aberto, não fazer nada
        if (showDisconnectionModal) {
          return;
        }

        // Abrir modal de desconexão
        setDisconnectedInstance({ instanceId, phoneNumber, error });
        setShowDisconnectionModal(true);
      }
    );

    // Configurar callback para reconexão (fechar modal)
    webSocket.onInstanceConnected((instanceId: string) => {
      // Fechar modal se estava aberto e instância reconectou
      if (
        showDisconnectionModal &&
        disconnectedInstance?.instanceId === instanceId
      ) {
        setShowDisconnectionModal(false);
        setDisconnectedInstance(null);
      }
    });
  }, [
    webSocket.onInstanceDisconnected,
    webSocket.onInstanceConnected,
    showDisconnectionModal,
    disconnectedInstance,
    webSocket,
  ]);

  // Chat store para mensagens em tempo real
  const { messagesCache, updateUnreadCountBulk } = useChatStore();

  // Combinar mensagens locais (enviadas) com mensagens do WebSocket (recebidas)
  const activeMessages = React.useMemo(() => {
    const conversationId = state.selectedDonor?.conversationId;

    // Mensagens locais (enviadas pelo usuário + carregadas da API inicialmente)
    const localMessages = state.messages;

    // Mensagens do WebSocket (apenas de outros usuários)
    const webSocketMessages =
      conversationId && messagesCache[conversationId]
        ? messagesCache[conversationId].messages
        : [];

    // Combinar e remover duplicatas (apenas por ID exato)
    const allMessages = [...localMessages];
    webSocketMessages.forEach((wsMsg) => {
      if (!allMessages.some((localMsg) => localMsg.id === wsMsg.id)) {
        allMessages.push(wsMsg);

        // Se recebeu uma mensagem de áudio do usuário via WebSocket, desabilitar loading
        if (
          wsMsg.messageType === "audio" &&
          wsMsg.isFromUser &&
          isAudioSending
        ) {
          setIsAudioSending(false);
        }
      }
    });

    // Ordenar por timestamp
    const sortedMessages = allMessages.sort((a, b) => {
      const timeA =
        a.timestamp instanceof Date
          ? a.timestamp.getTime()
          : new Date(a.timestamp).getTime();
      const timeB =
        b.timestamp instanceof Date
          ? b.timestamp.getTime()
          : new Date(b.timestamp).getTime();
      return timeA - timeB;
    });

    return sortedMessages;
  }, [state.selectedDonor, state.messages, messagesCache, isAudioSending]);

  useEffect(() => {
    if (webSocket.isConnected) {
      // WebSocket connected
    } else if (authService.isAuthenticated()) {
      webSocket.connect();
    }
  }, [webSocket, webSocket.isConnected]);

  // Detectar última mensagem do usuário para gerar drafts
  useEffect(() => {
    if (activeMessages.length > 0) {
      // Pegar a última mensagem que veio do usuário
      const userMessages = activeMessages.filter((msg) => msg.isFromUser);
      if (userMessages.length > 0) {
        const lastMessage = userMessages[userMessages.length - 1];
        if (lastMessage.content && lastMessage.content !== lastUserMessage) {
          setLastUserMessage(lastMessage.content);
        }
      }
    }
  }, [activeMessages, lastUserMessage]);

  // Carregar campanhas ativas da API
  const loadCampaigns = React.useCallback(async () => {
    try {
      setIsCampaignsLoading(true);
      const companyId = localStorage.getItem("auth_company_id");
      if (!companyId) {
        return;
      }

      const activeCampaigns = await campaignApi.getActiveCampaigns(companyId);
      setCampaigns(activeCampaigns);
    } catch (error) {
      console.error("❌ Erro ao carregar campanhas:", error);
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
    setState((prev) => ({ ...prev, ...updates }));
  }, []);

  // Atualizar lastMessage dos donors quando novas mensagens chegam via WebSocket
  useEffect(() => {
    // Para cada conversa no messagesCache, atualizar o donor correspondente
    Object.entries(messagesCache).forEach(([conversationId, cache]) => {
      if (cache.messages.length === 0) return;

      // Pegar a mensagem mais recente da conversa
      const lastMessage = cache.messages[cache.messages.length - 1];

      // Usar função callback para evitar dependência direta em donors
      setDonors((prevDonors) => {
        // Encontrar o donor correspondente
        const donorIndex = prevDonors.findIndex(
          (d) => d.conversationId === conversationId
        );

        if (donorIndex >= 0) {
          const currentDonor = prevDonors[donorIndex];

          // Verificar se é uma mensagem nova (diferente da última conhecida)
          const isNewMessage =
            !currentDonor.lastMessage ||
            currentDonor.lastMessage !== lastMessage.content ||
            !currentDonor.timestamp ||
            new Date(lastMessage.timestamp).getTime() >
              new Date(`1970-01-01 ${currentDonor.timestamp}`).getTime();

          if (isNewMessage) {
            const updatedDonor = {
              ...currentDonor,
              lastMessage:
                lastMessage.content ||
                (lastMessage.messageType === "audio"
                  ? "🎵 Mensagem de áudio"
                  : "Anexo enviado"),
              timestamp:
                lastMessage.timestamp instanceof Date
                  ? lastMessage.timestamp.toLocaleTimeString("pt-BR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : new Date(lastMessage.timestamp).toLocaleTimeString(
                      "pt-BR",
                      { hour: "2-digit", minute: "2-digit" }
                    ),
            };

            // Remove the conversation from its current position
            const otherDonors = prevDonors.filter(
              (d) => d.id !== currentDonor.id
            );

            // Place the updated conversation at the beginning of the array
            return [updatedDonor, ...otherDonors];
          }
        }

        // Se não houve mudanças, retornar o array original
        return prevDonors;
      });
    });
  }, [messagesCache]);

  // Atualizar selectedDonor quando o donor correspondente na lista muda
  useEffect(() => {
    if (state.selectedDonor) {
      const updatedDonor = donors.find((d) => d.id === state.selectedDonor!.id);
      if (
        updatedDonor &&
        (updatedDonor.lastMessage !== state.selectedDonor.lastMessage ||
          updatedDonor.timestamp !== state.selectedDonor.timestamp)
      ) {
        updateState({ selectedDonor: updatedDonor });
      }
    }
  }, [donors, state.selectedDonor, updateState]);

  const loadConversations = React.useCallback(
    async (reset = true, status: ChatStatus = "entrada") => {
      try {
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
        const backendStatus = conversationAdapter.mapStatusToBackend(
          status ?? currentStatus
        );

        // Usar endpoint CQRS otimizado com filtro de status
        const response = await conversationApi.getOrderedByLastMessage(
          pageToLoad,
          20,
          backendStatus as ConversationStatus
        );

        const unreadCount: Record<string, number> = {};

        const conversationsAsDonors = response.content.map((conv) => {
          unreadCount[conv.id] = conv.unreadCount ?? 0;

          return {
            id: conv.customerId,
            conversationId: conv.id,
            name: conv.customerName || conv.customerPhone || "Cliente",
            lastMessage: conv.lastMessage?.content || "",
            timestamp: conv.lastMessage?.createdAt
              ? new Date(conv.lastMessage.createdAt).toLocaleTimeString(
                  "pt-BR",
                  {
                    hour: "2-digit",
                    minute: "2-digit",
                  }
                )
              : "",
            status: "offline" as const,
            bloodType: conv.customerBloodType || "Não informado",
            phone: conv.customerPhone || "",
            email: "",
            lastDonation: conv.customerLastDonationDate || "Sem registro",
            totalDonations: 0,
            address: "",
            birthDate: conv.customerBirthDate || "",
            weight: conv.customerWeight || 0,
            height: conv.customerHeight || 0,
            hasActiveConversation: true,
            conversationStatus: conv.status,
            campaignId: conv.campaignId,
            avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(
              conv.customerName || conv.customerPhone || "C"
            )}&background=random&size=150`,
          };
        });

        updateUnreadCountBulk(unreadCount);

        // Atualizar estado da paginação
        currentPageRef.current = pageToLoad;
        const hasMore = pageToLoad + 1 < (response.totalPages || 0);
        setHasMorePages(hasMore);

        if (reset) {
          setDonors(conversationsAsDonors);
        } else {
          setDonors((prevDonors) => [...prevDonors, ...conversationsAsDonors]);
        }
      } catch (err) {
        console.error("❌ Erro ao carregar conversas CQRS:", err);
        setError("Erro ao carregar conversas. Tente novamente.");
        setDonors([]);
      } finally {
        setIsLoading(false);
        setIsLoadingMore(false);
      }
    },
    [currentStatus, updateUnreadCountBulk]
  );

  // Atualizar ref
  loadConversationsRef.current = loadConversations;

  // Função simplificada para evitar dependências circulares
  const callLoadConversations = React.useCallback(
    ({ reset, status }: { reset?: boolean; status?: ChatStatus }) => {
      loadConversationsRef.current?.(reset, status);
    },
    []
  );

  // Função para carregar mais conversas (scroll infinito)
  const loadMoreConversations = React.useCallback(async () => {
    if (!hasMorePages || isLoadingMore) return;
    await loadConversations(false);
  }, [hasMorePages, isLoadingMore, loadConversations]);

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
      } else if (
        customersResponse &&
        customersResponse.content &&
        Array.isArray(customersResponse.content)
      ) {
        // Fallback para formato paginado
        customers = customersResponse.content;
      } else {
        console.warn(
          "⚠️ Resposta da API customers não tem formato conhecido:",
          customersResponse
        );
        setAllContacts([]);
        return;
      }

      // Converter customers para Donors
      const contactsAsDonors = customers.map((customer) => {
        const user = customerAdapter.toUser(customer);
        return {
          id: customer.id,
          name: user.name,
          lastMessage: "", // Contatos não têm lastMessage por padrão
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "Não informado",
          phone: user.phone || "",
          email: "",
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
          ...customer,
        };
      });

      setAllContacts(contactsAsDonors);
    } catch (err) {
      console.error("❌ Erro ao carregar contatos:", err);
    } finally {
      setIsLoadingContacts(false);
    }
  }, []);

  const handleStatusChange = React.useCallback(
    (newStatus: ChatStatus) => {
      setCurrentStatus(newStatus);
      callLoadConversations({ reset: true, status: newStatus });
      updateState({ selectedDonor: null }); // Limpar seleção ao trocar status
      setCurrentDraftMessage(null); // Limpar draft ao trocar status
    },
    [callLoadConversations, updateState]
  );

  const handleCampaignChange = React.useCallback(
    (campaign: Campaign | null) => {
      setSelectedCampaign(campaign);
      updateState({ selectedDonor: null, selectedCampaign: campaign }); // Limpar seleção ao trocar campanha
      callLoadConversations({ reset: true });
    },
    [callLoadConversations, updateState]
  );

  // Função para trocar modo de visualização
  const handleViewModeChange = React.useCallback(
    (mode: ViewMode) => {
      updateState({ viewMode: mode });
    },
    [updateState]
  );

  // Função para trocar status de uma conversa específica
  const handleConversationStatusChange = React.useCallback(
    async (donorId: string, newStatus: ChatStatus) => {
      // Encontrar o donor atual
      const donor = donors.find((d) => d.id === donorId);
      if (!donor) return;

      // Verificar se tem conversationId
      const conversationId = donor.conversationId || donor.id;

      try {
        // Chamar API para mudar status no backend
        const backendStatus = conversationAdapter.mapStatusToBackend(newStatus);

        await conversationApi.changeStatus(
          conversationId,
          backendStatus as any
        );

        // Remover da lista atual após sucesso na API
        setDonors((prev) => prev.filter((d) => d.id !== donorId));

        // Se o donor selecionado foi movido, limpar seleção
        if (state.selectedDonor?.id === donorId) {
          updateState({ selectedDonor: null, messages: [] });
        }

        // Mostrar feedback
      } catch (error) {
        console.error("❌ Erro ao mudar status da conversa:", error);

        // Mostrar modal de erro
        updateState({
          showConfirmationModal: true,
          confirmationData: {
            title: "Erro ao Alterar Status",
            message: `Não foi possível alterar o status da conversa de ${donor.name}. Deseja tentar novamente?`,
            type: "warning",
            confirmText: "Tentar Novamente",
            onConfirm: () => {
              updateState({
                showConfirmationModal: false,
                confirmationData: null,
              });
              // Tentar novamente
              handleConversationStatusChange(donorId, newStatus);
            },
          },
        });
      }
    },
    [donors, state.selectedDonor, updateState]
  );

  // Função para lidar com agendamento
  const handleSchedule = React.useCallback(
    (scheduleData: {
      type: string;
      date: string;
      time: string;
      notes: string;
    }) => {
      const donor = state.scheduleTarget;
      if (!donor) return;

      const typeLabels = {
        doacao: "doação",
        triagem: "triagem médica",
        retorno: "consulta de retorno",
        orientacao: "orientação",
      };

      // Adicionar mensagem de agendamento à conversa
      const agendamentoMessage: Message = {
        id: `schedule_${Date.now()}`,
        senderId: "ai",
        content: `Perfeito! Agendei sua ${
          typeLabels[scheduleData.type as keyof typeof typeLabels] || "doação"
        } para ${new Date(scheduleData.date).toLocaleDateString("pt-BR")} às ${
          scheduleData.time
        }. Confirma presença? 📅${
          scheduleData.notes ? `\n\nObservações: ${scheduleData.notes}` : ""
        }`,
        timestamp: new Date(),
        isFromUser: false,
        messageType: "text",
        status: "sent",
      };

      // Se é a conversa ativa, adicionar a mensagem
      if (state.selectedDonor?.id === donor.id) {
        updateState({
          messages: [...state.messages, agendamentoMessage],
        });
      }

      // Fechar modal
      updateState({
        showScheduleModal: false,
        scheduleTarget: null,
      });
    },
    [state.scheduleTarget, state.selectedDonor, state.messages, updateState]
  );

  // Função para agendamento direto via botão do header
  const handleDirectSchedule = React.useCallback(
    (donorId: string) => {
      const donor =
        donors.find((d) => d.id === donorId) ||
        allContacts.find((c) => c.id === donorId);
      if (donor) {
        updateState({
          showScheduleModal: true,
          scheduleTarget: donor,
        });
      }
    },
    [donors, allContacts, updateState]
  );

  // Carregar dados ao montar componente
  useEffect(() => {
    callLoadConversations({ reset: true, status: "entrada" });
  }, [callLoadConversations]);

  // Cleanup URLs de preview ao desmontar o componente
  useEffect(() => {
    return () => {
      state.pendingMedia.forEach((media) => {
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

  const handleDonorSelect = React.useCallback(
    async (donor: Donor) => {
      // Carregar mensagens da API primeiro
      let donorMessages: Message[] = [];
      let draftMessage: any = null;

      try {
        if (donor.conversationId) {
          const messagesResponse = await messageApi.getByConversation(
            donor.conversationId
          );

          // A API retorna array direto, não objeto com content
          const allMessages = Array.isArray(messagesResponse)
            ? messagesResponse
            : messagesResponse && messagesResponse.content
            ? messagesResponse.content
            : [];

          if (allMessages.length > 0) {
            // Filtrar mensagens por status e buscar DRAFT
            const draftMessages = allMessages.filter(
              (msg) => msg.status === "DRAFT"
            );

            console.log("📝 Drafts encontrados na API:", draftMessages);

            // Se encontrou mensagem DRAFT, pegar a mais recente
            if (draftMessages.length > 0) {
              draftMessage = draftMessages.sort(
                (a, b) =>
                  new Date(b.createdAt || 0).getTime() -
                  new Date(a.createdAt || 0).getTime()
              )[0];
              console.log("📝 Draft mais recente:", draftMessage);
            }

            // Converter todas as mensagens exceto DRAFT para exibição
            const filteredMessages = allMessages.filter(
              (msg) => msg.status !== "DRAFT"
            );

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

            donorMessages = sortedMessages.map(
              (msg): Message => ({
                id: msg.id,
                senderId: msg.senderId || "unknown",
                content: msg.content,
                timestamp: msg.createdAt ? new Date(msg.createdAt) : new Date(),
                isFromUser: msg.senderType !== "CUSTOMER", // CUSTOMER = false (recebida do cliente), !CUSTOMER = true (enviada por mim/sistema)
                messageType:
                  (msg.messageType?.toLowerCase() as
                    | "text"
                    | "image"
                    | "file"
                    | "audio") || "text",
                status: "delivered",
                mediaUrl: msg.mediaUrl,
                mimeType: msg.mimeType,
                audioDuration: msg.audioDuration,
                // Only create attachments for non-audio media
                attachments:
                  msg.mediaUrl && msg.messageType !== "AUDIO"
                    ? [
                        {
                          id: `media_${msg.id}`,
                          name: msg.mediaUrl.split("/").pop() || "arquivo",
                          size: 0,
                          type: "application/octet-stream",
                          url: msg.mediaUrl,
                        },
                      ]
                    : undefined,
              })
            );
          }
        }
      } catch (error) {
        console.error("❌ Erro ao carregar mensagens da API:", error);
      }

      // Preservar mensagens temporárias (otimistas) mesmo em conversas diferentes
      const tempMessages = state.messages.filter((msg) =>
        msg.id.startsWith("temp-")
      );
      const isReloadingSameConversation =
        state.selectedDonor?.conversationId === donor.conversationId;

      let messagesToUse: Message[];
      if (isReloadingSameConversation) {
        // Mesma conversa: manter todas as mensagens locais - MAS ORDENAR ELAS!

        // Ordenar mensagens locais por timestamp (que é string HH:MM)
        const sortedLocalMessages = [...state.messages].sort((a, b) => {
          // Função para converter HH:MM em minutos
          const parseTime = (timeStr: string) => {
            if (!timeStr) return 0;
            const [hours, minutes] = timeStr.split(":").map(Number);
            return hours * 60 + minutes;
          };

          // Para mensagens temporárias, usar timestamp atual
          if (a.id.startsWith("temp-") && b.id.startsWith("temp-")) {
            const timeA =
              a.timestamp instanceof Date
                ? a.timestamp.getTime()
                : parseTime(a.timestamp);
            const timeB =
              b.timestamp instanceof Date
                ? b.timestamp.getTime()
                : parseTime(b.timestamp);
            return timeA - timeB;
          }
          // Se uma é temporária, ela vai por último (mais recente)
          if (a.id.startsWith("temp-")) return 1;
          if (b.id.startsWith("temp-")) return -1;
          // Para mensagens reais, usar timestamp
          const timeA =
            a.timestamp instanceof Date
              ? a.timestamp.getTime()
              : parseTime(a.timestamp);
          const timeB =
            b.timestamp instanceof Date
              ? b.timestamp.getTime()
              : parseTime(b.timestamp);
          return timeA - timeB;
        });

        messagesToUse = sortedLocalMessages;
      } else {
        // Conversa diferente: usar mensagens da API + preservar temporárias da conversa atual
        const relevantTempMessages = tempMessages.filter(
          (msg) =>
            // Verificar se a mensagem temporária é da conversa que estamos carregando
            donor.conversationId && msg.senderId // Se tem conversationId e senderId
        );
        messagesToUse = [...donorMessages, ...relevantTempMessages];
      }

      // FORCE FINAL SORT - Garantir ordem cronológica final
      const parseTimeToMinutes = (timeStr: string) => {
        if (!timeStr) return 0;
        const [hours, minutes] = timeStr.split(":").map(Number);
        return hours * 60 + minutes;
      };

      // Ordenar mensagens cronologicamente (mais antigas primeiro)
      messagesToUse.sort((a, b) => {
        const timeA =
          a.timestamp instanceof Date
            ? a.timestamp.getTime()
            : parseTimeToMinutes(a.timestamp);
        const timeB =
          b.timestamp instanceof Date
            ? b.timestamp.getTime()
            : parseTimeToMinutes(b.timestamp);
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
        if (
          !store.messagesCache[donor.conversationId] &&
          donorMessages.length > 0
        ) {
          store.messagesCache[donor.conversationId] = {
            messages: donorMessages.map((msg) => ({
              id: msg.id,
              content: msg.content,
              senderId: msg.senderId,
              timestamp: msg.timestamp,
              isFromUser: !msg.isAI,
              messageType: msg.messageType || "text",
              status: "delivered",
              attachments: msg.attachments,
              media: msg.media,
            })),
            page: 0,
            hasMore: false,
          };
        }
      }

      // Armazenar referência da mensagem DRAFT para o MessageInput
      setCurrentDraftMessage(draftMessage);
    },
    [state.messages, state.selectedDonor, updateState]
  );

  // Função reutilizável para carregar dados completos do customer e abrir modal (DRY)
  const handleOpenDonorProfile = React.useCallback(
    async (donor: Donor) => {
      try {
        // Buscar dados completos do customer na API
        const customerData = await customerApi.getById(donor.id);

        // Atualizar o donor com os dados completos
        const updatedDonor = customerAdapter.updateDonorWithCustomerData(
          donor,
          customerData
        );

        updateState({
          selectedDonor: updatedDonor,
          showDonorInfo: true,
        });
      } catch (error) {
        console.error("❌ Erro ao carregar dados do customer:", error);
        // Abrir modal mesmo com erro, mostrando dados que já temos
        updateState({
          selectedDonor: donor,
          showDonorInfo: true,
        });
      }
    },
    [updateState]
  );

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
      conversationId: "",
      ...contactData.donor,
    };
  };

  const handleNewContactCreate = React.useCallback(
    async (contactData: NewContactData) => {
      // Se o donor foi criado via API, usar ele diretamente
      if (contactData.donor) {
        // Adicionar à lista de donors (conversas ativas) sempre que tiver uma conversa criada
        // Mesmo sem lastMessage, para que apareça na sidebar imediatamente
        setDonors((prev) => {
          const exists = prev.some((d) => d.id === contactData.donor!.id);
          if (exists) return prev;
          return [...prev, contactData.donor!];
        });

        // Sempre adicionar à lista de todos os contatos
        setAllContacts((prev) => {
          // Verificar se já existe para evitar duplicatas
          const exists = prev.some(
            (contact) => contact.id === contactData.donor!.id
          );
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
    },
    [handleDonorSelect]
  );

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

  const handleContextMenuAction = React.useCallback(
    async (action: string, donorId: string) => {
      // Encontrar o donor
      const donor =
        donors.find((d) => d.id === donorId) ||
        allContacts.find((c) => c.id === donorId);
      if (!donor) {
        console.error("Donor não encontrado:", donorId);
        updateState({
          contextMenu: { show: false, x: 0, y: 0, donorId: "" },
        });
        return;
      }

      switch (action) {
        case "view-conversation":
          // Selecionar a conversa (igual ao clique normal)
          handleDonorSelect(donor);
          break;

        case "schedule-donation":
          // Abrir modal de agendamento
          updateState({
            showScheduleModal: true,
            scheduleTarget: donor,
          });
          break;

        case "view-profile":
          // Reutiliza a mesma função do ChatHeader (DRY)
          await handleOpenDonorProfile(donor);
          break;

        default:
          console.warn("Ação não implementada:", action);
      }

      // Fechar menu de contexto
      updateState({
        contextMenu: { show: false, x: 0, y: 0, donorId: "" },
      });
    },
    [
      donors,
      allContacts,
      handleDonorSelect,
      handleOpenDonorProfile,
      updateState,
    ]
  );

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

  const handleEnhanceMessage = async () => {
    if (!state.messageInput.trim()) {
      return;
    }

    const { user } = useAuthStore.getState();
    if (!user?.companyId) {
      console.error("❌ Company ID not found for message enhancement");
      return;
    }

    const originalMessage = state.messageInput;

    try {
      console.log("🔮 [AI] Starting message enhancement...");

      setIsEnhancing(true);

      const { aiAgentApi } = await import("../api/services/aiAgentApi");
      const conversationId = state.selectedDonor?.conversationId;
      const enhancedMessage = await aiAgentApi.enhanceMessage(
        user.companyId,
        originalMessage,
        conversationId
      );

      console.log("✅ [AI] Message enhanced successfully");

      // Update the message input with the enhanced version
      updateState({ messageInput: enhancedMessage });
    } catch (error) {
      console.error("❌ Error enhancing message:", error);

      // Restore original message
      updateState({ messageInput: originalMessage });

      // Show error modal
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: "Erro na Melhoria de Mensagem",
          message:
            "Não foi possível melhorar a mensagem com IA. Verifique se você tem um agente IA configurado e ativo.",
          type: "warning",
          confirmText: "OK",
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
            });
          },
        },
      });
    } finally {
      // Sempre desativar loading ao final
      setIsEnhancing(false);
    }
  };

  const handleApplyEnhancedMessage = (enhancedMessage: string) => {
    updateState({ messageInput: enhancedMessage });
    setShowMessageEnhancer(false);
  };

  const handleDeleteDraft = async () => {
    if (!currentDraftMessage) {
      return;
    }

    console.log("🗑️ Excluindo draft:", currentDraftMessage.id);

    try {
      await messageApi.delete(currentDraftMessage.id);
      console.log("✅ Draft excluído com sucesso!");

      // Limpar draft do estado local
      setCurrentDraftMessage(null);
      updateState({ messageInput: "" });

      // Feedback de sucesso (opcional)
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: "Draft Excluído",
          message: "A mensagem draft foi excluída com sucesso.",
          type: "info",
          confirmText: "OK",
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
            });
          },
        },
      });
    } catch (error) {
      console.error("❌ Erro ao excluir draft:", error);

      // Mostrar erro para o usuário
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: "Erro ao Excluir Draft",
          message:
            "Não foi possível excluir a mensagem draft. Tente novamente.",
          type: "warning",
          confirmText: "OK",
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
            });
          },
        },
      });
    }
  };

  const handleMediaSelected = (file: File) => {
    // Validar arquivo
    const validation = mediaApi.validateFile(file);
    if (!validation.valid) {
      handleMediaError(validation.error || "Arquivo inválido");
      return;
    }

    // Determinar tipo de mídia
    let mediaType: PendingMedia["mediaType"] = "OTHER";
    if (file.type.startsWith("image/")) mediaType = "IMAGE";
    else if (file.type.startsWith("video/")) mediaType = "VIDEO";
    else if (file.type.startsWith("audio/")) mediaType = "AUDIO";
    else if (
      file.type.includes("pdf") ||
      file.type.includes("document") ||
      file.type.includes("text")
    )
      mediaType = "DOCUMENT";

    // Criar preview local
    const previewUrl = file.type.startsWith("image/")
      ? URL.createObjectURL(file)
      : undefined;

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
    const mediaToRemove = state.pendingMedia.find((m) => m.id === mediaId);
    if (mediaToRemove?.previewUrl) {
      URL.revokeObjectURL(mediaToRemove.previewUrl);
    }

    updateState({
      pendingMedia: state.pendingMedia.filter((m) => m.id !== mediaId),
    });
  };

  const handleMediaError = (error: string) => {
    updateState({
      showConfirmationModal: true,
      confirmationData: {
        title: "Erro no Upload",
        message: error,
        type: "warning",
        confirmText: "OK",
        onConfirm: () => {
          updateState({
            showConfirmationModal: false,
            confirmationData: null,
          });
        },
      },
    });
  };

  const handleAudioRecorded = async (audioBlob: Blob) => {
    if (!state.selectedDonor) {
      handleMediaError("Selecione um doador para enviar áudio");
      return;
    }

    if (isCreatingConversation) {
      handleMediaError("Aguarde a criação da conversa");
      return;
    }

    if (isAudioSending) {
      handleMediaError("Aguarde o envio do áudio anterior");
      return;
    }

    setIsAudioSending(true);
    let conversationId = state.selectedDonor.conversationId;

    // Verificar se é a primeira mensagem de um novo contato
    const isFirstMessage =
      !state.selectedDonor.hasActiveConversation &&
      !state.selectedDonor.lastMessage;

    if (isFirstMessage) {
      setIsCreatingConversation(true);

      try {
        // Criar conversa para este cliente usando o adapter
        const createRequest = conversationAdapter.toCreateRequest(
          state.selectedDonor.id,
          "WHATSAPP"
        );

        const newConversation = await conversationApi.create(createRequest);
        conversationId = newConversation.id;

        // Atualizar o donor para marcar que agora tem conversa ativa
        const updatedDonor = {
          ...state.selectedDonor,
          conversationId: newConversation.id,
          hasActiveConversation: true,
        };

        // Atualizar lista de donors
        setDonors((prev) => {
          const existingDonorIndex = prev.findIndex(
            (d) => d.id === state.selectedDonor?.id
          );

          if (existingDonorIndex >= 0) {
            return prev.map((d) =>
              d.id === state.selectedDonor?.id ? updatedDonor : d
            );
          } else {
            return [...prev, updatedDonor];
          }
        });

        // Atualizar selectedDonor
        updateState({ selectedDonor: updatedDonor });
      } catch (error) {
        console.error("❌ Erro ao criar conversa:", error);
        handleMediaError("Não foi possível criar a conversa. Tente novamente.");
        setIsCreatingConversation(false);
        setIsAudioSending(false);
        return;
      } finally {
        setIsCreatingConversation(false);
      }
    }

    if (!conversationId) {
      handleMediaError("ID da conversa não encontrado");
      setIsAudioSending(false);
      return;
    }

    try {
      // Criar arquivo a partir do blob
      const audioFile = new File([audioBlob], `audio-${Date.now()}.wav`, {
        type: audioBlob.type,
      });

      const donorPhone = state.selectedDonor.phone;
      if (!donorPhone) {
        throw new Error("Número de telefone do doador não encontrado");
      }

      // Enviar áudio via Z-API e aguardar WebSocket
      await mediaApi.uploadForZApi(
        audioFile,
        donorPhone,
        undefined // sem texto para áudio
      );

      // Não fazer nada aqui - aguardar mensagem chegar via WebSocket
    } catch (error) {
      console.error("❌ Erro ao enviar áudio:", error);
      handleMediaError("Não foi possível enviar o áudio. Tente novamente.");
      setIsAudioSending(false);
    }
  };

  const handleSendMessage = async () => {
    if (
      !state.messageInput.trim() &&
      state.attachments.length === 0 &&
      state.pendingMedia.length === 0
    ) {
      return;
    }

    if (!state.selectedDonor) {
      return;
    }

    if (isCreatingConversation) {
      return;
    }

    // Armazenar conteúdo antes de qualquer processamento
    const messageContent = state.messageInput;

    let conversationId = state.selectedDonor.conversationId;

    // Verificar se é a primeira mensagem de um novo contato
    const isFirstMessage =
      !state.selectedDonor.hasActiveConversation &&
      !state.selectedDonor.lastMessage;

    if (isFirstMessage) {
      setIsCreatingConversation(true);

      try {
        // Criar conversa para este cliente usando o adapter
        const createRequest = conversationAdapter.toCreateRequest(
          state.selectedDonor.id,
          "WHATSAPP"
        );

        const newConversation = await conversationApi.create(createRequest);
        conversationId = newConversation.id;

        // Atualizar o donor para marcar que agora tem conversa ativa
        const updatedDonor = {
          ...state.selectedDonor,
          conversationId: newConversation.id,
          hasActiveConversation: true,
          lastMessage: messageContent.trim() || "Anexo enviado", // Usar messageContent armazenado
          timestamp: new Date().toLocaleTimeString("pt-BR", {
            hour: "2-digit",
            minute: "2-digit",
          }),
        };

        // Atualizar lista de donors
        setDonors((prev) => {
          const existingDonorIndex = prev.findIndex(
            (d) => d.id === state.selectedDonor?.id
          );
          let updated;

          if (existingDonorIndex >= 0) {
            // Doador existe, atualizar
            updated = prev.map((d) =>
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
        console.error("❌ Erro ao criar conversa:", error);

        // Mostrar feedback de erro ao usuário
        updateState({
          showConfirmationModal: true,
          confirmationData: {
            title: "Erro ao Criar Conversa",
            message:
              "Não foi possível criar a conversa. Deseja tentar novamente?",
            type: "warning",
            confirmText: "Tentar Novamente",
            onConfirm: () => {
              updateState({
                showConfirmationModal: false,
                confirmationData: null,
              });
              // Tentar novamente após fechar o modal
              setTimeout(() => handleSendMessage(), 100);
            },
          },
        });

        setIsCreatingConversation(false);
        return; // Não enviar a mensagem se falhou ao criar conversa
      } finally {
        setIsCreatingConversation(false);
      }
    }

    if (!conversationId) {
      console.error("❌ ID da conversa não encontrado");
      return;
    }

    // Conteúdo já foi armazenado no início da função

    // Se há mensagem DRAFT, atualizar status para SENT
    if (currentDraftMessage) {
      try {
        await messageApi.updateMessageStatus(currentDraftMessage.id, "SENT");
        setCurrentDraftMessage(null); // Limpar draft após envio
      } catch (error) {
        console.error("❌ Erro ao atualizar status da mensagem DRAFT:", error);
      }
    }

    // Criar mensagem temporária para UI otimista
    const currentUser = useAuthStore.getState().user;
    const tempMessage: Message = {
      id: `temp-${Date.now()}`,
      senderId: currentUser?.id || "user",
      content: messageContent,
      timestamp: new Date(),
      isFromUser: true,
      messageType: state.pendingMedia.length > 0 ? "file" : "text",
      status: "sending",
      attachments:
        state.attachments.length > 0 ? [...state.attachments] : undefined,
      // Converter pendingMedia para um formato de preview
      media:
        state.pendingMedia.length > 0
          ? state.pendingMedia.map((pm) => ({
              id: pm.id,
              conversationId: conversationId || "",
              fileUrl: pm.previewUrl || "uploading...",
              mediaType: pm.mediaType,
              mimeType: pm.mimeType,
              originalFileName: pm.originalFileName,
              fileSizeBytes: pm.fileSizeBytes,
              uploadedAt: new Date().toISOString(),
            }))
          : undefined,
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
          throw new Error("Número de telefone do doador não encontrado");
        }

        try {
          // Usar upload direto para Z-API
          zapiResult = await mediaApi.uploadForZApi(
            firstPendingMedia.file,
            donorPhone,
            messageContent || undefined
          );
        } catch (uploadError) {
          console.error("❌ Erro no upload de mídia Z-API:", uploadError);
          throw new Error("Falha no upload da mídia via Z-API");
        }
      }

      // Se teve upload de mídia via Z-API, a mensagem já foi enviada
      if (zapiResult) {
        // Atualizar UI para mostrar sucesso usando setState callback
        setState((currentState) => {
          return {
            ...currentState,
            messages: currentState.messages.map((msg) =>
              msg.id === tempMessage.id
                ? {
                    ...msg,
                    id: zapiResult.messageId,
                    timestamp: new Date(),
                  }
                : msg
            ),
          };
        });

        // Atualizar lastMessage do doador na sidebar para mensagens com mídia
        if (state.selectedDonor) {
          const updatedDonor = {
            ...state.selectedDonor,
            lastMessage: messageContent || "Anexo enviado",
            timestamp: new Date().toLocaleTimeString("pt-BR", {
              hour: "2-digit",
              minute: "2-digit",
            }),
          };

          setDonors((prev) => {
            const updated = prev.map((d) =>
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
        messageType: "TEXT",
      });

      // Substituir mensagem temporária pela real - usando setState callback para ter estado atualizado
      setState((currentState) => {
        const tempExists = currentState.messages.some(
          (msg) => msg.id === tempMessage.id
        );

        if (!tempExists) {
          return {
            ...currentState,
            messages: [
              ...currentState.messages.map((msg) => ({
                ...msg,
                timestamp:
                  msg.timestamp instanceof Date
                    ? msg.timestamp
                    : new Date(msg.timestamp),
              })),
              {
                id: sentMessage.id,
                senderId: tempMessage.senderId,
                content: tempMessage.content,
                timestamp: sentMessage.createdAt
                  ? new Date(sentMessage.createdAt)
                  : tempMessage.timestamp,
                isFromUser: tempMessage.isFromUser,
                messageType: tempMessage.messageType,
                status: "sent",
                attachments: tempMessage.attachments,
                media: tempMessage.media,
              } as Message,
            ],
          };
        }

        return {
          ...currentState,
          messages: currentState.messages.map((msg) =>
            msg.id === tempMessage.id
              ? {
                  ...msg,
                  id: sentMessage.id,
                  timestamp: sentMessage.createdAt
                    ? new Date(sentMessage.createdAt)
                    : msg.timestamp instanceof Date
                    ? msg.timestamp
                    : new Date(msg.timestamp),
                }
              : {
                  ...msg,
                  timestamp:
                    msg.timestamp instanceof Date
                      ? msg.timestamp
                      : new Date(msg.timestamp),
                }
          ),
        };
      });

      // Atualizar lastMessage do doador na sidebar para mensagens não iniciais
      if (state.selectedDonor && !isFirstMessage) {
        const updatedDonor = {
          ...state.selectedDonor,
          lastMessage: messageContent,
          timestamp: sentMessage.createdAt
            ? new Date(sentMessage.createdAt).toLocaleTimeString("pt-BR", {
                hour: "2-digit",
                minute: "2-digit",
              })
            : getCurrentTimestamp(),
        };

        setDonors((prev) => {
          const updated = prev.map((d) =>
            d.id === state.selectedDonor?.id ? updatedDonor : d
          );
          return updated;
        });

        updateState({ selectedDonor: updatedDonor });
      }
    } catch (error) {
      console.error("❌ Erro ao enviar mensagem:", error);

      // Limpar URLs de preview em caso de erro
      mediaToUpload.forEach((media) => {
        if (media.previewUrl) {
          URL.revokeObjectURL(media.previewUrl);
        }
      });

      // Remover mensagem temporária em caso de erro
      updateState({
        messages: state.messages.filter((msg) => msg.id !== tempMessage.id),
        // Restaurar pendingMedia em caso de erro para o usuário tentar novamente
        pendingMedia: mediaToUpload,
      });

      // Mostrar feedback de erro
      updateState({
        showConfirmationModal: true,
        confirmationData: {
          title: "Erro ao Enviar Mensagem",
          message:
            "Não foi possível enviar a mensagem. Deseja tentar novamente?",
          type: "warning",
          confirmText: "Tentar Novamente",
          onConfirm: () => {
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
              messageInput: messageContent,
            });
          },
        },
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
      callLoadConversations({ reset: true });
      loadCampaigns();
      setWasInConfiguration(false);
    }
  }, [
    state.showConfiguration,
    callLoadConversations,
    loadCampaigns,
    wasInConfiguration,
  ]);

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
        onRetry={() => callLoadConversations({ reset: true })}
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
        onClose={() =>
          updateState({ showScheduleModal: false, scheduleTarget: null })
        }
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
          onCancel={() =>
            updateState({
              showConfirmationModal: false,
              confirmationData: null,
            })
          }
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
              agentAvatar={undefined} // TODO: Buscar avatar do agente IA da empresa
            />

            <AudioErrorBoundary>
              <MessageInput
                messageInput={state.messageInput}
                attachments={state.attachments}
                pendingMedia={state.pendingMedia}
                conversationId={state.selectedDonor?.conversationId}
                draftMessage={currentDraftMessage}
                onMessageChange={(value) =>
                  updateState({ messageInput: value })
                }
                onSendMessage={handleSendMessage}
                onFileUpload={handleFileUpload}
                onRemoveAttachment={(id) =>
                  updateState({
                    attachments: state.attachments.filter(
                      (att) => att.id !== id
                    ),
                  })
                }
                onMediaSelected={handleMediaSelected}
                onRemovePendingMedia={handleRemovePendingMedia}
                onKeyPress={handleKeyPress}
                onEnhanceMessage={handleEnhanceMessage}
                onError={handleMediaError}
                onAudioRecorded={handleAudioRecorded}
                onDeleteDraft={handleDeleteDraft}
                isAudioSending={isAudioSending}
                isEnhancing={isEnhancing}
                maxRecordingTimeMs={300000}
                maxFileSizeMB={16}
              />
            </AudioErrorBoundary>
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
      {/* Modal de desconexão WhatsApp */}
      <Modal
        title="WhatsApp Desconectado"
        open={showDisconnectionModal}
        onCancel={() => setShowDisconnectionModal(false)}
        footer={[
          <Button key="close" onClick={() => setShowDisconnectionModal(false)}>
            Fechar
          </Button>,
          <Button
            key="reconnect"
            type="primary"
            icon={<QrcodeOutlined />}
            onClick={() => {
              // Manter modal aberto para mostrar QR code
            }}
          >
            Reconectar
          </Button>,
        ]}
        width={800}
        maskClosable={false}
      >
        {disconnectedInstance && (
          <div className="space-y-4">
            <Alert
              type="error"
              icon={<ExclamationCircleOutlined />}
              message="Conexão WhatsApp Perdida"
              description={
                <div>
                  <p>
                    A instância{" "}
                    <strong>
                      {whatsappSetupApi.formatPhoneNumber(
                        disconnectedInstance.phoneNumber
                      )}
                    </strong>{" "}
                    foi desconectada.
                  </p>
                  <p className="mt-2">
                    <strong>Motivo:</strong> {disconnectedInstance.error}
                  </p>
                  <p className="mt-2">
                    Para continuar enviando e recebendo mensagens, é necessário
                    reconectar.
                  </p>
                </div>
              }
              showIcon
            />

            <div className="bg-gray-50 p-4 rounded-lg">
              <h4 className="font-medium mb-2">Para reconectar:</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-gray-600">
                <li>Clique em "Reconectar" acima</li>
                <li>
                  Abra o WhatsApp no seu celular (
                  {whatsappSetupApi.formatPhoneNumber(
                    disconnectedInstance.phoneNumber
                  )}
                  )
                </li>
                <li>Vá em Configurações → Aparelhos conectados</li>
                <li>Escaneie o QR Code que aparecerá abaixo</li>
              </ol>
            </div>

            {/* Componente QR Code */}
            <ZApiActivation />
          </div>
        )}
      </Modal>
    </div>
  );
};
