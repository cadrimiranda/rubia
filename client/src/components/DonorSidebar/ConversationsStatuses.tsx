import type { ChatStatus } from "../../types/index";

interface ConversationsStatusesProps {
  currentStatus: ChatStatus;
  onStatusChange: (status: ChatStatus) => void;
  statusStats?: {
    entrada: number;
    esperando: number;
    finalizados: number;
    total: number;
  };
}

const ConversationsStatuses: React.FC<ConversationsStatusesProps> = ({
  currentStatus,
  onStatusChange,
  statusStats,
}) => {
  return (
    <div className="bg-gray-50 rounded-md p-2 border border-gray-200 mb-3">
      <div className="text-xs font-semibold text-gray-600 mb-2">
        STATUS DAS CONVERSAS
      </div>
      <div className="flex gap-1">
        <button
          onClick={() => onStatusChange("ativos")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors flex items-center gap-1.5 ${
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
            currentStatus === "ativos"
              ? "bg-green-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Ativos
          {statusStats && statusStats.entrada > 0 && (
            <span className="bg-green-600 text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 font-semibold">
              {statusStats.entrada}
            </span>
          )}
        </button>
        <button
          onClick={() => onStatusChange("aguardando")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors flex items-center gap-1.5 ${
            currentStatus === "aguardando"
              ? "bg-yellow-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Aguardando
          {statusStats && statusStats.esperando > 0 && (
            <span className="bg-yellow-600 text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 font-semibold">
              {statusStats.esperando}
            </span>
          )}
        </button>
        <button
          onClick={() => onStatusChange("inativo")}
          className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors flex items-center gap-1.5 ${

            currentStatus === "inativo"
              ? "bg-red-500 text-white"
              : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          Inativo
          {statusStats && statusStats.finalizados > 0 && (
            <span className="bg-red-600 text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 font-semibold">
              {statusStats.finalizados}
            </span>
          )}

        </button>
      </div>
    </div>
  );
};

export { ConversationsStatuses };
