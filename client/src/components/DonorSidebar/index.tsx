import React from "react";
import { Heart, Loader, AlertCircle, RefreshCw, Settings } from "lucide-react";
import type { Donor, Campaign, ViewMode } from "../../types/types";
import type { ChatStatus } from "../../types/index";
import { DonorCard } from "./DonorCard";
import { CampaignSelector } from "./CampaignSelector";
import { ConversationsStatuses } from "./ConversationsStatuses";
import { SearchConversation } from "./SearchConversation";
import { ConversationViewModeToggle } from "./ConversationViewModeToggle";

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
  onConfigClick?: () => void;
  currentStatus?: ChatStatus;
  onStatusChange: (status: ChatStatus) => void;
  campaigns?: Campaign[];
  selectedCampaign?: Campaign | null;
  onCampaignChange: (campaign: Campaign | null) => void;
  viewMode?: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  hasMorePages?: boolean;
  isLoadingMore?: boolean;
  onLoadMore?: () => void;
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
  onConfigClick,
  currentStatus = "ativos",
  onStatusChange,
  campaigns = [],
  selectedCampaign = null,
  onCampaignChange,
  viewMode = "full",
  onViewModeChange,
  hasMorePages = false,
  isLoadingMore = false,
  onLoadMore,
}) => {
  const activeDonors = donors.filter(
    (d) => d.lastMessage || d.hasActiveConversation
  );

  const handleScroll = React.useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;

      if (scrollTop + clientHeight >= scrollHeight * 0.8) {
        if (hasMorePages && !isLoadingMore && onLoadMore) {
          onLoadMore();
        }
      }
    },
    [hasMorePages, isLoadingMore, onLoadMore]
  );

  const campaignFilteredDonors = selectedCampaign
    ? activeDonors.filter((donor) => donor.campaignId === selectedCampaign.id)
    : activeDonors;

  const filteredDonors = campaignFilteredDonors.filter((donor) =>
    donor.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="w-80 bg-white border-r border-gray-200 flex flex-col shadow-sm">
      <div className="p-3 bg-white border-b border-gray-200 shadow-sm">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Heart className="text-red-500 text-xl" fill="currentColor" />
            <h1 className="text-base font-semibold text-gray-800 m-0">
              Centro de Sangue
            </h1>
          </div>
          {onConfigClick && (
            <button
              onClick={onConfigClick}
              className="p-1.5 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-md transition-colors"
              title="Configurações"
            >
              <Settings className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Seletor de Campanha */}
        {campaigns.length > 0 && (
          <CampaignSelector
            campaigns={campaigns}
            selectedCampaign={selectedCampaign}
            onCampaignChange={onCampaignChange}
          />
        )}

        <ConversationsStatuses
          currentStatus={currentStatus}
          onStatusChange={onStatusChange}
        />
        <div className="space-y-2">
          <SearchConversation
            searchTerm={searchTerm}
            onSearchChange={onSearchChange}
            onNewChat={onNewChat}
          />

          <ConversationViewModeToggle
            viewMode={viewMode}
            onViewModeChange={onViewModeChange}
          />
        </div>
      </div>

      <div
        className="flex-1 overflow-y-auto p-2 bg-gray-50"
        onScroll={handleScroll}
      >
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
                : "Nenhuma conversa ativa"}
            </span>
          </div>
        ) : (
          filteredDonors.map((donor) => (
            <DonorCard
              donor={donor}
              selectedDonor={selectedDonor}
              viewMode={viewMode}
              currentStatus={currentStatus}
              campaigns={campaigns}
              onDonorSelect={onDonorSelect}
              onContextMenu={onContextMenu}
            />
          ))
        )}

        {/* Indicador de carregamento para paginação infinita */}
        {hasMorePages && isLoadingMore && (
          <div className="flex items-center justify-center py-4">
            <div className="flex items-center gap-2 text-gray-500">
              <Loader className="w-4 h-4 animate-spin" />
              <span className="text-sm">Carregando mais conversas...</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
