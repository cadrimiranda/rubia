import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface AILog {
  id: string
  companyId: string
  companyName: string
  aiAgentId: string
  aiAgentName: string
  userId?: string
  userName?: string
  conversationId?: string
  messageId?: string
  messageTemplateId?: string
  messageTemplateName?: string
  requestPrompt: string
  rawResponse?: string
  processedResponse?: string
  tokensUsedInput?: number
  tokensUsedOutput?: number
  estimatedCost?: number
  status: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'TIMEOUT'
  errorMessage?: string
  createdAt: string
}

export interface CreateAILogDTO {
  companyId: string
  aiAgentId: string
  userId?: string
  conversationId?: string
  messageId?: string
  messageTemplateId?: string
  requestPrompt: string
  rawResponse?: string
  processedResponse?: string
  tokensUsedInput?: number
  tokensUsedOutput?: number
  estimatedCost?: number
  status: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'TIMEOUT'
  errorMessage?: string
}

export interface UpdateAILogDTO {
  rawResponse?: string
  processedResponse?: string
  tokensUsedOutput?: number
  estimatedCost?: number
  status?: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'TIMEOUT'
  errorMessage?: string
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

class AILogApi {
  private getAuthHeaders() {
    const token = localStorage.getItem('authToken')
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    }
  }

  // Create a new AI Log
  async createAILog(data: CreateAILogDTO): Promise<AILog> {
    const response = await axios.post(
      `${API_BASE_URL}/api/ai-logs`,
      data,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Log by ID
  async getAILogById(id: string): Promise<AILog> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/${id}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get all AI Logs with pagination
  async getAllAILogs(params?: {
    page?: number
    size?: number
    sortBy?: string
    sortDir?: 'asc' | 'desc'
  }): Promise<PaginatedResponse<AILog>> {
    const queryParams = new URLSearchParams()
    
    if (params?.page !== undefined) queryParams.append('page', params.page.toString())
    if (params?.size !== undefined) queryParams.append('size', params.size.toString())
    if (params?.sortBy) queryParams.append('sortBy', params.sortBy)
    if (params?.sortDir) queryParams.append('sortDir', params.sortDir)

    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs?${queryParams.toString()}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Logs by company ID
  async getAILogsByCompany(companyId: string): Promise<AILog[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/company/${companyId}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Logs by status
  async getAILogsByStatus(status: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'TIMEOUT'): Promise<AILog[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/status/${status}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Logs by AI Agent ID
  async getAILogsByAIAgent(aiAgentId: string): Promise<AILog[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/ai-agent/${aiAgentId}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Update AI Log
  async updateAILog(id: string, data: UpdateAILogDTO): Promise<AILog> {
    const response = await axios.put(
      `${API_BASE_URL}/api/ai-logs/${id}`,
      data,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Delete AI Log
  async deleteAILog(id: string): Promise<void> {
    await axios.delete(
      `${API_BASE_URL}/api/ai-logs/${id}`,
      { headers: this.getAuthHeaders() }
    )
  }

  // Get total cost by company
  async getTotalCostByCompany(companyId: string): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/company/${companyId}/total-cost`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get total tokens used by company
  async getTotalTokensUsedByCompany(companyId: string): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/company/${companyId}/total-tokens`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Count AI Logs by company and status
  async countAILogsByCompanyAndStatus(
    companyId: string,
    status: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'TIMEOUT'
  ): Promise<number> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/company/${companyId}/count?status=${status}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }

  // Get AI Logs by date range
  async getAILogsByDateRange(startDate: string, endDate: string): Promise<AILog[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/ai-logs/date-range?startDate=${startDate}&endDate=${endDate}`,
      { headers: this.getAuthHeaders() }
    )
    return response.data
  }
}

export const aiLogApi = new AILogApi()
export default aiLogApi