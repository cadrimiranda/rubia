import React from "react";
import { Search, Plus, User, Circle, Heart, Loader, AlertCircle, RefreshCw } from "lucide-react";
import type { Donor } from "../../types/types";
import { getStatusColor } from "../../utils";

interface DonorSidebarProps {
  donors: Donor[];
  selectedDonor: Donor | null;
  searchTerm: string;
  onSearchChange: (term: string) => void;
  onDonorSelect: (donor: Donor) => void;
  onNewChat: () => void;
  onContextMenu: (e: React.MouseEvent, donorId: string) => void;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

export const DonorSidebar: React.FC<DonorSidebarProps> = ({
  donors,
  selectedDonor,
  searchTerm,
  onSearchChange,
  onDonorSelect,
  onNewChat,
  onContextMenu,
  isLoading = false,
  error = null,
  onRetry,
}) => {
  const activeDonors = donors.filter((d) => d.lastMessage || d.hasActiveConversation);
  const filteredDonors = activeDonors.filter((donor) =>
    donor.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="w-80 bg-gray-50 border-r border-gray-200 flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center gap-3 mb-4">
          <Heart className="text-red-500 text-2xl" fill="currentColor" />
          <h1 className="text-lg font-semibold text-gray-800 m-0">
            Centro de Sangue
          </h1>
        </div>

        <div className="flex gap-2 mb-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <input
              type="text"
              placeholder="Buscar conversas..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <button
            onClick={onNewChat}
            className="bg-blue-500 hover:bg-blue-600 text-white p-2 rounded-lg transition-colors"
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-32 text-gray-500">
            <Loader className="w-6 h-6 animate-spin mb-2" />
            <span className="text-sm">Carregando conversas...</span>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center h-32 text-gray-500 p-4">
            <AlertCircle className="w-6 h-6 text-red-500 mb-2" />
            <span className="text-sm text-center mb-3">{error}</span>
            {onRetry && (
              <button
                onClick={onRetry}
                className="flex items-center gap-2 px-3 py-1 bg-blue-500 text-white text-sm rounded-lg hover:bg-blue-600 transition-colors"
              >
                <RefreshCw className="w-4 h-4" />
                Tentar novamente
              </button>
            )}
          </div>
        ) : filteredDonors.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-32 text-gray-500">
            <Heart className="w-6 h-6 mb-2" />
            <span className="text-sm">
              {donors.length === 0 
                ? "Nenhuma conversa encontrada" 
                : searchTerm 
                ? "Nenhuma conversa encontrada para sua busca" 
                : "Nenhuma conversa ativa"
              }
            </span>
          </div>
        ) : (
          filteredDonors.map((donor) => (
            <div
              key={donor.id}
              onClick={() => onDonorSelect(donor)}
              onContextMenu={(e) => onContextMenu(e, donor.id)}
              className={`p-3 mb-1 rounded-lg cursor-pointer transition-all duration-200 ${
                selectedDonor?.id === donor.id
                  ? "bg-blue-50 border border-blue-200"
                  : "hover:bg-gray-100"
              }`}
            >
              <div className="flex items-start gap-3">
                <div className="relative">
                  <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                    <User className="w-5 h-5 text-white" />
                  </div>
                  <Circle
                    className={`absolute -bottom-1 -right-1 w-3 h-3 ${getStatusColor(
                      donor.status
                    )} bg-white rounded-full`}
                    fill="currentColor"
                  />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-medium text-gray-800 text-sm truncate">
                      {donor.name}
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-gray-500">
                        {donor.timestamp}
                      </span>
                      {donor.unread > 0 && (
                        <div className="bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                          {donor.unread}
                        </div>
                      )}
                    </div>
                  </div>

                  <p className="text-xs text-gray-500 truncate mb-1 m-0">
                    {donor.lastMessage}
                  </p>

                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-1 rounded">
                      {donor.bloodType}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
