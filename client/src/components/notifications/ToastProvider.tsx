import { createContext, useContext, useCallback } from 'react'
import { notification } from 'antd'
import { CheckCircle, AlertCircle, XCircle, Info, Wifi, WifiOff } from 'lucide-react'

type NotificationType = 'success' | 'error' | 'warning' | 'info'

interface ToastContextType {
  showToast: (type: NotificationType, title: string, description?: string, duration?: number) => void
  showSuccess: (title: string, description?: string) => void
  showError: (title: string, description?: string) => void
  showWarning: (title: string, description?: string) => void
  showInfo: (title: string, description?: string) => void
  showNetworkStatus: (isOnline: boolean) => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export const useToast = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider')
  }
  return context
}

interface ToastProviderProps {
  children: React.ReactNode
}

export const ToastProvider: React.FC<ToastProviderProps> = ({ children }) => {
  const [api, contextHolder] = notification.useNotification()

  const getIcon = (type: NotificationType) => {
    const iconProps = { size: 20 }
    switch (type) {
      case 'success':
        return <CheckCircle {...iconProps} className="text-green-500" />
      case 'error':
        return <XCircle {...iconProps} className="text-red-500" />
      case 'warning':
        return <AlertCircle {...iconProps} className="text-yellow-500" />
      case 'info':
        return <Info {...iconProps} className="text-blue-500" />
      default:
        return <Info {...iconProps} className="text-gray-500" />
    }
  }

  const showToast = useCallback((
    type: NotificationType,
    title: string,
    description?: string,
    duration = 4
  ) => {
    api[type]({
      message: title,
      description,
      duration,
      icon: getIcon(type),
      placement: 'topRight',
      style: {
        borderRadius: '12px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
      }
    })
  }, [api])

  const showSuccess = useCallback((title: string, description?: string) => {
    showToast('success', title, description)
  }, [showToast])

  const showError = useCallback((title: string, description?: string) => {
    showToast('error', title, description, 6) // Erros ficam mais tempo
  }, [showToast])

  const showWarning = useCallback((title: string, description?: string) => {
    showToast('warning', title, description)
  }, [showToast])

  const showInfo = useCallback((title: string, description?: string) => {
    showToast('info', title, description)
  }, [showToast])

  const showNetworkStatus = useCallback((isOnline: boolean) => {
    const title = isOnline ? 'Conectado' : 'Desconectado'
    const description = isOnline 
      ? 'Conexão com o servidor restaurada'
      : 'Sem conexão com o servidor'
    
    api[isOnline ? 'success' : 'error']({
      message: title,
      description,
      duration: isOnline ? 3 : 0, // Offline fica até voltar
      icon: isOnline 
        ? <Wifi size={20} className="text-green-500" />
        : <WifiOff size={20} className="text-red-500" />,
      placement: 'topRight',
      style: {
        borderRadius: '12px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
      }
    })
  }, [api])

  const value: ToastContextType = {
    showToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showNetworkStatus
  }

  return (
    <ToastContext.Provider value={value}>
      {contextHolder}
      {children}
    </ToastContext.Provider>
  )
}

export default ToastProvider