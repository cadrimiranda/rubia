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
  const { user } = useAuthStore()
  const accessToken = authService.getAccessToken()
  const { addMessage, updateConversation, setTypingUser } = useChatStore()

  const handleNewMessage = useCallback((message: WebSocketMessage) => {
    if (message.type === 'NEW_MESSAGE' && message.message && message.conversation) {
      addMessage(message.conversation.id, message.message)
      updateConversation(message.conversation)
    }
  }, [addMessage, updateConversation])

  const handleConversationUpdate = useCallback((message: WebSocketMessage) => {
    if (message.type === 'CONVERSATION_UPDATE' && message.conversation) {
      updateConversation(message.conversation)
    }
  }, [updateConversation])

  const handleTypingStatus = useCallback((message: WebSocketMessage) => {
    if (message.type === 'TYPING_STATUS' && message.conversationId && message.userName) {
      setTypingUser(message.conversationId, message.userName, message.isTyping || false)
    }
  }, [setTypingUser])

  const connect = useCallback(() => {
    if (!accessToken || clientRef.current?.connected) {
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
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
      console.log('WebSocket connected:', frame)
      setIsConnected(true)

      if (!user?.companyId) return

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

      // Send join message
      client.publish({
        destination: '/app/join',
        body: JSON.stringify({ companyId: user.companyId })
      })
    }

    client.onStompError = (frame: IFrame) => {
      console.error('STOMP error:', frame)
      setIsConnected(false)
    }

    client.onWebSocketClose = () => {
      console.log('WebSocket connection closed')
      setIsConnected(false)
    }

    client.onDisconnect = () => {
      console.log('STOMP disconnected')
      setIsConnected(false)
    }

    client.activate()
    clientRef.current = client
  }, [accessToken, user, handleNewMessage, handleConversationUpdate, handleTypingStatus])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
      setIsConnected(false)
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
      connect()
    } else {
      disconnect()
    }

    return () => {
      disconnect()
    }
  }, [accessToken, user, connect, disconnect])

  useEffect(() => {
    return () => {
      disconnect()
    }
  }, [disconnect])

  return {
    isConnected,
    connect,
    disconnect,
    sendTyping,
    stopTyping
  }
}