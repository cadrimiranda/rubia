import { useState, useEffect, useCallback } from "react";
import { useAuthStore } from "../store/useAuthStore";
import { unreadCountApi } from "../api/services/unreadCountApi";

export const useUnreadCounts = () => {
  const { user } = useAuthStore();
  const [conversationCounts, setConversationCounts] = useState<
    Record<string, number>
  >({});
  const [totalCount, setTotalCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const loadTotalCount = useCallback(async () => {
    if (!user?.id) return;

    try {
      const result = await unreadCountApi.getTotalUnreadCount();
      const count = result?.totalCount || 0;
      setTotalCount(count);
    } catch (error) {
      console.error("❌ Error loading total unread count:", error);
      setTotalCount(0);
    }
  }, [user?.id]);

  // Load unread count for specific conversation
  const loadConversationCount = useCallback(
    async (conversationId: string) => {
      if (!user?.id) return;

      try {
        const result = await unreadCountApi.getUnreadCount(conversationId);

        setConversationCounts((prev) => ({
          ...prev,
          [conversationId]: result.count,
        }));
      } catch (error) {
        console.error(
          `❌ Error loading count for conversation ${conversationId}:`,
          error
        );
      }
    },
    [user?.id]
  );

  // Load counts for multiple conversations (batch)
  const loadConversationCounts = useCallback(
    async (conversationIds: string[]) => {
      if (!user?.id || conversationIds.length === 0) return;

      try {
        setIsLoading(true);
        const results = await unreadCountApi.getUnreadCountsForConversations(
          conversationIds
        );

        setConversationCounts((prev) => ({
          ...prev,
          ...results,
        }));

        // Recalculate total
        const newTotal = Object.values(results).reduce(
          (sum, count) => sum + count,
          0
        );
        setTotalCount(newTotal);
      } catch (error) {
        console.error("❌ Error loading conversation counts:", error);
      } finally {
        setIsLoading(false);
      }
    },
    [user?.id]
  );

  // Mark conversation as read
  const markAsRead = useCallback(
    async (conversationId: string) => {
      if (!user?.id) return;

      try {
        await unreadCountApi.markAsRead(conversationId);

        setConversationCounts((prev) => ({
          ...prev,
          [conversationId]: 0,
        }));
      } catch (error) {
        console.error("❌ Error marking conversation as read:", error);
      }
    },
    [user?.id]
  );

  // Mark all as read
  const markAllAsRead = useCallback(async () => {
    if (!user?.id) return;

    try {
      await unreadCountApi.markAllAsRead();

      // Reset all local counts
      setConversationCounts({});
      setTotalCount(0);
    } catch (error) {
      console.error("❌ Error marking all as read:", error);
    }
  }, [user?.id]);

  // Get count for specific conversation
  const getUnreadCount = useCallback(
    (conversationId: string): number => {
      return conversationCounts[conversationId] || 0;
    },
    [conversationCounts]
  );

  // Check if conversation has unread messages
  const hasUnreadMessages = useCallback(
    (conversationId: string): boolean => {
      return getUnreadCount(conversationId) > 0;
    },
    [getUnreadCount]
  );

  // Refresh all counts
  const refreshCounts = useCallback(() => {
    loadTotalCount();
  }, [loadTotalCount]);

  // Initial load - disabled temporarily until backend is ready
  useEffect(() => {
    if (user?.id) {
      loadTotalCount();
    }
  }, [user?.id, loadTotalCount]);

  // Listen for WebSocket events
  useEffect(() => {
    const handleUnreadCountUpdate = () => {
      loadTotalCount();
    };

    // Add event listeners
    window.addEventListener("unread:count-update", handleUnreadCountUpdate);

    // Cleanup
    return () => {
      window.removeEventListener(
        "unread:count-update",
        handleUnreadCountUpdate
      );
    };
  }, [loadTotalCount]);

  return {
    conversationCounts,
    totalCount,
    isLoading,
    getUnreadCount,
    hasUnreadMessages,
    loadConversationCount,
    loadConversationCounts,
    markAsRead,
    markAllAsRead,
    refreshCounts,
  };
};
