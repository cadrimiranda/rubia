import { useState, useEffect } from 'react'
import { Badge, Tooltip } from 'antd'
import { WifiOff, AlertCircle, CheckCircle } from 'lucide-react'

interface ConnectionStatusProps {
  className?: string
  showText?: boolean
}

export const useConnectionStatus = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine)
  const [isApiConnected, setIsApiConnected] = useState(true)
  const [lastChecked, setLastChecked] = useState<Date>(new Date())

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true)
      setLastChecked(new Date())
    }

    const handleOffline = () => {
      setIsOnline(false)
      setLastChecked(new Date())
    }

    const checkApiConnection = async () => {
      try {
        const response = await fetch('/api/health', {
          method: 'HEAD',
          cache: 'no-cache'
        })
        setIsApiConnected(response.ok)
      } catch {
        setIsApiConnected(false)
      }
      setLastChecked(new Date())
    }

    // Event listeners para conexão de rede
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    // Verificar conexão com API a cada 30 segundos
    const apiCheckInterval = setInterval(checkApiConnection, 30000)

    // Verificação inicial
    checkApiConnection()

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
      clearInterval(apiCheckInterval)
    }
  }, [])

  const connectionStatus = {
    isOnline,
    isApiConnected,
    isFullyConnected: isOnline && isApiConnected,
    lastChecked
  }

  return connectionStatus
}

const ConnectionStatus: React.FC<ConnectionStatusProps> = ({ 
  className = '', 
  showText = false 
}) => {
  const { isOnline, isApiConnected, lastChecked } = useConnectionStatus()

  const getStatusInfo = () => {
    if (!isOnline) {
      return {
        icon: <WifiOff size={16} />,
        color: 'error' as const,
        text: 'Offline',
        description: 'Sem conexão com a internet'
      }
    }

    if (!isApiConnected) {
      return {
        icon: <AlertCircle size={16} />,
        color: 'warning' as const,
        text: 'Instável',
        description: 'Problemas de conexão com o servidor'
      }
    }

    return {
      icon: <CheckCircle size={16} />,
      color: 'success' as const,
      text: 'Online',
      description: 'Conectado ao servidor'
    }
  }

  const status = getStatusInfo()

  const tooltipContent = (
    <div className="text-center">
      <div className="font-medium">{status.description}</div>
      <div className="text-xs opacity-75 mt-1">
        Última verificação: {lastChecked.toLocaleTimeString()}
      </div>
    </div>
  )

  if (showText) {
    return (
      <Tooltip title={tooltipContent}>
        <div className={`flex items-center space-x-2 ${className}`}>
          <Badge status={status.color} />
          <span className="text-sm text-gray-600">{status.text}</span>
        </div>
      </Tooltip>
    )
  }

  return (
    <Tooltip title={tooltipContent}>
      <div className={`flex items-center ${className}`}>
        <Badge 
          status={status.color}
          className="cursor-pointer"
        />
      </div>
    </Tooltip>
  )
}

// Componente de banner para conexão offline
export const OfflineBanner: React.FC = () => {
  const { isOnline, isApiConnected } = useConnectionStatus()
  const isFullyConnected = isOnline && isApiConnected

  if (isFullyConnected) return null

  const getMessage = () => {
    if (!isOnline) {
      return {
        text: 'Você está offline',
        subtitle: 'Verifique sua conexão com a internet',
        bgColor: 'bg-red-500'
      }
    }

    if (!isApiConnected) {
      return {
        text: 'Problemas de conexão',
        subtitle: 'Dificuldades para conectar com o servidor',
        bgColor: 'bg-yellow-500'
      }
    }

    return null
  }

  const message = getMessage()
  if (!message) return null

  return (
    <div className={`${message.bgColor} text-white px-4 py-2 text-center text-sm`}>
      <div className="flex items-center justify-center space-x-2">
        <WifiOff size={16} />
        <div>
          <span className="font-medium">{message.text}</span>
          {message.subtitle && (
            <span className="ml-2 opacity-90">• {message.subtitle}</span>
          )}
        </div>
      </div>
    </div>
  )
}

export default ConnectionStatus