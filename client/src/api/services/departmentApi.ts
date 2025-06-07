import apiClient from '../client'
import type {
  DepartmentDTO,
  PageResponse
} from '../types'

export interface CreateDepartmentRequest {
  name: string
  description?: string
  autoAssign?: boolean
}

export interface UpdateDepartmentRequest {
  name?: string
  description?: string
  autoAssign?: boolean
}

export class DepartmentAPI {
  private basePath = '/api/departments'

  async getAll(): Promise<PageResponse<DepartmentDTO>> {
    return apiClient.get<PageResponse<DepartmentDTO>>(this.basePath)
  }

  async getById(id: string): Promise<DepartmentDTO> {
    return apiClient.get<DepartmentDTO>(`${this.basePath}/${id}`)
  }

  async create(data: CreateDepartmentRequest): Promise<DepartmentDTO> {
    return apiClient.post<DepartmentDTO>(this.basePath, data)
  }

  async update(id: string, data: UpdateDepartmentRequest): Promise<DepartmentDTO> {
    return apiClient.put<DepartmentDTO>(`${this.basePath}/${id}`, data)
  }

  async delete(id: string): Promise<void> {
    return apiClient.delete<void>(`${this.basePath}/${id}`)
  }

  async getWithAutoAssign(): Promise<DepartmentDTO[]> {
    const response = await apiClient.get<PageResponse<DepartmentDTO>>(this.basePath, {
      autoAssign: 'true'
    })
    return response.content
  }

  async getStats(id: string): Promise<{
    userCount: number
    activeConversations: number
    todayConversations: number
    avgResponseTime: number
  }> {
    return apiClient.get<{
      userCount: number
      activeConversations: number
      todayConversations: number
      avgResponseTime: number
    }>(`${this.basePath}/${id}/stats`)
  }

  async getAllStats(): Promise<Record<string, {
    userCount: number
    activeConversations: number
    todayConversations: number
    avgResponseTime: number
  }>> {
    return apiClient.get<Record<string, {
      userCount: number
      activeConversations: number
      todayConversations: number
      avgResponseTime: number
    }>>(`${this.basePath}/stats`)
  }
}

export const departmentApi = new DepartmentAPI()
export default departmentApi