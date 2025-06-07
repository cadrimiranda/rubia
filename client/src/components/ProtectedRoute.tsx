import { useEffect } from 'react'
import { Spin } from 'antd'
import { useAuthStore } from '../store/useAuthStore'
import LoginModal from './LoginModal'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredRole?: 'ADMIN' | 'SUPERVISOR' | 'AGENT'
  fallback?: React.ReactNode
}

/**
 * Componente para proteção de rotas que requer autenticação
 * e opcionalmente uma role específica
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole = 'AGENT',
  fallback 
}) => {
  const { 
    isAuthenticated, 
    isLoading, 
    user, 
    hasPermission,
    initialize,
    showLoginModal 
  } = useAuthStore()

  // Inicializar autenticação quando o componente montar
  useEffect(() => {
    initialize()
  }, [initialize])

  // Loading inicial enquanto verifica autenticação
  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100">
        <div className="text-center">
          <Spin size="large" />
          <p className="mt-4 text-gray-600">Verificando autenticação...</p>
        </div>
      </div>
    )
  }

  // Se não está autenticado, mostrar modal de login
  if (!isAuthenticated) {
    return (
      <div className="h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100">
        <div className="text-center px-6">
          <div className="w-20 h-20 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg 
              className="w-10 h-10 text-rose-500" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" 
              />
            </svg>
          </div>
          <h2 className="text-2xl font-semibold text-gray-800 mb-2">
            Acesso Restrito
          </h2>
          <p className="text-gray-600 mb-6">
            Você precisa fazer login para acessar esta área
          </p>
        </div>
        <LoginModal />
      </div>
    )
  }

  // Verificar permissões se uma role específica for necessária
  if (requiredRole && !hasPermission(requiredRole)) {
    return (
      <div className="h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100">
        <div className="text-center px-6">
          <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg 
              className="w-10 h-10 text-red-500" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728L5.636 5.636m12.728 12.728L5.636 5.636" 
              />
            </svg>
          </div>
          <h2 className="text-2xl font-semibold text-gray-800 mb-2">
            Acesso Negado
          </h2>
          <p className="text-gray-600 mb-2">
            Você não tem permissão para acessar esta área
          </p>
          <p className="text-sm text-gray-500">
            Role necessária: <span className="font-medium">{requiredRole}</span>
          </p>
          <p className="text-sm text-gray-500">
            Sua role: <span className="font-medium">{user?.role}</span>
          </p>
          
          {fallback && (
            <div className="mt-6">
              {fallback}
            </div>
          )}
        </div>
      </div>
    )
  }

  // Renderizar conteúdo protegido
  return (
    <>
      {children}
      {showLoginModal && <LoginModal />}
    </>
  )
}

export default ProtectedRoute