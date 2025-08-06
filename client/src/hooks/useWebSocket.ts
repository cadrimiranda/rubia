import { useCallback, useEffect, useRef, useState } from "react";
import { Client, type IFrame, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";
import { authService } from "../auth/authService";
import type { Message } from "../types";

interface WebSocketMessage {
  type: string;
  message?: Message;
  conversation?: any; // Using any for now until ConversationDTO is available
  conversationId?: string;
  userName?: string;
  isTyping?: boolean;
  // WhatsApp instance status fields
  instanceId?: string;
  status?: string;
  phoneNumber?: string;
  displayName?: string;
  statusData?: {
    connected: boolean;
    error?: string;
    justConnected?: boolean;
    moment?: number;
  };
}

interface UseWebSocketReturn {
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
  sendTyping: (conversationId: string) => void;
  stopTyping: (conversationId: string) => void;
  onInstanceConnected: (callback: (instanceId: string, phoneNumber: string) => void) => void;
  onInstanceDisconnected: (callback: (instanceId: string, phoneNumber: string, error: string) => void) => void;
}

export const useWebSocket = (): UseWebSocketReturn => {
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<boolean>(false);
  const instanceConnectedCallbackRef = useRef<((instanceId: string, phoneNumber: string) => void) | null>(null);
  const instanceDisconnectedCallbackRef = useRef<((instanceId: string, phoneNumber: string, error: string) => void) | null>(null);
  const { user } = useAuthStore();
  const accessToken = authService.getAccessToken();
  const {
    addMessage,
    updateConversation,
    setUserTyping,
  } = useChatStore();

  const handleNewMessage = useCallback(
    (message: WebSocketMessage) => {
      console.log("WebSocket message received:", message);

      if (
        message.type === "NEW_MESSAGE" &&
        message.message &&
        message.conversation
      ) {
        const messageData = message.message;
        const conversationId = message.conversation.id;

        // Verificar se a conversa existe na lista atual
        const { chats, refreshConversations } = useChatStore.getState();
        const conversationExists = chats.some(
          (chat) => chat.id === conversationId
        );

        // Adicionar mensagem ao cache
        addMessage(conversationId, messageData);

        if (conversationExists) {
          // Se a conversa existe, apenas atualizar
          updateConversation(conversationId, message.conversation);
        } else {
          // Se a conversa não existe, recarregar lista para mostrar em tempo real
          console.log(
            "Conversa não encontrada na lista atual, recarregando...",
            conversationId
          );
          refreshConversations();
        }
      }
    },
    [addMessage, updateConversation, user?.id]
  );

  const handleConversationUpdate = useCallback(
    (message: WebSocketMessage) => {
      if (message.type === "CONVERSATION_UPDATE" && message.conversation) {
        updateConversation(message.conversation.id, message.conversation);
      }
    },
    [updateConversation]
  );

  const handleTypingStatus = useCallback(
    (message: WebSocketMessage) => {
      if (
        message.type === "TYPING_STATUS" &&
        message.conversationId &&
        message.userName
      ) {
        setUserTyping(
          message.conversationId,
          "user",
          message.userName,
          message.isTyping || false
        );
      }
    },
    []
  );

  const handleInstanceStatusChange = useCallback(
    (message: WebSocketMessage) => {
      console.log("🔌 WebSocket message received - type:", message.type);
      
      if (message.type === "INSTANCE_STATUS_CHANGE") {
        console.log("📱 Instance status change received:", {
          instanceId: message.instanceId,
          status: message.status,
          phoneNumber: message.phoneNumber,
          statusData: message.statusData,
          fullMessage: message
        });
        
        // Check if instance just connected and trigger callback
        if (message.statusData?.justConnected && message.instanceId && message.phoneNumber) {
          console.log("✅ Instance just connected, triggering callback:", message.instanceId);
          if (instanceConnectedCallbackRef.current) {
            instanceConnectedCallbackRef.current(message.instanceId, message.phoneNumber);
          }
        } else if (message.statusData?.connected === false) {
          console.log("❌ Instance disconnected:", {
            instanceId: message.instanceId,
            error: message.statusData?.error,
            phoneNumber: message.phoneNumber
          });
          
          // Trigger disconnection callback
          if (instanceDisconnectedCallbackRef.current && message.instanceId && message.phoneNumber) {
            const error = message.statusData?.error || 'Instance disconnected';
            instanceDisconnectedCallbackRef.current(message.instanceId, message.phoneNumber, error);
          }
        }
      } else {
        console.log("🔔 Other WebSocket message type:", message.type, message);
      }
    },
    []
  );

  const connect = useCallback(() => {
    if (!accessToken || !user) {
      return;
    }

    // Prevent multiple connections
    if (clientRef.current?.connected) {
      console.log("WebSocket already connected, skipping connect");
      return;
    }

    // Disconnect existing client before creating new one
    if (clientRef.current) {
      console.log("Disconnecting existing WebSocket client");
      clientRef.current.deactivate();
      clientRef.current = null;
      subscriptionsRef.current = false;
    }

    // Usar a mesma base que o frontend mas porta 8080
    const currentOrigin = window.location.origin; // http://rubia.localhost:3000
    const wsUrl = currentOrigin.replace(":3000", ":8080") + "/ws";

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      setIsConnected(true);

      if (!user?.companyId) {
        return;
      }

      // Prevent duplicate subscriptions
      if (subscriptionsRef.current) {
        return;
      }

      // Subscribe to company-specific message topics
      client.subscribe(`/user/topic/messages`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body);
          handleNewMessage(data);
        } catch (error) {
          console.error("Error parsing message notification:", error);
        }
      });

      client.subscribe(`/user/topic/conversations`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body);
          handleConversationUpdate(data);
        } catch (error) {
          console.error("Error parsing conversation notification:", error);
        }
      });

      client.subscribe(`/user/topic/typing`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body);
          handleTypingStatus(data);
        } catch (error) {
          console.error("Error parsing typing notification:", error);
        }
      });

      client.subscribe(`/user/topic/instance-status`, (message: IMessage) => {
        try {
          console.log("🎯 Raw WebSocket message received on /user/topic/instance-status:", message.body);
          const data: WebSocketMessage = JSON.parse(message.body);
          console.log("🎯 Parsed WebSocket data:", data);
          handleInstanceStatusChange(data);
        } catch (error) {
          console.error("❌ Error parsing instance status notification:", error, "Raw body:", message.body);
        }
      });

      // Mark subscriptions as created
      subscriptionsRef.current = true;

      // Send join message
      client.publish({
        destination: "/app/join",
        body: JSON.stringify({ companyId: user.companyId }),
      });
    };

    client.onStompError = (frame: IFrame) => {
      console.error("STOMP error:", frame);
      setIsConnected(false);
      subscriptionsRef.current = false;
    };

    client.onWebSocketClose = () => {
      setIsConnected(false);
      subscriptionsRef.current = false;
    };

    client.onDisconnect = () => {
      setIsConnected(false);
      subscriptionsRef.current = false;
    };

    client.activate();
    clientRef.current = client;
  }, [
    accessToken,
    user,
    handleNewMessage,
    handleConversationUpdate,
    handleTypingStatus,
    handleInstanceStatusChange,
  ]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
      setIsConnected(false);
      subscriptionsRef.current = false;
    }
  }, []);

  const sendTyping = useCallback(
    (conversationId: string) => {
      if (clientRef.current?.connected && user) {
        clientRef.current.publish({
          destination: "/app/typing",
          body: JSON.stringify({
            conversationId,
            userName: user.name,
            isTyping: true,
          }),
        });
      }
    },
    [user]
  );

  const stopTyping = useCallback(
    (conversationId: string) => {
      if (clientRef.current?.connected && user) {
        clientRef.current.publish({
          destination: "/app/typing",
          body: JSON.stringify({
            conversationId,
            userName: user.name,
            isTyping: false,
          }),
        });
      }
    },
    [user]
  );

  const onInstanceConnected = useCallback((callback: (instanceId: string, phoneNumber: string) => void) => {
    instanceConnectedCallbackRef.current = callback;
  }, []);

  const onInstanceDisconnected = useCallback((callback: (instanceId: string, phoneNumber: string, error: string) => void) => {
    instanceDisconnectedCallbackRef.current = callback;
  }, []);

  useEffect(() => {
    if (accessToken && user) {
      try {
        // Delay inicial para evitar race conditions após refresh
        const connectTimeout = setTimeout(() => {
          connect();
        }, 500);

        return () => clearTimeout(connectTimeout);
      } catch (error) {
        console.error("Error in connect():", error);
      }
    } else {
      disconnect();
    }

    // CRITICAL: Cleanup on unmount/dependency change
    return () => {
      console.log("WebSocket useEffect cleanup - disconnecting");
      disconnect();
    };
  }, [accessToken, user?.id]);

  // Additional cleanup on page unload/refresh
  useEffect(() => {
    const handleBeforeUnload = () => {
      console.log("Page unloading - disconnecting WebSocket");
      if (clientRef.current?.connected) {
        clientRef.current.deactivate();
      }
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        console.log(
          "Page hidden - keeping WebSocket connected for quick resume"
        );
        // Não desconectar imediatamente, dar tempo para o usuário voltar
        // Em vez disso, apenas reduzir a frequência de heartbeat
      } else if (
        document.visibilityState === "visible" &&
        accessToken &&
        user &&
        !clientRef.current?.connected
      ) {
        console.log("Page visible and not connected - reconnecting WebSocket");
        setTimeout(() => connect(), 500); // Delay menor para reconexão mais rápida
      }
    };

    const handleWindowFocus = () => {
      // Se a página ganhou foco e não está conectada, tentar reconectar
      if (accessToken && user && !clientRef.current?.connected) {
        console.log(
          "Window focus gained and not connected - reconnecting WebSocket"
        );
        setTimeout(() => connect(), 200);
      }
    };

    const handlePageShow = (event: PageTransitionEvent) => {
      // Reconectar quando página é carregada do cache (back/forward)
      if (event.persisted && accessToken && user) {
        console.log("Page restored from cache - reconnecting WebSocket");
        setTimeout(() => connect(), 300);
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("focus", handleWindowFocus);
    window.addEventListener("pageshow", handlePageShow);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("focus", handleWindowFocus);
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, [accessToken, user, connect]);

  return {
    isConnected,
    connect,
    disconnect,
    sendTyping,
    stopTyping,
    onInstanceConnected,
    onInstanceDisconnected,
  };
};
