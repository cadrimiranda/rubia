import { apiClient } from "../client";

class UnreadCountApiService {
  private readonly basePath = "/api/unread-counts";

  /**
   * Mark conversation as read (reset counter)
   */
  async markAsRead(conversationId: string): Promise<void> {
    console.log("🌐 Marking conversation as read:", conversationId);
    await apiClient.put(`${this.basePath}/conversation/${conversationId}/read`);
    console.log("✅ Conversation marked as read:", conversationId);
  }
}

export const unreadCountApi = new UnreadCountApiService();
