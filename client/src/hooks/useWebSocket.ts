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
    console.log('🔔 NEW MESSAGE received via WebSocket!', message)
    if (message.type === 'NEW_MESSAGE' && message.message && message.conversation) {
      // WebSocket deve processar APENAS mensagens de outros usuários
      const currentUserId = user?.id;
      const messageData = message.message;
      
      // Múltiplas verificações para identificar mensagens próprias
      const isFromCurrentUser = 
        messageData.senderId === currentUserId ||
        messageData.senderType === 'AGENT' ||
        messageData.isFromUser === true;
      
      console.log('🔍 Verificação de origem da mensagem:', {
        currentUserId,
        messageSenderId: messageData.senderId,
        messageSenderType: messageData.senderType,
        messageIsFromUser: messageData.isFromUser,
        isFromCurrentUser
      });
      
      if (isFromCurrentUser) {
        console.log('🚫 Ignorando mensagem própria - mensagens enviadas são tratadas localmente');
        return;
      }
      
      console.log('📨 Processando mensagem de terceiro:', message.conversation.id, messageData)
      addMessage(message.conversation.id, messageData)
      updateConversation(message.conversation.id, message.conversation)
      console.log('✅ Mensagem de terceiro adicionada ao chat!')
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
    console.log('🚀 Attempting to connect WebSocket...')
    console.log('🔑 Access token:', accessToken ? '✅ Present' : '❌ Missing')
    console.log('🔑 Access token value:', accessToken ? `${accessToken.substring(0, 20)}...` : 'null')
    console.log('👤 User:', user)
    console.log('🔌 Current connection:', clientRef.current?.connected ? '✅ Already connected' : '⚡ Connecting...')
    
    if (!accessToken || !user || clientRef.current?.connected) {
      console.log('⏹️ Connection aborted - token/user missing or already connected')
      return
    }

    // Usar a mesma base que o frontend mas porta 8080
    const currentOrigin = window.location.origin; // http://rubia.localhost:3000
    const wsUrl = currentOrigin.replace(':3000', ':8080') + '/ws';
    console.log('🌐 WebSocket URL:', wsUrl);
    console.log('🌐 Current origin:', currentOrigin);
    
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      debug: (str) => {
        console.log('STOMP: ' + str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    client.onConnect = (frame: IFrame) => {
      console.log('🌐 WebSocket connected successfully:', frame)
      console.log('🏢 User company ID:', user?.companyId)
      console.log('👤 User ID:', user?.id)
      console.log('📧 User email:', user?.email)
      console.log('🔗 User principal name should be:', user?.id)
      setIsConnected(true)

      if (!user?.companyId) {
        console.log('❌ No company ID found, skipping subscriptions')
        return
      }

      // Prevent duplicate subscriptions
      if (subscriptionsRef.current) {
        console.log('🔄 Subscriptions already exist, skipping re-subscription')
        return
      }

      // Subscribe to company-specific message topics
      console.log('📡 Subscribing to /user/topic/messages')
      client.subscribe(`/user/topic/messages`, (message: IMessage) => {
        console.log('🔔 MESSAGE RECEIVED ON /user/topic/messages!')
        console.log('🔔 Raw message:', message)
        console.log('🔔 Message body:', message.body)
        try {
          const data: WebSocketMessage = JSON.parse(message.body)
          console.log('🔔 Parsed data:', data)
          handleNewMessage(data)
        } catch (error) {
          console.error('❌ Error parsing message notification:', error, 'Raw body:', message.body)
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
      console.error('❌ STOMP error:', frame)
      console.error('❌ Error details:', frame.headers, frame.body)
      setIsConnected(false)
      subscriptionsRef.current = false
    }

    client.onWebSocketClose = (event: any) => {
      console.log('🔌 WebSocket connection closed')
      console.log('🔌 Close event:', event)
      setIsConnected(false)
      subscriptionsRef.current = false
    }

    client.onDisconnect = (frame: IFrame) => {
      console.log('🔌 STOMP disconnected')
      console.log('🔌 Disconnect frame:', frame)
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
    console.log('🔄 WebSocket useEffect triggered')
    console.log('🔄 - accessToken exists:', !!accessToken)
    console.log('🔄 - user exists:', !!user)
    
    if (accessToken && user) {
      console.log('✅ Conditions met, calling connect()')
      try {
        connect()
      } catch (error) {
        console.error('❌ Error in connect():', error)
      }
    } else {
      console.log('❌ Conditions not met, calling disconnect()')
      console.log('❌ - accessToken:', !!accessToken)
      console.log('❌ - user:', !!user)
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