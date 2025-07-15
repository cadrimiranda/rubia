import { getCurrentCompanySlug } from "../utils/company";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
}

export interface ApiError {
  message: string;
  status: number;
  code?: string;
}

class ApiClient {
  private baseURL: string;
  private tokenRefreshPromise: Promise<string> | null = null;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    skipAuth = false
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;

    const config: RequestInit = {
      headers: {
        ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
        ...options.headers,
      },
      ...options,
    };

    // Adicionar token de autorização e contexto da empresa se disponível e não for para pular auth
    if (!skipAuth) {
      const token = this.getAuthToken();
      if (token) {
        // Limitar tamanho do token para evitar erro 431
        const sanitizedToken = token.substring(0, 500);
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${sanitizedToken}`,
        };
      }

      // Adicionar contexto da empresa para multi-tenant
      const companySlug = localStorage.getItem('auth_company_slug') || getCurrentCompanySlug();
      if (companySlug) {
        // Limitar tamanho do header para evitar erro 431
        const sanitizedSlug = companySlug.substring(0, 50);
        config.headers = {
          ...config.headers,
          "X-Company-Slug": sanitizedSlug,
        };
      }
      
      // Debug para upload de mídia
      if (endpoint.includes('/media')) {
        console.log('🔍 Media upload request:', {
          endpoint,
          hasToken: !!token,
          companySlug,
          tokenPrefix: token ? token.substring(0, 20) + '...' : 'none',
          headers: Object.keys(config.headers || {}),
          isFormData: config.body instanceof FormData
        });
      }
    }

    // Debug: verificar tamanho dos headers
    const headerSize = JSON.stringify(config.headers || {}).length;
    if (headerSize > 1000) {
      console.warn(`⚠️ Headers muito grandes: ${headerSize} bytes`, config.headers);
    }

    try {
      const response = await fetch(url, config);

      // Interceptar erro 401 (não autorizado) para renovar token
      if (
        response.status === 401 &&
        !skipAuth &&
        !endpoint.includes("/auth/")
      ) {
        try {
          const newToken = await this.handleTokenRefresh();

          // Tentar novamente com o novo token
          config.headers = {
            ...config.headers,
            Authorization: `Bearer ${newToken}`,
          };

          const retryResponse = await fetch(url, config);
          if (retryResponse.ok) {
            const contentType = retryResponse.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
              return await retryResponse.json();
            }
            return retryResponse.text() as unknown as T;
          }
        } catch (refreshError) {
          console.warn("Falha ao renovar token:", refreshError);
          this.handleAuthError();
          throw {
            message: "Sessão expirada. Faça login novamente.",
            status: 401,
            code: "TOKEN_EXPIRED",
          } as ApiError;
        }
      }

      if (!response.ok) {
        const error: ApiError = {
          message: `HTTP ${response.status}: ${response.statusText}`,
          status: response.status,
        };

        // Tentar extrair mensagem de erro do body
        try {
          const errorData = await response.json();
          error.message = errorData.message || error.message;
          error.code = errorData.code;
          
          // Debug especial para 403 em uploads de mídia
          if (response.status === 403 && endpoint.includes('/media')) {
            console.error('❌ 403 Forbidden on media upload:', {
              endpoint,
              errorData,
              headers: Object.keys(config.headers || {}),
              companySlug: localStorage.getItem('auth_company_slug'),
              tokenExists: !!localStorage.getItem('auth_token')
            });
          }
        } catch (parseError) {
          // Tentar ler como texto se não for JSON
          try {
            const errorText = await response.text();
            console.error('❌ Error response (text):', errorText);
            error.message = errorText || error.message;
          } catch {
            // Manter mensagem padrão se não conseguir parsear nada
          }
        }

        // Se for erro 403, tratar como falta de permissão
        if (response.status === 403) {
          error.message = "Você não tem permissão para realizar esta ação";
          error.code = "INSUFFICIENT_PERMISSIONS";
        }

        throw error;
      }

      const contentType = response.headers.get("content-type");
      if (contentType && contentType.includes("application/json")) {
        return await response.json();
      }

      return response.text() as unknown as T;
    } catch (error) {
      if (error instanceof Error && error.name === "TypeError") {
        throw {
          message: "Erro de conexão com o servidor",
          status: 0,
          code: "NETWORK_ERROR",
        } as ApiError;
      }
      throw error;
    }
  }

  private getAuthToken(): string | null {
    return localStorage.getItem("auth_token");
  }

  private async handleTokenRefresh(): Promise<string> {
    // Evitar múltiplas tentativas de refresh simultâneas
    if (this.tokenRefreshPromise) {
      return this.tokenRefreshPromise;
    }

    this.tokenRefreshPromise = this.performTokenRefresh();

    try {
      const newToken = await this.tokenRefreshPromise;
      return newToken;
    } finally {
      this.tokenRefreshPromise = null;
    }
  }

  private async performTokenRefresh(): Promise<string> {
    const refreshToken = localStorage.getItem("refresh_token");
    if (!refreshToken) {
      throw new Error("Não há refresh token disponível");
    }

    // Fazer chamada de refresh sem auth para evitar loop
    const response = await this.request<{ token: string; expiresIn: number }>(
      "/api/auth/refresh",
      {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      },
      true // skipAuth = true
    );

    // Salvar novo token
    localStorage.setItem("auth_token", response.token);
    localStorage.setItem(
      "token_expires_at",
      (Date.now() + response.expiresIn * 1000).toString()
    );

    return response.token;
  }

  private handleAuthError(): void {
    // Limpar dados de auth incluindo company context (formato antigo e novo)
    localStorage.removeItem("auth_token");
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("auth_user");
    localStorage.removeItem("token_expires_at");
    localStorage.removeItem("auth_company");
    localStorage.removeItem("auth_company_id");
    localStorage.removeItem("auth_company_slug");

    // Notificar store de auth (se disponível)
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("auth:logout"));
    }
  }

  async get<T>(endpoint: string, params?: Record<string, string>): Promise<T> {
    const url = new URL(`${this.baseURL}${endpoint}`);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.append(key, value);
      });
    }

    return this.request<T>(url.pathname + url.search);
  }

  async post<T>(endpoint: string, data?: unknown): Promise<T> {
    const isFormData = data instanceof FormData;
    
    return this.request<T>(endpoint, {
      method: "POST",
      body: isFormData ? data : (data ? JSON.stringify(data) : undefined),
      // Don't set Content-Type for FormData - let browser set it with boundary
      ...(isFormData ? {} : { headers: { "Content-Type": "application/json" } }),
    });
  }

  async put<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async patch<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, {
      method: "DELETE",
    });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
export default apiClient;
