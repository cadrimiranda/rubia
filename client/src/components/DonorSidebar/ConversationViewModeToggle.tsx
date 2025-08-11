import { Grid, List } from "lucide-react";
import type { ViewMode } from "../../types/types";

interface ConversationViewModeToggleProps {
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
}

const ConversationViewModeToggle: React.FC<ConversationViewModeToggleProps> = ({
  viewMode,
  onViewModeChange
}) => {
  return (
    <div className="flex items-center gap-0.5 bg-gray-100 rounded-md p-0.5">
      <button
        onClick={() => onViewModeChange("full")}
        className={`flex items-center gap-1 px-2 py-1 rounded text-xs font-medium transition-colors ${
          viewMode === "full"
            ? "bg-white text-gray-800 shadow-sm"
            : "text-gray-600 hover:text-gray-800"
        }`}
        title="Visualização completa"
      >
        <List className="w-3 h-3" />
        Completo
      </button>
      <button
        onClick={() => onViewModeChange("compact")}
        className={`flex items-center gap-1 px-2 py-1 rounded text-xs font-medium transition-colors ${
          viewMode === "compact"
            ? "bg-white text-gray-800 shadow-sm"
            : "text-gray-600 hover:text-gray-800"
        }`}
        title="Visualização compacta"
      >
        <Grid className="w-3 h-3" />
        Compacto
      </button>
    </div>
  );
};

export { ConversationViewModeToggle };