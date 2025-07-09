import { apiClient } from '../api/client'
import type { LoginRequest, LoginResponse } from '../api/types'
import { getCurrentCompanySlug, getCompanyFromSubdomain } from '../utils/company'

export interface AuthTokens {
  accessToken: string
  refreshToken?: string
  expiresAt: number
}

export interface AuthUser {
  id: string
  name: string
  email: string
  role: 'ADMIN' | 'SUPERVISOR' | 'AGENT'
  department?: {
    id: string
    name: string
  }
  avatarUrl?: string
  isOnline: boolean
  companyId: string
  companySlug: string
}

class AuthService {
  private readonly ACCESS_TOKEN_KEY = 'auth_token'
  private readonly REFRESH_TOKEN_KEY = 'refresh_token'
  private readonly USER_KEY = 'auth_user'
  private readonly EXPIRES_AT_KEY = 'token_expires_at'
  private readonly COMPANY_KEY = 'auth_company'
  

  /**
   * Realiza login com email e senha com contexto da empresa
   */
  async login(credentials: LoginRequest): Promise<AuthUser> {
    try {
      // Login real com API
      console.log('🌐 Usando autenticação da API');
      
      // Obter contexto da empresa atual
      const companyInfo = getCompanyFromSubdomain()
      
      // Incluir company slug nas credenciais (limitado para evitar headers grandes)
      const sanitizedSlug = companyInfo.slug?.substring(0, 50) || 'default';
      const loginData = {
        email: credentials.email,
        password: credentials.password,
        companySlug: sanitizedSlug
      }
      
      const response = await apiClient.post<LoginResponse>('/api/auth/login', loginData)
      
      // Salvar tokens
      this.setTokens({
        accessToken: response.token,
        expiresAt: Date.now() + (response.expiresIn * 1000)
      })

      // Converter UserInfo para AuthUser com company context
      const user = this.mapUserInfoToAuthUser(response.user, response.companyId, response.companySlug)
      this.setUser(user)
      this.setCompanyContext(response.companyId, response.companySlug)

      return user
    } catch (error) {
      console.error('Erro no login:', error)
      throw error
    }
  }

  /**
   * Realiza logout e limpa dados
   */
  async logout(): Promise<void> {
    try {
      // Chamar endpoint de logout no backend (opcional)
      const token = this.getAccessToken()
      if (token) {
        await apiClient.post('/api/auth/logout')
      }
    } catch (error) {
      console.warn('Erro ao fazer logout no servidor:', error)
    } finally {
      // Sempre limpar dados locais
      this.clearAuthData()
    }
  }

  /**
   * Verifica se o usuário está autenticado e no contexto correto da empresa
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken()
    const expiresAt = this.getTokenExpiresAt()
    const user = this.getCurrentUser()
    
    if (!token || !expiresAt || !user) {
      return false
    }

    // Verificar se o token ainda é válido (com margem de 5 minutos)
    const fiveMinutes = 5 * 60 * 1000
    const isTokenValid = Date.now() < (expiresAt - fiveMinutes)
    
    // Verificar se está na empresa correta
    const currentCompanySlug = getCurrentCompanySlug()
    const isCorrectCompany = user.companySlug === currentCompanySlug
    
    return isTokenValid && isCorrectCompany
  }

  /**
   * Verifica se o token está próximo do vencimento
   */
  isTokenExpiringSoon(): boolean {
    const expiresAt = this.getTokenExpiresAt()
    if (!expiresAt) return false

    // Considerar "expirando em breve" se falta menos de 10 minutos
    const tenMinutes = 10 * 60 * 1000
    return Date.now() > (expiresAt - tenMinutes)
  }

  /**
   * Obtém o usuário atual
   */
  getCurrentUser(): AuthUser | null {
    try {
      const userData = localStorage.getItem(this.USER_KEY)
      return userData ? JSON.parse(userData) : null
    } catch {
      return null
    }
  }

  /**
   * Obtém o token de acesso
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY)
  }

  /**
   * Obtém o token de refresh
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY)
  }

  /**
   * Obtém o timestamp de expiração do token
   */
  getTokenExpiresAt(): number | null {
    const expiresAt = localStorage.getItem(this.EXPIRES_AT_KEY)
    return expiresAt ? parseInt(expiresAt, 10) : null
  }

  /**
   * Renova o token de acesso
   */
  async refreshToken(): Promise<string> {
    const refreshToken = this.getRefreshToken()
    if (!refreshToken) {
      throw new Error('Não há refresh token disponível')
    }

    try {
      const response = await apiClient.post<LoginResponse>('/api/auth/refresh', {
        refreshToken
      })

      // Atualizar tokens
      this.setTokens({
        accessToken: response.token,
        expiresAt: Date.now() + (response.expiresIn * 1000)
      })

      return response.token
    } catch (error) {
      // Se falhar, limpar dados e forçar novo login
      this.clearAuthData()
      throw error
    }
  }

  /**
   * Atualiza dados do usuário
   */
  async updateUserProfile(userData: Partial<AuthUser>): Promise<AuthUser> {
    const currentUser = this.getCurrentUser()
    if (!currentUser) {
      throw new Error('Usuário não autenticado')
    }

    try {
      await apiClient.put<any>(`/api/users/${currentUser.id}`, userData)
      const updatedUser = { ...currentUser, ...userData }
      this.setUser(updatedUser)
      return updatedUser
    } catch (error) {
      console.error('Erro ao atualizar perfil:', error)
      throw error
    }
  }

  /**
   * Atualiza status online/offline do usuário
   */
  async updateOnlineStatus(isOnline: boolean): Promise<void> {
    const user = this.getCurrentUser()
    if (!user) return

    try {
      await apiClient.put(`/api/users/${user.id}/status`, { isOnline })
      
      // Atualizar localmente
      const updatedUser = { ...user, isOnline }
      this.setUser(updatedUser)
    } catch (error) {
      console.error('Erro ao atualizar status online:', error)
    }
  }

  /**
   * Verifica permissões do usuário
   */
  hasPermission(requiredRole: 'ADMIN' | 'SUPERVISOR' | 'AGENT'): boolean {
    const user = this.getCurrentUser()
    if (!user) return false

    const roleHierarchy = {
      'ADMIN': 3,
      'SUPERVISOR': 2,
      'AGENT': 1
    }

    return roleHierarchy[user.role] >= roleHierarchy[requiredRole]
  }

  /**
   * Métodos privados para gerenciar tokens
   */
  private setTokens(tokens: AuthTokens): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, tokens.accessToken)
    localStorage.setItem(this.EXPIRES_AT_KEY, tokens.expiresAt.toString())
    
    if (tokens.refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, tokens.refreshToken)
    }
  }

  private setUser(user: AuthUser): void {
    // Store only essential user data to avoid large localStorage values
    const essentialUser = {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      department: user.department ? {
        id: user.department.id,
        name: user.department.name
      } : undefined,
      isOnline: user.isOnline,
      companyId: user.companyId,
      companySlug: user.companySlug
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(essentialUser))
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY)
    localStorage.removeItem(this.REFRESH_TOKEN_KEY)
    localStorage.removeItem(this.USER_KEY)
    localStorage.removeItem(this.EXPIRES_AT_KEY)
    localStorage.removeItem(`${this.COMPANY_KEY}_id`)
    localStorage.removeItem(`${this.COMPANY_KEY}_slug`)
  }

  private mapUserInfoToAuthUser(userInfo: LoginResponse['user'], companyId: string, companySlug: string): AuthUser {
    return {
      id: userInfo.id,
      name: userInfo.name,
      email: userInfo.email,
      role: userInfo.role,
      department: userInfo.departmentId ? {
        id: userInfo.departmentId,
        name: userInfo.departmentName || ''
      } : undefined,
      avatarUrl: userInfo.avatarUrl,
      isOnline: userInfo.isOnline,
      companyId: companyId,
      companySlug
    }
  }

  /**
   * Salva contexto da empresa de forma otimizada
   */
  private setCompanyContext(companyId: string, companySlug: string): void {
    // Store minimal company data
    localStorage.setItem(`${this.COMPANY_KEY}_id`, companyId)
    localStorage.setItem(`${this.COMPANY_KEY}_slug`, companySlug)
  }

  /**
   * Obtém contexto da empresa
   */
  getCompanyContext(): { companyId: string; companySlug: string } | null {
    try {
      const companyId = localStorage.getItem(`${this.COMPANY_KEY}_id`)
      const companySlug = localStorage.getItem(`${this.COMPANY_KEY}_slug`)
      
      if (companyId && companySlug) {
        return { companyId, companySlug }
      }
      return null
    } catch {
      return null
    }
  }

  /**
   * Verifica se o usuário tem acesso à empresa atual
   */
  hasCompanyAccess(): boolean {
    const user = this.getCurrentUser()
    const currentCompanySlug = getCurrentCompanySlug()
    
    if (!user) return false
    
    // Em desenvolvimento local, permitir acesso para localhost
    if (currentCompanySlug === 'localhost' || currentCompanySlug === '127.0.0.1') {
      return true
    }
    
    return user.companySlug === currentCompanySlug
  }

  /**
   * Limpa dados antigos para evitar problemas de header size
   */
  private cleanupLegacyData(): void {
    try {
      // Lista de chaves que podem estar causando problemas
      const keysToCheck = [
        this.COMPANY_KEY,
        'auth_company',
        'conversations', 
        'chat_state',
        'large_data'
      ];

      keysToCheck.forEach(key => {
        const value = localStorage.getItem(key);
        if (value && value.length > 1000) {
          console.log(`🧹 Removendo chave grande do localStorage: ${key} (${value.length} chars)`);
          localStorage.removeItem(key);
        }
      });

      // Remove old company format that might be large
      const oldCompany = localStorage.getItem(this.COMPANY_KEY);
      if (oldCompany) {
        localStorage.removeItem(this.COMPANY_KEY);
        // Try to migrate if it's valid JSON
        try {
          const parsed = JSON.parse(oldCompany);
          if (parsed.companyId && parsed.companySlug) {
            this.setCompanyContext(parsed.companyId, parsed.companySlug);
          }
        } catch {
          // Invalid data, just remove it
        }
      }

      // Força limpeza completa se ainda há problemas
      if (typeof window !== 'undefined') {
        const totalSize = new Blob(Object.values(localStorage)).size;
        if (totalSize > 50000) { // Se localStorage > 50KB
          console.log('🧹 localStorage muito grande, limpando dados desnecessários');
          Object.keys(localStorage).forEach(key => {
            if (!key.startsWith('auth_') && !key.startsWith('token_')) {
              localStorage.removeItem(key);
            }
          });
        }
      }
    } catch (error) {
      console.warn('Error cleaning legacy data:', error);
      // Em caso de erro, limpar tudo exceto dados essenciais de auth
      try {
        const essentialKeys = [this.ACCESS_TOKEN_KEY, this.USER_KEY, this.EXPIRES_AT_KEY];
        const backup: Record<string, string> = {};
        
        essentialKeys.forEach(key => {
          const value = localStorage.getItem(key);
          if (value) backup[key] = value;
        });
        
        localStorage.clear();
        
        Object.entries(backup).forEach(([key, value]) => {
          localStorage.setItem(key, value);
        });
        
        console.log('🧹 localStorage completamente limpo e refeito');
      } catch (e) {
        console.error('Erro crítico na limpeza:', e);
      }
    }
  }

  /**
   * Inicializa serviços automáticos
   */
  init(): void {
    // Clean up legacy data first
    this.cleanupLegacyData();
    // Auto refresh token quando necessário
    setInterval(() => {
      if (this.isAuthenticated() && this.isTokenExpiringSoon()) {
        this.refreshToken().catch(console.error)
      }
    }, 60000) // Verificar a cada minuto

    // Atualizar status online no início (temporariamente desabilitado)
    // if (this.isAuthenticated()) {
    //   this.updateOnlineStatus(true).catch(console.error)
    // }

    // Cleanup quando a página for fechada
    window.addEventListener('beforeunload', () => {
      if (this.isAuthenticated()) {
        // Usar navigator.sendBeacon para garantir que a requisição seja enviada
        const user = this.getCurrentUser()
        if (user) {
          navigator.sendBeacon(
            `${apiClient['baseURL']}/api/users/${user.id}/status`,
            JSON.stringify({ isOnline: false })
          )
        }
      }
    })
  }
}

export const authService = new AuthService()
export default authService