import { useEffect } from 'react'
import { useToast } from '../components/notifications/ToastProvider'

/**
 * Hook para gerenciar notificações do sistema
 * Escuta eventos globais e exibe toasts apropriados
 */
export const useNotifications = () => {
  const toast = useToast()

  useEffect(() => {
    // Listener para erros de chat
    const handleChatError = (event: CustomEvent) => {
      const { title, description } = event.detail
      toast.showError(title, description)
    }

    // Listener para sucessos de chat
    const handleChatSuccess = (event: CustomEvent) => {
      const { title, description } = event.detail
      toast.showSuccess(title, description)
    }

    // Listener para status de rede
    const handleNetworkStatus = () => {
      toast.showNetworkStatus(navigator.onLine)
    }

    // Listener para erros de API
    const handleApiError = (event: CustomEvent) => {
      const { title, description, type = 'error' } = event.detail
      
      if (type === 'network') {
        toast.showError('Erro de Conexão', 'Verifique sua conexão com a internet')
      } else if (type === 'auth') {
        toast.showWarning('Sessão Expirada', 'Faça login novamente')
      } else {
        toast.showError(title, description)
      }
    }

    // Registrar event listeners
    window.addEventListener('chat:error', handleChatError as EventListener)
    window.addEventListener('chat:success', handleChatSuccess as EventListener)
    window.addEventListener('api:error', handleApiError as EventListener)
    window.addEventListener('online', handleNetworkStatus)
    window.addEventListener('offline', handleNetworkStatus)

    // Cleanup
    return () => {
      window.removeEventListener('chat:error', handleChatError as EventListener)
      window.removeEventListener('chat:success', handleChatSuccess as EventListener)
      window.removeEventListener('api:error', handleApiError as EventListener)
      window.removeEventListener('online', handleNetworkStatus)
      window.removeEventListener('offline', handleNetworkStatus)
    }
  }, [toast])

  return {
    showSuccess: toast.showSuccess,
    showError: toast.showError,
    showWarning: toast.showWarning,
    showInfo: toast.showInfo,
    showNetworkStatus: toast.showNetworkStatus
  }
}

export default useNotifications