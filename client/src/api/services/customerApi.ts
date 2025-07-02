import apiClient from '../client'
import type {
  CustomerDTO,
  CustomerFilters,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  PageResponse
} from '../types'
import { mockGetAllCustomers, mockFindCustomerByPhone, mockCreateCustomer } from '../../mocks/authMock'

export class CustomerAPI {
  private basePath = '/api/customers'
  
  /**
   * Verifica se deve usar mock baseado na variável de ambiente
   */
  private get useMockData(): boolean {
    return import.meta.env.VITE_USE_MOCK_DATA === 'true'
  }

  async getAll(filters?: CustomerFilters): Promise<PageResponse<CustomerDTO>> {
    if (this.useMockData) {
      console.log('🎭 Usando mock para getAll customers');
      return mockGetAllCustomers({ size: filters?.size, search: filters?.search });
    }

    const params: Record<string, string> = {}
    
    if (filters?.page !== undefined) params.page = filters.page.toString()
    if (filters?.size !== undefined) params.size = filters.size.toString()
    if (filters?.sort) params.sort = filters.sort
    if (filters?.isBlocked !== undefined) params.isBlocked = filters.isBlocked.toString()
    if (filters?.search) params.search = filters.search

    return apiClient.get<PageResponse<CustomerDTO>>(this.basePath, params)
  }

  async getById(id: string): Promise<CustomerDTO> {
    return apiClient.get<CustomerDTO>(`${this.basePath}/${id}`)
  }

  async create(data: CreateCustomerRequest): Promise<CustomerDTO> {
    if (this.useMockData) {
      console.log('🎭 Usando mock para create customer');
      return mockCreateCustomer(data);
    }

    return apiClient.post<CustomerDTO>(this.basePath, data)
  }

  async update(id: string, data: UpdateCustomerRequest): Promise<CustomerDTO> {
    return apiClient.put<CustomerDTO>(`${this.basePath}/${id}`, data)
  }

  async delete(id: string): Promise<void> {
    return apiClient.delete<void>(`${this.basePath}/${id}`)
  }

  async block(id: string): Promise<CustomerDTO> {
    return apiClient.put<CustomerDTO>(`${this.basePath}/${id}/block`)
  }

  async unblock(id: string): Promise<CustomerDTO> {
    return apiClient.put<CustomerDTO>(`${this.basePath}/${id}/unblock`)
  }

  async findByPhone(phone: string): Promise<CustomerDTO | null> {
    if (this.useMockData) {
      console.log('🎭 Usando mock para findByPhone customer');
      return mockFindCustomerByPhone(phone);
    }

    try {
      return await apiClient.get<CustomerDTO>(`${this.basePath}/phone/${encodeURIComponent(phone)}`)
    } catch (error: unknown) {
      if ((error as { status?: number }).status === 404) {
        return null
      }
      throw error
    }
  }

  async findByWhatsappId(whatsappId: string): Promise<CustomerDTO | null> {
    try {
      return await apiClient.get<CustomerDTO>(`${this.basePath}/whatsapp/${encodeURIComponent(whatsappId)}`)
    } catch (error: unknown) {
      if ((error as { status?: number }).status === 404) {
        return null
      }
      throw error
    }
  }

  async findOrCreateByPhone(phone: string, name?: string): Promise<CustomerDTO> {
    const existing = await this.findByPhone(phone)
    if (existing) {
      return existing
    }

    return this.create({ phone, name })
  }

  async search(query: string): Promise<CustomerDTO[]> {
    const response = await this.getAll({ search: query, size: 50 })
    return response.content
  }

  async getBlocked(): Promise<CustomerDTO[]> {
    const response = await this.getAll({ isBlocked: true, size: 100 })
    return response.content
  }

  async getActive(): Promise<PageResponse<CustomerDTO>> {
    return this.getAll({ 
      isBlocked: false, 
      sort: 'updatedAt,desc' 
    })
  }

  async getRecent(limit = 20): Promise<CustomerDTO[]> {
    const response = await this.getAll({
      page: 0,
      size: limit,
      sort: 'createdAt,desc',
      isBlocked: false
    })
    return response.content
  }

  async updateProfile(id: string, profileData: {
    name?: string
    profileUrl?: string
  }): Promise<CustomerDTO> {
    return this.update(id, profileData)
  }

  async getStats(): Promise<{
    total: number
    active: number
    blocked: number
    newThisMonth: number
  }> {
    return apiClient.get<{
      total: number
      active: number
      blocked: number
      newThisMonth: number
    }>(`${this.basePath}/stats`)
  }

  async getConversationHistory(id: string): Promise<{
    totalConversations: number
    activeConversations: number
    lastActivity: string
  }> {
    return apiClient.get<{
      totalConversations: number
      activeConversations: number
      lastActivity: string
    }>(`${this.basePath}/${id}/conversation-history`)
  }

  async validatePhone(phone: string): Promise<{ valid: boolean; formatted: string }> {
    return apiClient.post<{ valid: boolean; formatted: string }>(
      `${this.basePath}/validate-phone`,
      { phone }
    )
  }

  async bulkBlock(customerIds: string[]): Promise<{ blocked: number; errors: string[] }> {
    return apiClient.post<{ blocked: number; errors: string[] }>(
      `${this.basePath}/bulk-block`,
      { customerIds }
    )
  }

  async bulkUnblock(customerIds: string[]): Promise<{ unblocked: number; errors: string[] }> {
    return apiClient.post<{ unblocked: number; errors: string[] }>(
      `${this.basePath}/bulk-unblock`,
      { customerIds }
    )
  }
}

export const customerApi = new CustomerAPI()
export default customerApi