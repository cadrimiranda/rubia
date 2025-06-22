import { apiClient } from '../api/client'
import type { LoginRequest, LoginResponse, UserDTO } from '../api/types'
import { getCurrentCompanySlug, getCompanyFromSubdomain } from '../utils/company'
import { mockLogin, mockLogout, generateMockToken } from '../mocks/authMock'

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
   * Verifica se deve usar mock baseado na variável de ambiente
   */
  private get useMockAuth(): boolean {
    return import.meta.env.VITE_USE_MOCK_AUTH === 'true'
  }

  /**
   * Realiza login com email e senha com contexto da empresa
   */
  async login(credentials: LoginRequest): Promise<AuthUser> {
    try {
      if (this.useMockAuth) {
        console.log('🎭 Usando mock de autenticação');
        
        // Usar mock login
        const user = await mockLogin(credentials);
        
        // Gerar token mock
        const mockToken = generateMockToken(user);
        
        // Salvar tokens mock
        this.setTokens({
          accessToken: mockToken,
          expiresAt: Date.now() + (8 * 60 * 60 * 1000) // 8 horas
        });

        this.setUser(user);
        this.setCompanyContext(user.companyId, user.companySlug);

        return user;
      }

      // Login real com API
      console.log('🌐 Usando autenticação da API');
      
      // Obter contexto da empresa atual
      const companyInfo = getCompanyFromSubdomain()
      
      // Incluir company slug nas credenciais
      const loginData = {
        ...credentials,
        companySlug: companyInfo.slug
      }
      
      const response = await apiClient.post<LoginResponse>('/api/auth/login', loginData)
      
      // Salvar tokens
      this.setTokens({
        accessToken: response.token,
        expiresAt: Date.now() + (response.expiresIn * 1000)
      })

      // Converter UserDTO para AuthUser com company context
      const user = this.mapUserDtoToAuthUser(response.user, response.companyId, response.companySlug)
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
      if (this.useMockAuth) {
        console.log('🎭 Usando mock de logout');
        await mockLogout();
      } else {
        // Chamar endpoint de logout no backend (opcional)
        const token = this.getAccessToken()
        if (token) {
          await apiClient.post('/api/auth/logout')
        }
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
    
    // Para mock, sempre considerar empresa correta
    if (this.useMockAuth) {
      return isTokenValid
    }
    
    // Para API real, verificar se está na empresa correta
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
      const response = await apiClient.put<UserDTO>(`/api/users/${currentUser.id}`, userData)
      const updatedUser = this.mapUserDtoToAuthUser(response, currentUser.companyId, currentUser.companySlug)
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
    localStorage.setItem(this.USER_KEY, JSON.stringify(user))
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY)
    localStorage.removeItem(this.REFRESH_TOKEN_KEY)
    localStorage.removeItem(this.USER_KEY)
    localStorage.removeItem(this.EXPIRES_AT_KEY)
    localStorage.removeItem(this.COMPANY_KEY)
  }

  private mapUserDtoToAuthUser(dto: UserDTO, companyId: string, companySlug: string): AuthUser {
    return {
      id: dto.id,
      name: dto.name,
      email: dto.email,
      role: dto.role,
      department: dto.department ? {
        id: dto.department.id,
        name: dto.department.name
      } : undefined,
      avatarUrl: dto.avatarUrl,
      isOnline: dto.isOnline,
      companyId,
      companySlug
    }
  }

  /**
   * Salva contexto da empresa
   */
  private setCompanyContext(companyId: string, companySlug: string): void {
    const companyData = { companyId, companySlug }
    localStorage.setItem(this.COMPANY_KEY, JSON.stringify(companyData))
  }

  /**
   * Obtém contexto da empresa
   */
  getCompanyContext(): { companyId: string; companySlug: string } | null {
    try {
      const companyData = localStorage.getItem(this.COMPANY_KEY)
      return companyData ? JSON.parse(companyData) : null
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
    
    return user.companySlug === currentCompanySlug
  }

  /**
   * Inicializa serviços automáticos
   */
  init(): void {
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