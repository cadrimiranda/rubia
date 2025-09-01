import { useCallback } from "react";

import { unreadCountApi } from "../api/services/unreadCountApi";
import { useChatStore } from "../store/useChatStore";

export const useUnreadCounts = () => {

  const store = useChatStore();

  const markAsRead = useCallback(
    async (conversationId: string) => {

      try {
        await unreadCountApi.markAsRead(conversationId);
        store.updateUnreadCount(conversationId, 0);
      } catch (error) {
        console.error("❌ Error marking conversation as read:", error);
      }
    },
    [store]

  );

  return {
    markAsRead,
  };
};
