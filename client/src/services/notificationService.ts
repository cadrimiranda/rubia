import type { ConversationNotification, NotificationStorage } from '../hooks/useNotifications';

const NOTIFICATIONS_STORAGE_KEY = 'rubia_message_notifications';

class NotificationService {
  private listeners: Set<() => void> = new Set();

  private getStoredNotifications(): NotificationStorage {
    try {
      const stored = localStorage.getItem(NOTIFICATIONS_STORAGE_KEY);
      return stored ? JSON.parse(stored) : {};
    } catch (error) {
      console.error('Error loading notifications from localStorage:', error);
      return {};
    }
  }

  private saveNotifications(notifications: NotificationStorage): void {
    try {
      localStorage.setItem(NOTIFICATIONS_STORAGE_KEY, JSON.stringify(notifications));
      this.notifyListeners();
    } catch (error) {
      console.error('Error saving notifications to localStorage:', error);
    }
  }

  private notifyListeners(): void {
    this.listeners.forEach(listener => listener());
  }

  addListener(listener: () => void): void {
    this.listeners.add(listener);
  }

  removeListener(listener: () => void): void {
    this.listeners.delete(listener);
  }

  addNotification(
    userId: string,
    conversationId: string,
    donorId: string,
    donorName: string,
    messageId: string,
    timestamp: number
  ): void {
    const allNotifications = this.getStoredNotifications();
    
    if (!allNotifications[userId]) {
      allNotifications[userId] = {};
    }

    const existing = allNotifications[userId][conversationId];
    allNotifications[userId][conversationId] = {
      conversationId,
      donorId,
      donorName,
      lastMessageId: messageId,
      timestamp: Math.max(timestamp, existing?.timestamp || 0),
      count: existing ? existing.count + 1 : 1,
    };

    this.saveNotifications(allNotifications);
  }

  removeNotification(userId: string, conversationId: string): void {
    const allNotifications = this.getStoredNotifications();
    
    if (allNotifications[userId]) {
      delete allNotifications[userId][conversationId];
      this.saveNotifications(allNotifications);
    }
  }

  getNotificationCount(userId: string, conversationId: string): number {
    const allNotifications = this.getStoredNotifications();
    return allNotifications[userId]?.[conversationId]?.count || 0;
  }

  hasNotification(userId: string, conversationId: string): boolean {
    const allNotifications = this.getStoredNotifications();
    return !!allNotifications[userId]?.[conversationId];
  }

  getUserNotifications(userId: string): Record<string, ConversationNotification> {
    const allNotifications = this.getStoredNotifications();
    return allNotifications[userId] || {};
  }

  clearAllNotifications(userId: string): void {
    const allNotifications = this.getStoredNotifications();
    if (allNotifications[userId]) {
      allNotifications[userId] = {};
      this.saveNotifications(allNotifications);
    }
  }

  getTotalNotificationCount(userId: string): number {
    const userNotifications = this.getUserNotifications(userId);
    return Object.values(userNotifications).reduce((total, notif) => total + notif.count, 0);
  }
}

export const notificationService = new NotificationService();