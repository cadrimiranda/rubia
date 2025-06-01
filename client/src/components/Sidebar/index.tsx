import { MessageCircle, Plus } from "lucide-react";
import SearchBar from "../SearchBar";
import TopTabsSwitcher from "../TopTabsSwitcher";
import ChatListItem from "../ChatListItem";
import { useChatStore } from "../../store/useChatStore";

const Sidebar = () => {
  const { getFilteredChats } = useChatStore();
  const filteredChats = getFilteredChats();

  return (
    <div className="h-full bg-white border-r border-gray-200 flex flex-col shadow-sm rounded-r-[32px]">
      {/* Header */}
      <div className="px-4 py-4 ">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-lg font-semibold text-gray-900">Conversas</h1>
          <button className="p-2 text-rose-500 hover:bg-rose-50 rounded-xl transition-all duration-200">
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
      <div className="flex-1 overflow-y-auto bg-gray-50/30">
        {filteredChats.length === 0 ? (
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
              <ChatListItem key={chat.id} chat={chat} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Sidebar;
