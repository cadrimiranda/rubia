import { useEffect, useRef, useCallback, useState } from "react";
import { MessageCircle, Plus, Loader2, Heart } from "lucide-react";
import SearchBar from "../SearchBar";
import TopTabsSwitcher from "../TopTabsSwitcher";
import ChatListItem from "../ChatListItem";
import ComponentErrorBoundary from "../ComponentErrorBoundary";
import { ChatListSkeleton } from "../skeletons";
import NewConversationModal from "../NewConversationModal";
import { useChatStore } from "../../store/useChatStore";

const Sidebar = () => {
  const {
    getFilteredChats,
    loadConversations,
    currentStatus,
    currentPage,
    hasMore,
    isLoading,
  } = useChatStore();
  const filteredChats = getFilteredChats();
  const scrollRef = useRef<HTMLDivElement>(null);
  const loadingRef = useRef<HTMLDivElement>(null);
  const [isNewConversationModalOpen, setIsNewConversationModalOpen] =
    useState(false);

  // Load more conversations when scrolling to bottom
  const loadMore = useCallback(() => {
    if (!isLoading && hasMore) {
      loadConversations(currentStatus, currentPage + 1);
    }
  }, [isLoading, hasMore, currentStatus, currentPage, loadConversations]);

  // Intersection Observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      { threshold: 1.0 }
    );

    if (loadingRef.current) {
      observer.observe(loadingRef.current);
    }

    return () => observer.disconnect();
  }, [loadMore]);

  return (
    <div className="h-screen bg-white border-r border-gray-200 flex flex-col shadow-sm">
      {/* Header */}

      <div className="px-4 py-4 ">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-3">
            <Heart className="text-red-500" fill="currentColor" size={24} />
            <h1 className="text-lg font-semibold text-gray-900">
              Centro de Sangue
            </h1>
          </div>
          <button
            className="p-2 text-blue-500 hover:bg-blue-50 rounded-xl transition-all duration-200"
            onClick={() => setIsNewConversationModalOpen(true)}
          >
            <Plus size={18} />
          </button>
        </div>

        {/* Search */}
        <SearchBar />
      </div>

      {/* Status Tabs */}
      <div className="px-4 py-3 bg-gray-50/50">
        <TopTabsSwitcher />
      </div>

      {/* Chat List */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto bg-gray-50/30">
        {isLoading && filteredChats.length === 0 ? (
          <ChatListSkeleton count={8} />
        ) : filteredChats.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-40 text-gray-500 px-8">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
              <MessageCircle size={24} className="text-gray-400" />
            </div>
            <p className="text-sm font-medium mb-1">Nenhuma conversa</p>
            <p className="text-xs text-center">
              As conversas aparecerão aqui quando chegarem
            </p>
          </div>
        ) : (
          <div>
            {filteredChats.map((chat) => (
              <ComponentErrorBoundary
                key={chat.id}
                componentName="ChatListItem"
              >
                <ChatListItem chat={chat} />
              </ComponentErrorBoundary>
            ))}

            {/* Loading indicator for infinite scroll */}
            {hasMore && (
              <div
                ref={loadingRef}
                className="flex items-center justify-center py-4"
              >
                {isLoading && (
                  <div className="flex items-center space-x-2 text-gray-500">
                    <Loader2 size={16} className="animate-spin" />
                    <span className="text-sm">
                      Carregando mais conversas...
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* New Conversation Modal */}
      <NewConversationModal
        open={isNewConversationModalOpen}
        onClose={() => setIsNewConversationModalOpen(false)}
      />
    </div>
  );
};

export default Sidebar;
