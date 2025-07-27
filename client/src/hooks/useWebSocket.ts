import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, type IFrame, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/useAuthStore'
import { useChatStore } from '../store/useChatStore'
import { authService } from '../auth/authService'
import type { Message } from '../types'

interface WebSocketMessage {
  type: string
  message?: Message
  conversation?: any // Using any for now until ConversationDTO is available
  conversationId?: string
  userName?: string
  isTyping?: boolean
}

interface UseWebSocketReturn {
  isConnected: boolean
  connect: () => void
  disconnect: () => void
  sendTyping: (conversationId: string) => void
  stopTyping: (conversationId: string) => void
}

export const useWebSocket = (): UseWebSocketReturn => {
  const [isConnected, setIsConnected] = useState(false)
  const clientRef = useRef<Client | null>(null)
  const subscriptionsRef = useRef<boolean>(false)
  const { user } = useAuthStore()
  const accessToken = authService.getAccessToken()
  const { addMessage, updateConversation, setTypingUser: setUserTyping } = useChatStore()

  const handleNewMessage = useCallback((message: WebSocketMessage) => {
    if (message.type === 'NEW_MESSAGE' && message.message && message.conversation) {
      // WebSocket deve processar APENAS mensagens de outros usuários
      const currentUserId = user?.id;
      const messageData = message.message;
      
      // Múltiplas verificações para identificar mensagens próprias
      const isFromCurrentUser = 
        messageData.senderId === currentUserId ||
        messageData.senderType === 'AGENT' ||
        messageData.isFromUser === true;
      
      
      if (isFromCurrentUser) {
        return;
      }
      
      addMessage(message.conversation.id, messageData)
      updateConversation(message.conversation.id, message.conversation)
    }
  }, [addMessage, updateConversation, user?.id])

  const handleConversationUpdate = useCallback((message: WebSocketMessage) => {
    if (message.type === 'CONVERSATION_UPDATE' && message.conversation) {
      updateConversation(message.conversation.id, message.conversation)
    }
  }, [updateConversation])

  const handleTypingStatus = useCallback((message: WebSocketMessage) => {
    if (message.type === 'TYPING_STATUS' && message.conversationId && message.userName) {
      setUserTyping(message.conversationId, 'user', message.userName, message.isTyping || false)
    }
  }, [setUserTyping])

  const connect = useCallback(() => {
    
    if (!accessToken || !user || clientRef.current?.connected) {
      return
    }

    // Usar a mesma base que o frontend mas porta 8080
    const currentOrigin = window.location.origin; // http://rubia.localhost:3000
    const wsUrl = currentOrigin.replace(':3000', ':8080') + '/ws';
    
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    client.onConnect = (frame: IFrame) => {
      setIsConnected(true)

      if (!user?.companyId) {
        return
      }

      // Prevent duplicate subscriptions
      if (subscriptionsRef.current) {
        return
      }

      // Subscribe to company-specific message topics
      client.subscribe(`/user/topic/messages`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body)
          handleNewMessage(data)
        } catch (error) {
          console.error('Error parsing message notification:', error)
        }
      })

      client.subscribe(`/user/topic/conversations`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body)
          handleConversationUpdate(data)
        } catch (error) {
          console.error('Error parsing conversation notification:', error)
        }
      })

      client.subscribe(`/user/topic/typing`, (message: IMessage) => {
        try {
          const data: WebSocketMessage = JSON.parse(message.body)
          handleTypingStatus(data)
        } catch (error) {
          console.error('Error parsing typing notification:', error)
        }
      })

      // Mark subscriptions as created
      subscriptionsRef.current = true

      // Send join message
      client.publish({
        destination: '/app/join',
        body: JSON.stringify({ companyId: user.companyId })
      })
    }

    client.onStompError = (frame: IFrame) => {
      console.error('STOMP error:', frame)
      setIsConnected(false)
      subscriptionsRef.current = false
    }

    client.onWebSocketClose = (event: any) => {
      setIsConnected(false)
      subscriptionsRef.current = false
    }

    client.onDisconnect = (frame: IFrame) => {
      setIsConnected(false)
      subscriptionsRef.current = false
    }

    client.activate()
    clientRef.current = client
  }, [accessToken, user, handleNewMessage, handleConversationUpdate, handleTypingStatus])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
      setIsConnected(false)
      subscriptionsRef.current = false
    }
  }, [])

  const sendTyping = useCallback((conversationId: string) => {
    if (clientRef.current?.connected && user) {
      clientRef.current.publish({
        destination: '/app/typing',
        body: JSON.stringify({
          conversationId,
          userName: user.name,
          isTyping: true
        })
      })
    }
  }, [user])

  const stopTyping = useCallback((conversationId: string) => {
    if (clientRef.current?.connected && user) {
      clientRef.current.publish({
        destination: '/app/typing',
        body: JSON.stringify({
          conversationId,
          userName: user.name,
          isTyping: false
        })
      })
    }
  }, [user])

  useEffect(() => {
    
    if (accessToken && user) {
      try {
        connect()
      } catch (error) {
        console.error('Error in connect():', error)
      }
    } else {
      disconnect()
    }

    return () => {
      disconnect()
    }
  }, [accessToken, user?.id])


  return {
    isConnected,
    connect,
    disconnect,
    sendTyping,
    stopTyping
  }
}