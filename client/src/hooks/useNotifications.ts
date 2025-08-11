import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { notificationApi, type NotificationSummaryDTO } from '../api/services/notificationApi';

export interface ConversationNotification {
  conversationId: string;
  donorId: string;
  donorName: string;
  lastMessageId: string;
  timestamp: number;
  count: number;
}

export interface NotificationStorage {
  [userId: string]: {
    [conversationId: string]: ConversationNotification;
  };
}

export const useNotifications = () => {
  const { user } = useAuthStore();
  const [notificationSummary, setNotificationSummary] = useState<NotificationSummaryDTO[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  // Load notification summary from backend
  const loadNotificationSummary = useCallback(async () => {
    if (!user?.id) return;

    try {
      setIsLoading(true);
      console.log('🔄 Carregando resumo de notificações do backend...');
      
      const [summary, countData] = await Promise.all([
        notificationApi.getNotificationSummary(),
        notificationApi.getTotalNotificationCount(),
      ]);
      
      console.log('📊 Resumo recebido:', summary);
      console.log('📈 Count data recebido:', countData);
      console.log('📈 Total count recebido:', countData?.totalCount);
      
      setNotificationSummary(summary || []);
      setTotalCount(countData?.totalCount || 0);
    } catch (error) {
      console.error('❌ Error loading notification summary:', error);
    } finally {
      setIsLoading(false);
    }
  }, [user?.id]);

  // Initial load
  useEffect(() => {
    if (user?.id) {
      loadNotificationSummary();
    }
  }, [user?.id, loadNotificationSummary]);

  // Listen for WebSocket notification events
  useEffect(() => {
    const handleNewNotification = () => {
      // Refresh notifications when new notification arrives
      loadNotificationSummary();
    };

    const handleNotificationCountUpdate = () => {
      // Refresh notifications when count is updated
      loadNotificationSummary();
    };

    // Add event listeners
    window.addEventListener('notification:new', handleNewNotification);
    window.addEventListener('notification:count-update', handleNotificationCountUpdate);

    // Cleanup
    return () => {
      window.removeEventListener('notification:new', handleNewNotification);
      window.removeEventListener('notification:count-update', handleNotificationCountUpdate);
    };
  }, [loadNotificationSummary]);

  // Add or update a notification (not needed anymore - handled by backend)
  const addNotification = useCallback(() => {
    // Backend handles notification creation automatically
    // Just refresh the summary
    loadNotificationSummary();
  }, [loadNotificationSummary]);

  // Remove notification for a conversation (when user views the conversation)
  const removeNotification = useCallback(async (conversationId: string) => {
    if (!user?.id) return;

    try {
      await notificationApi.markConversationNotificationsAsRead(conversationId);
      // Refresh the summary after marking as read
      await loadNotificationSummary();
    } catch (error) {
      console.error('Error marking notifications as read:', error);
    }
  }, [user?.id, loadNotificationSummary]);

  // Clear all notifications
  const clearAllNotifications = useCallback(async () => {
    if (!user?.id) return;

    try {
      await notificationApi.markAllNotificationsAsRead();
      // Refresh the summary after marking all as read
      await loadNotificationSummary();
    } catch (error) {
      console.error('Error marking all notifications as read:', error);
    }
  }, [user?.id, loadNotificationSummary]);

  // Get notification count for a specific conversation
  const getNotificationCount = useCallback((conversationId: string): number => {
    const summary = notificationSummary.find(s => s.conversationId === conversationId);
    return summary ? summary.count : 0;
  }, [notificationSummary]);

  // Check if a conversation has notifications
  const hasNotification = useCallback((conversationId: string): boolean => {
    return getNotificationCount(conversationId) > 0;
  }, [getNotificationCount]);

  // Get total notification count across all conversations
  const getTotalNotificationCount = useCallback((): number => {
    return totalCount;
  }, [totalCount]);

  // Get all notifications sorted by timestamp (newest first)
  const getAllNotifications = useCallback((): ConversationNotification[] => {
    return notificationSummary.map(summary => ({
      conversationId: summary.conversationId,
      donorId: summary.conversationId, // Using conversationId as fallback
      donorName: summary.customerName || summary.conversationTitle,
      lastMessageId: '', // Not available in summary
      timestamp: new Date(summary.lastNotification).getTime(),
      count: summary.count,
    }));
  }, [notificationSummary]);

  // Refresh notifications (useful for manual refresh)
  const refreshNotifications = useCallback(() => {
    loadNotificationSummary();
  }, [loadNotificationSummary]);

  return {
    notifications: notificationSummary,
    addNotification,
    removeNotification,
    clearAllNotifications,
    getNotificationCount,
    hasNotification,
    getTotalNotificationCount,
    getAllNotifications,
    refreshNotifications,
    isLoading,
  };
};