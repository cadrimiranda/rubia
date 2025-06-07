import { Avatar, Dropdown, Badge } from 'antd'
import { LogOut, Settings, User, Shield, Wifi, WifiOff } from 'lucide-react'
import { useAuthStore, useCurrentUser } from '../store/useAuthStore'
import { useConfirmDialog } from './ConfirmDialog'
import ConnectionStatus from './ConnectionStatus'

const UserHeader = () => {
  const user = useCurrentUser()
  const { logout, isLoggingOut, setOnlineStatus } = useAuthStore()
  const { confirmLogout } = useConfirmDialog()

  if (!user) return null

  const handleLogout = () => {
    confirmLogout(async () => {
      try {
        await logout()
      } catch (error) {
        console.error('Erro ao fazer logout:', error)
      }
    })
  }

  const toggleOnlineStatus = () => {
    setOnlineStatus(!user.isOnline)
  }

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'text-red-600 bg-red-50'
      case 'SUPERVISOR':
        return 'text-blue-600 bg-blue-50'
      case 'AGENT':
        return 'text-green-600 bg-green-50'
      default:
        return 'text-gray-600 bg-gray-50'
    }
  }

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'Administrador'
      case 'SUPERVISOR':
        return 'Supervisor'
      case 'AGENT':
        return 'Agente'
      default:
        return role
    }
  }

  const menuItems = [
    {
      key: 'profile',
      label: (
        <div className="flex items-center space-x-3 py-2">
          <User size={16} />
          <span>Meu Perfil</span>
        </div>
      ),
    },
    {
      key: 'settings',
      label: (
        <div className="flex items-center space-x-3 py-2">
          <Settings size={16} />
          <span>Configurações</span>
        </div>
      ),
    },
    {
      key: 'divider',
      type: 'divider' as const,
    },
    {
      key: 'online-status',
      label: (
        <div 
          className="flex items-center space-x-3 py-2 cursor-pointer"
          onClick={toggleOnlineStatus}
        >
          {user.isOnline ? <Wifi size={16} /> : <WifiOff size={16} />}
          <span>{user.isOnline ? 'Marcar como Offline' : 'Marcar como Online'}</span>
        </div>
      ),
    },
    {
      key: 'divider2',
      type: 'divider' as const,
    },
    {
      key: 'logout',
      label: (
        <div 
          className="flex items-center space-x-3 py-2 text-red-600 cursor-pointer"
          onClick={handleLogout}
        >
          <LogOut size={16} />
          <span>{isLoggingOut ? 'Saindo...' : 'Sair'}</span>
        </div>
      ),
      disabled: isLoggingOut,
    },
  ]

  return (
    <div className="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200">
      {/* Logo/Title */}
      <div className="flex items-center space-x-3">
        <div className="w-8 h-8 bg-rose-500 rounded-lg flex items-center justify-center">
          <span className="text-white font-bold text-sm">R</span>
        </div>
        <h1 className="text-lg font-semibold text-gray-800">Rubia Chat</h1>
      </div>

      {/* User Info */}
      <div className="flex items-center space-x-3">
        {/* Connection Status */}
        <ConnectionStatus showText className="hidden lg:flex" />

        {/* Department Info */}
        {user.department && (
          <div className="hidden md:flex items-center space-x-2 text-sm text-gray-600">
            <Shield size={14} />
            <span>{user.department.name}</span>
          </div>
        )}

        {/* Role Badge */}
        <div className={`hidden sm:flex px-2 py-1 rounded-full text-xs font-medium ${getRoleColor(user.role)}`}>
          {getRoleLabel(user.role)}
        </div>

        {/* User Dropdown */}
        <Dropdown
          menu={{ items: menuItems }}
          trigger={['click']}
          placement="bottomRight"
        >
          <div className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 rounded-lg p-2 transition-colors">
            <Badge 
              dot 
              status={user.isOnline ? 'success' : 'default'}
              offset={[-2, 2]}
            >
              <Avatar
                src={user.avatarUrl}
                size={32}
                className="ring-1 ring-gray-200"
              >
                {user.name.charAt(0).toUpperCase()}
              </Avatar>
            </Badge>
            
            <div className="hidden md:block text-left">
              <div className="text-sm font-medium text-gray-800 truncate max-w-32">
                {user.name}
              </div>
              <div className="text-xs text-gray-500 truncate max-w-32">
                {user.email}
              </div>
            </div>
          </div>
        </Dropdown>
      </div>
    </div>
  )
}

export default UserHeader