import apiClient from '../client'
import type {
  MessageDTO,
  MessageFilters,
  CreateMessageRequest,
  MessageStatusResponse,
  PageResponse,
  MessageStatus,
  SearchResponse
} from '../types'

export class MessageAPI {
  private basePath = '/api/messages'

  async getByConversation(
    conversationId: string, 
    page = 0, 
    size = 50
  ): Promise<PageResponse<MessageDTO>> {
    return apiClient.get<PageResponse<MessageDTO>>(
      `/api/conversations/${conversationId}/messages`,
      {
        page: page.toString(),
        size: size.toString(),
        sort: 'createdAt,asc'
      }
    )
  }

  async getById(id: string): Promise<MessageDTO> {
    return apiClient.get<MessageDTO>(`${this.basePath}/${id}`)
  }

  async send(conversationId: string, data: CreateMessageRequest): Promise<MessageDTO> {
    return apiClient.post<MessageDTO>(
      `/api/conversations/${conversationId}/messages`,
      data
    )
  }

  async markAsRead(messageId: string): Promise<MessageStatusResponse> {
    return apiClient.put<MessageStatusResponse>(`${this.basePath}/${messageId}/read`)
  }

  async markAsDelivered(messageId: string): Promise<MessageStatusResponse> {
    return apiClient.put<MessageStatusResponse>(`${this.basePath}/${messageId}/delivered`)
  }

  async updateStatus(messageId: string, status: MessageStatus): Promise<MessageStatusResponse> {
    return apiClient.put<MessageStatusResponse>(`${this.basePath}/${messageId}/status`, {
      status
    })
  }

  async search(query: string, filters?: MessageFilters): Promise<SearchResponse<MessageDTO>> {
    const params: Record<string, string> = { q: query }
    
    if (filters?.conversationId) params.conversationId = filters.conversationId
    if (filters?.senderType) params.senderType = filters.senderType
    if (filters?.messageType) params.messageType = filters.messageType
    if (filters?.startDate) params.startDate = filters.startDate
    if (filters?.endDate) params.endDate = filters.endDate
    if (filters?.page !== undefined) params.page = filters.page.toString()
    if (filters?.size !== undefined) params.size = filters.size.toString()

    return apiClient.get<SearchResponse<MessageDTO>>(`${this.basePath}/search`, params)
  }

  async getAll(filters?: MessageFilters): Promise<PageResponse<MessageDTO>> {
    const params: Record<string, string> = {}
    
    if (filters?.page !== undefined) params.page = filters.page.toString()
    if (filters?.size !== undefined) params.size = filters.size.toString()
    if (filters?.sort) params.sort = filters.sort
    if (filters?.conversationId) params.conversationId = filters.conversationId
    if (filters?.senderType) params.senderType = filters.senderType
    if (filters?.messageType) params.messageType = filters.messageType
    if (filters?.startDate) params.startDate = filters.startDate
    if (filters?.endDate) params.endDate = filters.endDate

    return apiClient.get<PageResponse<MessageDTO>>(this.basePath, params)
  }

  async delete(messageId: string): Promise<void> {
    return apiClient.delete<void>(`${this.basePath}/${messageId}`)
  }

  async getUnreadCount(conversationId?: string): Promise<{ count: number }> {
    const params = conversationId ? { conversationId } : undefined
    return apiClient.get<{ count: number }>(`${this.basePath}/unread-count`, params)
  }

  async markConversationAsRead(conversationId: string): Promise<{ updated: number }> {
    return apiClient.put<{ updated: number }>(
      `/api/conversations/${conversationId}/messages/mark-all-read`
    )
  }

  async getRecent(limit = 20): Promise<MessageDTO[]> {
    const response = await this.getAll({
      page: 0,
      size: limit,
      sort: 'createdAt,desc'
    })
    return response.content
  }

  async getByExternalId(externalMessageId: string): Promise<MessageDTO | null> {
    try {
      return await apiClient.get<MessageDTO>(`${this.basePath}/external/${externalMessageId}`)
    } catch (error: unknown) {
      if ((error as { status?: number }).status === 404) {
        return null
      }
      throw error
    }
  }

  async uploadMedia(file: File): Promise<{ url: string; type: string }> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch('/api/media/upload', {
      method: 'POST',
      body: formData,
      headers: {
        Authorization: `Bearer ${localStorage.getItem('auth_token')}`,
      },
    })

    if (!response.ok) {
      throw new Error('Falha no upload do arquivo')
    }

    return response.json()
  }

  async sendMedia(
    conversationId: string,
    file: File,
    caption?: string
  ): Promise<MessageDTO> {
    const { url, type } = await this.uploadMedia(file)
    
    return this.send(conversationId, {
      content: caption || '',
      messageType: type.startsWith('image/') ? 'IMAGE' : 
                   type.startsWith('audio/') ? 'AUDIO' : 'FILE',
      mediaUrl: url
    })
  }

  async getMediaUrl(messageId: string): Promise<{ url: string; expiresAt: string }> {
    return apiClient.get<{ url: string; expiresAt: string }>(
      `${this.basePath}/${messageId}/media-url`
    )
  }

  async getDraftMessages(conversationId: string): Promise<MessageDTO[]> {
    const response = await apiClient.get<MessageDTO[]>(
      `/api/conversations/${conversationId}/messages`,
      {
        status: 'DRAFT',
        size: '10'
      }
    )
    return response
  }

  async updateMessageStatus(messageId: string, status: MessageStatus): Promise<MessageDTO> {
    return apiClient.put<MessageDTO>(`${this.basePath}/${messageId}/status`, {
      status
    })
  }
}

export const messageApi = new MessageAPI()
export default messageApi