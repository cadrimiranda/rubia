export function formatTime(date: Date): string {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  
  if (messageDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }
  
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  
  if (messageDate.getTime() === yesterday.getTime()) {
    return 'Ontem'
  }
  
  const diffTime = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 7) {
    return date.toLocaleDateString('pt-BR', { weekday: 'short' })
  }
  
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit'
  })
}

export function formatMessageTime(date: Date): string {
  return date.toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatLastSeen(date: Date): string {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  
  if (diffMinutes < 1) return 'Agora mesmo'
  if (diffMinutes < 60) return `${diffMinutes} min atrás`
  if (diffHours < 24) return `${diffHours}h atrás`
  if (diffDays === 1) return 'Ontem'
  if (diffDays < 7) return `${diffDays} dias atrás`
  
  return date.toLocaleDateString('pt-BR')
}

export function formatChatListTime(date: Date): string {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  
  if (diffMinutes < 60) {
    return date.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }
  
  if (diffDays === 0) {
    return date.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }
  
  if (diffDays === 1) return 'Ontem'
  
  if (diffDays < 7) {
    return date.toLocaleDateString('pt-BR', { weekday: 'short' })
  }
  
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit'
  })
}

export function getStatusDisplayName(status: string): string {
  switch (status) {
    case 'entrada':
      return 'Entrada'
    case 'esperando':
      return 'Esperando'
    case 'finalizados':
      return 'Finalizados'
    default:
      return status
  }
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}