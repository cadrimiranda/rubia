import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface AIAgent {
  id: string
  companyId: string
  companyName: string
  name: string
  description?: string
  avatarBase64?: string
  aiModelId: string
  aiModelName: string
  aiModelDisplayName: string
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
  avatarBase64?: string
  aiModelId: string
  temperament: string
  maxResponseLength?: number
  temperature?: number
  isActive?: boolean
}

export interface UpdateAIAgentDTO {
  name?: string
  description?: string
  avatarBase64?: string
  aiModelId?: string
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
    const token = localStorage.getItem('auth_token')
    const user = localStorage.getItem('auth_user')
    const companyId = localStorage.getItem('auth_company_id')
    
    console.log('🔍 [DEBUG] Auth headers debug:', {
      hasToken: !!token,
      tokenLength: token?.length,
      hasUser: !!user,
      hasCompanyId: !!companyId,
      tokenPreview: token?.substring(0, 20) + '...'
    })
    
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

  // Get AI Agents by model ID
  async getAIAgentsByModelId(modelId: string): Promise<AIAgent[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/model-id/${encodeURIComponent(modelId)}`,
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

  // Check if company can create more AI agents
  async canCreateAgent(companyId: string): Promise<boolean> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/can-create`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get remaining agent slots for company
  async getRemainingAgentSlots(companyId: string): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/remaining-slots`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Debug context
  async debugContext(): Promise<any> {
    console.log('🔍 [DEBUG] Calling debug context endpoint...');
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-agents/debug/context`,
      { headers: this.getAuthHeaders() }
    )
    console.log('🔍 [DEBUG] Context response:', response.data);
    return response.data
  }

  // Enhance message using company's AI agent
  async enhanceMessage(companyId: string, message: string): Promise<string> {
    console.log('🔮 [AI] Enhancing message for company:', companyId);
    const response = await axios.post(
      `${API_BASE_URL}/api/ai-agents/company/${companyId}/enhance-message`,
      { message },
      { headers: this.getAuthHeaders() }
    )
    return response.data.enhancedMessage
  }

}

export const aiAgentApi = new AIAgentApi()
export default aiAgentApi