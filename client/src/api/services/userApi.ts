import apiClient from "../client";
import type {
  UserDTO,
  LoginRequest,
  LoginResponse,
  UserRole,
  PageResponse,
} from "../types";

export class UserAPI {
  private basePath = "/api/users";

  async clearQueue() {
    return apiClient.post("/api/secure/campaigns/queue/clean");
  }

  async login(credentials: LoginRequest): Promise<LoginResponse> {
    return apiClient.post<LoginResponse>("/api/auth/login", credentials);
  }

  async logout(): Promise<void> {
    return apiClient.post<void>("/api/auth/logout");
  }

  async getCurrentUser(): Promise<UserDTO> {
    return apiClient.get<UserDTO>("/api/auth/me");
  }

  async getAll(): Promise<PageResponse<UserDTO>> {
    return apiClient.get<PageResponse<UserDTO>>(this.basePath);
  }

  async getById(id: string): Promise<UserDTO> {
    return apiClient.get<UserDTO>(`${this.basePath}/${id}`);
  }

  async updateOnlineStatus(isOnline: boolean): Promise<UserDTO> {
    return apiClient.put<UserDTO>(`${this.basePath}/online-status`, {
      isOnline,
    });
  }

  async getAvailableAgents(departmentId?: string): Promise<UserDTO[]> {
    const params: Record<string, string> = { isOnline: "true" };
    if (departmentId) {
      params.departmentId = departmentId;
    }

    const response = await apiClient.get<PageResponse<UserDTO>>(
      `${this.basePath}/available-agents`,
      params
    );
    return response.content;
  }

  async getByDepartment(departmentId: string): Promise<UserDTO[]> {
    const response = await apiClient.get<PageResponse<UserDTO>>(this.basePath, {
      departmentId,
      size: "100",
    });
    return response.content;
  }

  async getByRole(role: UserRole): Promise<UserDTO[]> {
    const response = await apiClient.get<PageResponse<UserDTO>>(this.basePath, {
      role,
      size: "100",
    });
    return response.content;
  }

  async getOnlineUsers(): Promise<UserDTO[]> {
    const response = await apiClient.get<PageResponse<UserDTO>>(this.basePath, {
      isOnline: "true",
      size: "100",
    });
    return response.content;
  }

  async updateProfile(data: {
    name?: string;
    avatarUrl?: string;
  }): Promise<UserDTO> {
    return apiClient.put<UserDTO>(`${this.basePath}/profile`, data);
  }

  async changePassword(data: {
    currentPassword: string;
    newPassword: string;
  }): Promise<{ success: boolean }> {
    return apiClient.put<{ success: boolean }>(
      `${this.basePath}/change-password`,
      data
    );
  }

  async refreshToken(): Promise<{ token: string; expiresIn: number }> {
    return apiClient.post<{ token: string; expiresIn: number }>(
      "/api/auth/refresh"
    );
  }

  async getStats(): Promise<{
    total: number;
    online: number;
    byRole: Record<UserRole, number>;
    byDepartment: Record<string, number>;
  }> {
    return apiClient.get<{
      total: number;
      online: number;
      byRole: Record<UserRole, number>;
      byDepartment: Record<string, number>;
    }>(`${this.basePath}/stats`);
  }
}

export const userApi = new UserAPI();
export default userApi;
