import { useCallback } from "react";
import { useAuthStore } from "../store/useAuthStore";
import { unreadCountApi } from "../api/services/unreadCountApi";
import { useChatStore } from "../store/useChatStore";

export const useUnreadCounts = () => {
  const { user } = useAuthStore();
  const store = useChatStore();

  const markAsRead = useCallback(
    async (conversationId: string) => {
      if (!user?.id) return;

      try {
        await unreadCountApi.markAsRead(conversationId);
        store.updateUnreadCount(conversationId, 0);
      } catch (error) {
        console.error("❌ Error marking conversation as read:", error);
      }
    },
    [store, user]
  );

  return {
    markAsRead,
  };
};
