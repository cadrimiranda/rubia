import { useEffect, useState } from 'react'
import { useChatStore } from '../store/useChatStore'

interface TypingIndicatorProps {
  conversationId: string
  className?: string
}

export const TypingIndicator: React.FC<TypingIndicatorProps> = ({ 
  conversationId, 
  className = '' 
}) => {
  const getTypingUsers = useChatStore(state => state.getTypingUsers)
  const [typingUsers, setTypingUsers] = useState<string[]>([])

  useEffect(() => {
    const updateTypingUsers = () => {
      const users = getTypingUsers(conversationId)
      setTypingUsers(users)
    }

    // Atualizar imediatamente
    updateTypingUsers()

    // Atualizar a cada segundo para limpar usuários inativos
    const interval = setInterval(updateTypingUsers, 1000)

    return () => clearInterval(interval)
  }, [conversationId, getTypingUsers])

  if (typingUsers.length === 0) {
    return null
  }

  const getTypingText = () => {
    if (typingUsers.length === 1) {
      return `${typingUsers[0]} está digitando...`
    }
    
    if (typingUsers.length === 2) {
      return `${typingUsers[0]} e ${typingUsers[1]} estão digitando...`
    }
    
    return `${typingUsers.length} pessoas estão digitando...`
  }

  return (
    <div className={`flex items-center space-x-2 text-sm text-gray-500 px-4 py-2 ${className}`}>
      <div className="flex space-x-1">
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" 
             style={{ animationDelay: '0ms' }} />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" 
             style={{ animationDelay: '150ms' }} />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" 
             style={{ animationDelay: '300ms' }} />
      </div>
      <span className="italic">{getTypingText()}</span>
    </div>
  )
}

export default TypingIndicator