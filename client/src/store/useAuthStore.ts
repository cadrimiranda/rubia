import { create } from 'zustand'
import { authService, type AuthUser } from '../auth/authService'
import type { LoginRequest } from '../api/types'

interface AuthState {
  // Estado
  user: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
  
  // Estados de UI
  showLoginModal: boolean
  isLoggingIn: boolean
  isLoggingOut: boolean
}

interface AuthActions {
  // Autenticação
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  checkAuth: () => void
  refreshToken: () => Promise<void>
  
  // Gestão de usuário
  updateProfile: (userData: Partial<AuthUser>) => Promise<void>
  setOnlineStatus: (isOnline: boolean) => Promise<void>
  
  // UI
  showLogin: () => void
  hideLogin: () => void
  clearError: () => void
  
  // Permissões
  hasPermission: (role: 'ADMIN' | 'SUPERVISOR' | 'AGENT') => boolean
  
  // Utilitários
  initialize: () => void
}

export const useAuthStore = create<AuthState & AuthActions>((set, get) => ({
  // Estado inicial
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  showLoginModal: false,
  isLoggingIn: false,
  isLoggingOut: false,

  // Login
  login: async (credentials: LoginRequest) => {
    const state = get()
    if (state.isLoggingIn) return

    try {
      set({ isLoggingIn: true, error: null })
      
      const user = await authService.login(credentials)
      
      set({
        user,
        isAuthenticated: true,
        isLoggingIn: false,
        showLoginModal: false,
        error: null
      })
      
      // Inicializar status online
      await authService.updateOnlineStatus(true)
      
    } catch (error) {
      console.error('Erro no login:', error)
      set({
        error: error instanceof Error ? error.message : 'Erro ao fazer login',
        isLoggingIn: false,
        user: null,
        isAuthenticated: false
      })
      throw error
    }
  },

  // Logout
  logout: async () => {
    const state = get()
    if (state.isLoggingOut) return

    try {
      set({ isLoggingOut: true, error: null })
      
      // Atualizar status para offline antes de fazer logout
      if (state.user) {
        await authService.updateOnlineStatus(false)
      }
      
      await authService.logout()
      
      set({
        user: null,
        isAuthenticated: false,
        isLoggingOut: false,
        error: null
      })
      
    } catch (error) {
      console.error('Erro no logout:', error)
      // Mesmo com erro, limpar dados locais
      set({
        user: null,
        isAuthenticated: false,
        isLoggingOut: false,
        error: 'Erro ao fazer logout'
      })
    }
  },

  // Verificar autenticação
  checkAuth: () => {
    try {
      const isAuthenticated = authService.isAuthenticated()
      const user = authService.getCurrentUser()
      
      set({
        isAuthenticated,
        user,
        isLoading: false
      })
      
      // Se não autenticado, mostrar modal de login
      if (!isAuthenticated) {
        set({ showLoginModal: true })
      }
      
    } catch (error) {
      console.error('Erro ao verificar autenticação:', error)
      set({
        isAuthenticated: false,
        user: null,
        isLoading: false,
        showLoginModal: true
      })
    }
  },

  // Renovar token
  refreshToken: async () => {
    try {
      await authService.refreshToken()
      
      // Verificar se ainda está autenticado
      const isAuthenticated = authService.isAuthenticated()
      const user = authService.getCurrentUser()
      
      set({ isAuthenticated, user })
      
    } catch (error) {
      console.error('Erro ao renovar token:', error)
      
      // Token inválido, fazer logout
      set({
        user: null,
        isAuthenticated: false,
        showLoginModal: true,
        error: 'Sessão expirada. Faça login novamente.'
      })
    }
  },

  // Atualizar perfil
  updateProfile: async (userData: Partial<AuthUser>) => {
    const state = get()
    if (!state.user) return

    try {
      set({ isLoading: true, error: null })
      
      const updatedUser = await authService.updateUserProfile(userData)
      
      set({
        user: updatedUser,
        isLoading: false
      })
      
    } catch (error) {
      console.error('Erro ao atualizar perfil:', error)
      set({
        error: error instanceof Error ? error.message : 'Erro ao atualizar perfil',
        isLoading: false
      })
      throw error
    }
  },

  // Definir status online
  setOnlineStatus: async (isOnline: boolean) => {
    const state = get()
    if (!state.user) return

    try {
      await authService.updateOnlineStatus(isOnline)
      
      // Atualizar estado local
      set({
        user: {
          ...state.user,
          isOnline
        }
      })
      
    } catch (error) {
      console.error('Erro ao atualizar status online:', error)
    }
  },

  // Mostrar modal de login
  showLogin: () => {
    set({ showLoginModal: true, error: null })
  },

  // Esconder modal de login
  hideLogin: () => {
    set({ showLoginModal: false, error: null })
  },

  // Limpar erro
  clearError: () => {
    set({ error: null })
  },

  // Verificar permissões
  hasPermission: (requiredRole: 'ADMIN' | 'SUPERVISOR' | 'AGENT') => {
    return authService.hasPermission(requiredRole)
  },

  // Inicializar store
  initialize: () => {
    set({ isLoading: true })
    
    // Verificar autenticação inicial
    get().checkAuth()
    
    // Configurar listeners para renovação automática de token
    const checkTokenInterval = setInterval(() => {
      const state = get()
      if (state.isAuthenticated && authService.isTokenExpiringSoon()) {
        get().refreshToken()
      }
    }, 60000) // Verificar a cada minuto
    
    // Listener para logout automático do API client
    const handleAutoLogout = () => {
      set({
        user: null,
        isAuthenticated: false,
        showLoginModal: true,
        error: 'Sessão expirada. Faça login novamente.'
      })
    }

    if (typeof window !== 'undefined') {
      window.addEventListener('auth:logout', handleAutoLogout)
      
      // Cleanup do interval quando a store for destruída
      window.addEventListener('beforeunload', () => {
        clearInterval(checkTokenInterval)
        window.removeEventListener('auth:logout', handleAutoLogout)
      })
    }
    
    // Listener para mudanças de visibilidade da página
    if (typeof document !== 'undefined') {
      const handleVisibilityChange = () => {
        const state = get()
        if (state.isAuthenticated) {
          if (document.visibilityState === 'visible') {
            // Página ficou visível, marcar como online
            get().setOnlineStatus(true)
          } else {
            // Página ficou oculta, marcar como offline
            get().setOnlineStatus(false)
          }
        }
      }
      
      document.addEventListener('visibilitychange', handleVisibilityChange)
    }
    
    // Inicializar authService
    authService.init()
  }
}))

// Hook para verificar se está autenticado
export const useIsAuthenticated = () => {
  return useAuthStore(state => state.isAuthenticated)
}

// Hook para verificar permissões
export const useHasPermission = (role: 'ADMIN' | 'SUPERVISOR' | 'AGENT') => {
  return useAuthStore(state => state.hasPermission(role))
}

// Hook para dados do usuário atual
export const useCurrentUser = () => {
  return useAuthStore(state => state.user)
}

// Hook para estado de loading
export const useAuthLoading = () => {
  return useAuthStore(state => ({
    isLoading: state.isLoading,
    isLoggingIn: state.isLoggingIn,
    isLoggingOut: state.isLoggingOut
  }))
}

export default useAuthStore