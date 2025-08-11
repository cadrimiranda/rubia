import { apiClient } from '../client';

export interface NotificationDTO {
  id: string;
  userId: string;
  conversationId: string;
  messageId: string;
  type: 'NEW_MESSAGE' | 'MESSAGE_REPLY' | 'CONVERSATION_ASSIGNED' | 'CONVERSATION_STATUS_CHANGED' | 'CAMPAIGN_MESSAGE' | 'SYSTEM_ALERT';
  status: 'UNREAD' | 'READ' | 'DISMISSED';
  title: string;
  content?: string;
  readAt?: string;
  companyId: string;
  createdAt: string;
  updatedAt?: string;
  customerName?: string;
  conversationTitle?: string;
  isRead: boolean;
}

export interface NotificationSummaryDTO {
  conversationId: string;
  conversationTitle: string;
  customerName?: string;
  count: number;
  lastNotification: string;
  lastMessageContent?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

class NotificationApiService {
  private readonly basePath = '/api/notifications';

  /**
   * Get all notifications for the current user
   */
  async getNotifications(page = 0, size = 20): Promise<PaginatedResponse<NotificationDTO>> {
    console.log('🌐 Fazendo request para:', `${this.basePath}?page=${page}&size=${size}`);
    const response = await apiClient.get<PaginatedResponse<NotificationDTO>>(
      `${this.basePath}?page=${page}&size=${size}`
    );
    console.log('📨 Response recebida (getNotifications):', response);
    return response.data;
  }

  /**
   * Get unread notifications for the current user
   */
  async getUnreadNotifications(page = 0, size = 20): Promise<PaginatedResponse<NotificationDTO>> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/unread?page=${page}&size=${size}`);
    const response = await apiClient.get<PaginatedResponse<NotificationDTO>>(
      `${this.basePath}/unread?page=${page}&size=${size}`
    );
    console.log('📨 Response recebida (getUnreadNotifications):', response);
    return response.data;
  }

  /**
   * Get notification count for a specific conversation
   */
  async getNotificationCountByConversation(conversationId: string): Promise<{ count: number }> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/count/conversation/${conversationId}`);
    const response = await apiClient.get<{ count: number }>(
      `${this.basePath}/count/conversation/${conversationId}`
    );
    console.log('📨 Response recebida (getNotificationCountByConversation):', response);
    return response.data;
  }

  /**
   * Get total unread notification count for the current user
   */
  async getTotalNotificationCount(): Promise<{ totalCount: number }> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/count`);
    const response = await apiClient.get<{ totalCount: number }>(
      `${this.basePath}/count`
    );
    console.log('📨 Response recebida (getTotalNotificationCount):', response);
    console.log('📨 Response data:', response.data);
    return response.data;
  }

  /**
   * Get notification summary grouped by conversation
   */
  async getNotificationSummary(): Promise<NotificationSummaryDTO[]> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/summary`);
    const response = await apiClient.get<NotificationSummaryDTO[]>(
      `${this.basePath}/summary`
    );
    console.log('📨 Response recebida (getNotificationSummary):', response);
    return response.data;
  }

  /**
   * Mark notifications as read for a specific conversation
   */
  async markConversationNotificationsAsRead(conversationId: string): Promise<void> {
    await apiClient.put(`${this.basePath}/conversation/${conversationId}/read`);
  }

  /**
   * Mark all notifications as read for the current user
   */
  async markAllNotificationsAsRead(): Promise<void> {
    await apiClient.put(`${this.basePath}/read-all`);
  }

  /**
   * Delete notifications for a specific conversation
   */
  async deleteConversationNotifications(conversationId: string): Promise<void> {
    await apiClient.delete(`${this.basePath}/conversation/${conversationId}`);
  }

  /**
   * Get notification counts for multiple conversations (batch request)
   */
  async getNotificationCountsForConversations(conversationIds: string[]): Promise<Record<string, number>> {
    console.log('🌐 Fazendo request para:', `${this.basePath}/counts/conversations`, 'com:', conversationIds);
    const response = await apiClient.post<Record<string, number>>(
      `${this.basePath}/counts/conversations`,
      conversationIds
    );
    console.log('📨 Response recebida (getNotificationCountsForConversations):', response);
    return response.data;
  }
}

export const notificationApi = new NotificationApiService();