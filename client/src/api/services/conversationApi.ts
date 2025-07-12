import apiClient from '../client'
import type {
  ConversationDTO,
  ConversationFilters,
  CreateConversationRequest,
  UpdateConversationRequest,
  ConversationAssignResponse,
  PageResponse,
  ConversationStatus
} from '../types'

export class ConversationAPI {
  private basePath = '/api/conversations'

  async getAll(filters?: ConversationFilters): Promise<PageResponse<ConversationDTO>> {
    const params: Record<string, string> = {}
    
    if (filters?.page !== undefined) params.page = filters.page.toString()
    if (filters?.size !== undefined) params.size = filters.size.toString()
    if (filters?.sort) params.sort = filters.sort
    if (filters?.status) params.status = filters.status
    if (filters?.assignedUserId) params.assignedUserId = filters.assignedUserId
    if (filters?.departmentId) params.departmentId = filters.departmentId
    if (filters?.customerId) params.customerId = filters.customerId
    if (filters?.isPinned !== undefined) params.isPinned = filters.isPinned.toString()
    if (filters?.search) params.search = filters.search

    return apiClient.get<PageResponse<ConversationDTO>>(this.basePath, params)
  }

  async getByStatus(status: ConversationStatus, page = 0, size = 20): Promise<PageResponse<ConversationDTO>> {
    return this.getAll({ status, page, size, sort: 'updatedAt,desc' })
  }

  async getById(id: string): Promise<ConversationDTO> {
    return apiClient.get<ConversationDTO>(`${this.basePath}/${id}`)
  }

  async create(data: CreateConversationRequest): Promise<ConversationDTO> {
    return apiClient.post<ConversationDTO>(this.basePath, data)
  }

  async update(id: string, data: UpdateConversationRequest): Promise<ConversationDTO> {
    return apiClient.put<ConversationDTO>(`${this.basePath}/${id}`, data)
  }

  async delete(id: string): Promise<void> {
    return apiClient.delete<void>(`${this.basePath}/${id}`)
  }

  async changeStatus(id: string, status: ConversationStatus): Promise<ConversationDTO> {
    return apiClient.put<ConversationDTO>(`${this.basePath}/${id}/status?status=${status}`)
  }

  async assignToUser(id: string, userId: string): Promise<ConversationDTO> {
    return apiClient.put<ConversationDTO>(`${this.basePath}/${id}/assign/${userId}`)
  }

  async unassign(id: string): Promise<ConversationDTO> {
    // TODO: Implementar endpoint de unassign no backend se necessário
    return this.update(id, { assignedUserId: undefined })
  }

  async getAssignedToUser(userId: string, status?: ConversationStatus): Promise<PageResponse<ConversationDTO>> {
    return this.getAll({ 
      assignedUserId: userId, 
      status,
      sort: 'updatedAt,desc' 
    })
  }

  async search(query: string, filters?: Omit<ConversationFilters, 'search'>): Promise<PageResponse<ConversationDTO>> {
    return this.getAll({ ...filters, search: query })
  }

  async getByCustomer(customerId: string): Promise<ConversationDTO[]> {
    return apiClient.get<ConversationDTO[]>(`${this.basePath}/customer/${customerId}`)
  }

  async getByAssignedUser(userId: string): Promise<ConversationDTO[]> {
    return apiClient.get<ConversationDTO[]>(`${this.basePath}/user/${userId}`)
  }

  async getUnassigned(): Promise<ConversationDTO[]> {
    return apiClient.get<ConversationDTO[]>(`${this.basePath}/unassigned`)
  }

  async getStats(): Promise<{
    entrada: number
    esperando: number
    finalizados: number
    total: number
  }> {
    const [entrada, esperando, finalizados] = await Promise.all([
      apiClient.get<number>(`${this.basePath}/stats/count?status=ENTRADA`),
      apiClient.get<number>(`${this.basePath}/stats/count?status=ESPERANDO`),
      apiClient.get<number>(`${this.basePath}/stats/count?status=FINALIZADOS`)
    ])
    
    return {
      entrada,
      esperando,
      finalizados,
      total: entrada + esperando + finalizados
    }
  }
}

export const conversationApi = new ConversationAPI()
export default conversationApi