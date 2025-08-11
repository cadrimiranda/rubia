import { Search, Plus } from "lucide-react";

interface SearchConversationProps {
  searchTerm: string;
  onSearchChange: (term: string) => void;
  onNewChat: () => void;
}

const SearchConversation: React.FC<SearchConversationProps> = ({
  searchTerm,
  onSearchChange,
  onNewChat
}) => {
  return (
    <div className="flex gap-2">
      <div className="relative flex-1">
        <Search className="absolute left-2.5 top-1/2 transform -translate-y-1/2 text-gray-400 w-3.5 h-3.5" />
        <input
          type="text"
          placeholder="Buscar doador..."
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm text-sm bg-white"
        />
      </div>
      <button
        onClick={onNewChat}
        className="bg-blue-500 hover:bg-blue-600 text-white p-2 rounded-md transition-colors shadow-sm flex items-center justify-center min-w-[36px]"
        title="Nova conversa"
      >
        <Plus className="w-3.5 h-3.5" />
      </button>
    </div>
  );
};

export { SearchConversation };