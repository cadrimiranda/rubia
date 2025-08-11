import { apiClient } from '../client';

class UnreadCountApiService {
  private readonly basePath = '/api/unread-counts';

  /**
   * Get unread count for a specific conversation
   */
  async getUnreadCount(conversationId: string): Promise<{ count: number }> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/conversation/${conversationId}`);
    const response = await apiClient.get<{ count: number }>(
      `${this.basePath}/conversation/${conversationId}`
    );
    console.log('📨 Response recebida (getUnreadCount):', response.data);
    return response.data;
  }

  /**
   * Get total unread count for the current user
   */
  async getTotalUnreadCount(): Promise<{ totalCount: number }> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/total`);
    const response = await apiClient.get<{ totalCount: number }>(
      `${this.basePath}/total`
    );
    console.log('📨 Response recebida (getTotalUnreadCount):', response.data);
    return response.data;
  }

  /**
   * Get unread counts for multiple conversations (batch request)
   */
  async getUnreadCountsForConversations(conversationIds: string[]): Promise<Record<string, number>> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/conversations`, 'com:', conversationIds);
    const response = await apiClient.post<Record<string, number>>(
      `${this.basePath}/conversations`,
      conversationIds
    );
    console.log('📨 Response recebida (getUnreadCountsForConversations):', response.data);
    return response.data;
  }

  /**
   * Mark conversation as read (reset counter)
   */
  async markAsRead(conversationId: string): Promise<void> {
    console.log('🌐 Marking conversation as read:', conversationId);
    await apiClient.put(`${this.basePath}/conversation/${conversationId}/read`);
    console.log('✅ Conversation marked as read:', conversationId);
  }

  /**
   * Mark all conversations as read for the current user
   */
  async markAllAsRead(): Promise<void> {
    console.log('🌐 Marking all conversations as read');
    await apiClient.put(`${this.basePath}/read-all`);
    console.log('✅ All conversations marked as read');
  }
}

export const unreadCountApi = new UnreadCountApiService();