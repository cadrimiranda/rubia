import type { ChatStatus } from "../../types/index";

interface ConversationsStatusesProps {
  currentStatus: ChatStatus;
  onStatusChange: (status: ChatStatus) => void;
}

const ConversationsStatuses: React.FC<ConversationsStatusesProps> = ({
  currentStatus,
  onStatusChange,
}) => {
  return (
    <div className="bg-gray-50 rounded-md p-2 border border-gray-200 mb-3">
      <div className="text-xs font-semibold text-gray-600 mb-2">
        STATUS DAS CONVERSAS
      </div>
      <div className="flex gap-1">
        <button
          onClick={() => onStatusChange("ativos")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
            currentStatus === "ativos"
              ? "bg-green-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Ativos
        </button>
        <button
          onClick={() => onStatusChange("aguardando")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
            currentStatus === "aguardando"
              ? "bg-yellow-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Aguardando
        </button>
        <button
          onClick={() => onStatusChange("inativo")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
            currentStatus === "inativo"
              ? "bg-red-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Inativo
        </button>
      </div>
    </div>
  );
};

export { ConversationsStatuses };
