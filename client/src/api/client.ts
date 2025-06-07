const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

export interface ApiError {
  message: string
  status: number
  code?: string
}

class ApiClient {
  private baseURL: string
  private tokenRefreshPromise: Promise<string> | null = null

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    skipAuth = false
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`
    
    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    }

    // Adicionar token de autorização se disponível e não for para pular auth
    if (!skipAuth) {
      const token = this.getAuthToken()
      if (token) {
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${token}`,
        }
      }
    }

    try {
      const response = await fetch(url, config)

      // Interceptar erro 401 (não autorizado) para renovar token
      if (response.status === 401 && !skipAuth && !endpoint.includes('/auth/')) {
        try {
          const newToken = await this.handleTokenRefresh()
          
          // Tentar novamente com o novo token
          config.headers = {
            ...config.headers,
            Authorization: `Bearer ${newToken}`,
          }
          
          const retryResponse = await fetch(url, config)
          if (retryResponse.ok) {
            const contentType = retryResponse.headers.get('content-type')
            if (contentType && contentType.includes('application/json')) {
              return await retryResponse.json()
            }
            return retryResponse.text() as unknown as T
          }
        } catch (refreshError) {
          console.warn('Falha ao renovar token:', refreshError)
          this.handleAuthError()
          throw {
            message: 'Sessão expirada. Faça login novamente.',
            status: 401,
            code: 'TOKEN_EXPIRED'
          } as ApiError
        }
      }

      if (!response.ok) {
        const error: ApiError = {
          message: `HTTP ${response.status}: ${response.statusText}`,
          status: response.status,
        }

        // Tentar extrair mensagem de erro do body
        try {
          const errorData = await response.json()
          error.message = errorData.message || error.message
          error.code = errorData.code
        } catch {
          // Manter mensagem padrão se não conseguir parsear JSON
        }

        // Se for erro 403, tratar como falta de permissão
        if (response.status === 403) {
          error.message = 'Você não tem permissão para realizar esta ação'
          error.code = 'INSUFFICIENT_PERMISSIONS'
        }

        throw error
      }

      const contentType = response.headers.get('content-type')
      if (contentType && contentType.includes('application/json')) {
        return await response.json()
      }

      return response.text() as unknown as T
    } catch (error) {
      if (error instanceof Error && error.name === 'TypeError') {
        throw {
          message: 'Erro de conexão com o servidor',
          status: 0,
          code: 'NETWORK_ERROR'
        } as ApiError
      }
      throw error
    }
  }

  private getAuthToken(): string | null {
    return localStorage.getItem('auth_token')
  }

  private async handleTokenRefresh(): Promise<string> {
    // Evitar múltiplas tentativas de refresh simultâneas
    if (this.tokenRefreshPromise) {
      return this.tokenRefreshPromise
    }

    this.tokenRefreshPromise = this.performTokenRefresh()
    
    try {
      const newToken = await this.tokenRefreshPromise
      return newToken
    } finally {
      this.tokenRefreshPromise = null
    }
  }

  private async performTokenRefresh(): Promise<string> {
    const refreshToken = localStorage.getItem('refresh_token')
    if (!refreshToken) {
      throw new Error('Não há refresh token disponível')
    }

    // Fazer chamada de refresh sem auth para evitar loop
    const response = await this.request<{ token: string; expiresIn: number }>(
      '/api/auth/refresh',
      {
        method: 'POST',
        body: JSON.stringify({ refreshToken })
      },
      true // skipAuth = true
    )

    // Salvar novo token
    localStorage.setItem('auth_token', response.token)
    localStorage.setItem('token_expires_at', (Date.now() + (response.expiresIn * 1000)).toString())

    return response.token
  }

  private handleAuthError(): void {
    // Limpar dados de auth
    localStorage.removeItem('auth_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('token_expires_at')

    // Notificar store de auth (se disponível)
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('auth:logout'))
    }
  }

  async get<T>(endpoint: string, params?: Record<string, string>): Promise<T> {
    const url = new URL(`${this.baseURL}${endpoint}`)
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.append(key, value)
      })
    }

    return this.request<T>(url.pathname + url.search)
  }

  async post<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async put<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async patch<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'DELETE',
    })
  }
}

export const apiClient = new ApiClient(API_BASE_URL)
export default apiClient