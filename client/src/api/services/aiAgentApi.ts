import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export interface AIAgent {
  id: string
  companyId: string
  companyName: string
  name: string
  description?: string
  avatarUrl?: string
  aiModelType: string
  temperament: string
  maxResponseLength: number
  temperature: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateAIAgentDTO {
  companyId: string
  name: string
  description?: string
  avatarUrl?: string
  aiModelType: string
  temperament: string
  maxResponseLength?: number
  temperature?: number
  isActive?: boolean
}

export interface UpdateAIAgentDTO {
  name?: string
  description?: string
  avatarUrl?: string
  aiModelType?: string
  temperament?: string
  maxResponseLength?: number
  temperature?: number
  isActive?: boolean
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

class AIAgentApi {
  private getAuthHeaders() {
    const token = localStorage.getItem('authToken')
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    }
  }

  // Create a new AI Agent
  async createAIAgent(data: CreateAIAgentDTO): Promise<AIAgent> {
    const response = await axios.post(
      `${API_BASE_URL}/api/ai-agents`,
      data,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Agent by ID
  async getAIAgentById(id: string): Promise<AIAgent> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/${id}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get all AI Agents with pagination
  async getAllAIAgents(params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: 'asc' | 'desc'
  }): Promise<PaginatedResponse<AIAgent>> {
    const queryParams = new URLSearchParams()
    
    if (params?.page !== undefined) queryParams.append('page', params.page.toString())
    if (params?.size !== undefined) queryParams.append('size', params.size.toString())
    if (params?.sortBy) queryParams.append('sortBy', params.sortBy)
    if (params?.sortDir) queryParams.append('sortDir', params.sortDir)

    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents?${queryParams.toString()}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Agents by company ID
  async getAIAgentsByCompany(companyId: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get active AI Agents by company ID
  async getActiveAIAgentsByCompany(companyId: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/active`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Agents by company ID ordered by name
  async getAIAgentsByCompanyOrderedByName(companyId: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/ordered`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Update AI Agent
  async updateAIAgent(id: string, data: UpdateAIAgentDTO): Promise<AIAgent> {
    const response = await axios.put(
      `${API_BASE_URL}/api/ai-agents/${id}`,
      data,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Delete AI Agent
  async deleteAIAgent(id: string): Promise<void> {
    await axios.delete(
      `${API_BASE_URL}/api/ai-agents/${id}`,
      { headers: this.getAuthHeaders() }
    )
  }

  // Count AI Agents by company
  async countAIAgentsByCompany(companyId: string): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/count`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Count active AI Agents by company
  async countActiveAIAgentsByCompany(companyId: string): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/count/active`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Check if AI Agent exists by name and company
  async checkAIAgentExists(companyId: string, name: string): Promise<boolean> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/exists?name=${encodeURIComponent(name)}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Agents by model type
  async getAIAgentsByModelType(modelType: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/model-type/${encodeURIComponent(modelType)}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Agents by temperament
  async getAIAgentsByTemperament(temperament: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/temperament/${encodeURIComponent(temperament)}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }
}

export const aiAgentApi = new AIAgentApi()
export default aiAgentApi