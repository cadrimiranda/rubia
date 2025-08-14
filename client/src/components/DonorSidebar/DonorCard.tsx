import React from "react";
import { User, Circle } from "lucide-react";
import type { Donor, Campaign, ViewMode } from "../../types/types";
import type { ChatStatus } from "../../types/index";
import { getStatusColor, calculateAge } from "../../utils";
import { useChatStore } from "../../store/useChatStore";
import { useUnreadCounts } from "../../hooks/useUnreadCounts";

interface DonorCardProps {
  donor: Donor;
  selectedDonor: Donor | null;
  viewMode: ViewMode;
  currentStatus: ChatStatus | null;
  campaigns: Campaign[];
  onDonorSelect: (donor: Donor) => void;
  onContextMenu: (e: React.MouseEvent, donorId: string) => void;
}

export const DonorCard: React.FC<DonorCardProps> = ({
  donor,
  selectedDonor,
  viewMode,
  currentStatus,
  campaigns,
  onDonorSelect,
  onContextMenu,
}) => {
  const { markAsRead } = useUnreadCounts();
  const { unreadCount } = useChatStore();
  const isSelected = selectedDonor?.id === donor.id;
  const cardClassName = `cursor-pointer transition-all duration-200 shadow-sm border ${
    isSelected
      ? "bg-blue-50 border-blue-200 shadow-md"
      : "bg-white border-gray-200 hover:bg-gray-50 hover:shadow-md hover:border-gray-300"
  }`;

  const displayCount = unreadCount[donor.conversationId];
  const hasUnreadMessages = displayCount > 0;

  const handleSelectConversation = () => {
    markAsRead(donor.conversationId);
    onDonorSelect(donor);
  };

  if (viewMode === "compact") {
    return (
      <div
        key={donor.id}
        onClick={handleSelectConversation}
        onContextMenu={(e) => onContextMenu(e, donor.id)}
        className={`p-2.5 mb-1.5 rounded-md ${cardClassName}`}
      >
        <div className="flex items-center gap-2.5">
          {/* Avatar compacto */}
          <div className="relative flex-shrink-0">
            <div className="w-9 h-9 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-sm">
              <User className="w-4 h-4 text-white" />
            </div>
            <Circle
              className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 ${getStatusColor(
                donor.status
              )} bg-white rounded-xl border-2 border-white`}
              fill="currentColor"
            />
          </div>

          {/* Info principal */}
          <div className="flex-1 min-w-0 space-y-0.5">
            <div className="flex items-center justify-between">
              <h4 className="font-semibold text-gray-900 text-sm truncate">
                {donor.name}
              </h4>
              <div className="flex items-center gap-2 flex-shrink-0">
                <span className="text-xs text-gray-500 font-medium">
                  {donor.timestamp}
                </span>
                {hasUnreadMessages && (
                  <div className="bg-red-500 text-white text-xs rounded-xl min-w-[16px] h-3.5 flex items-center justify-center px-1 font-semibold">
                    {displayCount}
                  </div>
                )}
              </div>
            </div>

            {/* Badges compactas */}
            <div className="flex items-center gap-1 flex-wrap">
              <span className="inline-flex items-center text-xs font-medium text-blue-700 bg-blue-100 px-1.5 py-0.5 rounded border border-blue-200">
                {donor.bloodType}
              </span>

              {donor.campaignId && (
                <span
                  className="inline-flex items-center text-xs font-medium px-1.5 py-0.5 rounded text-white"
                  style={{
                    backgroundColor:
                      campaigns.find((c) => c.id === donor.campaignId)?.color ||
                      "#6b7280",
                  }}
                >
                  {campaigns
                    .find((c) => c.id === donor.campaignId)
                    ?.name?.split(" ")[0] || "Camp"}
                </span>
              )}
            </div>

            {/* Última mensagem compacta */}
            <p
              className="text-xs text-gray-600 leading-tight overflow-hidden"
              style={{
                display: "-webkit-box",
                WebkitLineClamp: 1,
                WebkitBoxOrient: "vertical" as const,
                maxHeight: "1.1rem",
              }}
            >
              {donor.lastMessage}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Visualização Completa (original)
  return (
    <div
      key={donor.id}
      onClick={handleSelectConversation}
      onContextMenu={(e) => onContextMenu(e, donor.id)}
      className={`p-3 mb-2 rounded-lg ${cardClassName}`}
    >
      <div className="space-y-2.5">
        {/* Header: Avatar + Nome + Timestamp + Unread */}
        <div className="flex items-center gap-2.5">
          <div className="relative flex-shrink-0">
            <div className="w-11 h-11 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-sm">
              <User className="w-5 h-5 text-white" />
            </div>
            <Circle
              className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 ${getStatusColor(
                donor.status
              )} bg-white rounded-xl border-2 border-white`}
              fill="currentColor"
            />
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <h4 className="font-semibold text-gray-900 text-sm truncate">
                {donor.name}
              </h4>
              <div className="flex items-center gap-2 flex-shrink-0">
                <span className="text-xs text-gray-500 font-medium">
                  {donor.timestamp}
                </span>
                {hasUnreadMessages && (
                  <div className="bg-red-500 text-white text-xs rounded-xl min-w-[20px] h-5 flex items-center justify-center px-1.5 font-semibold shadow-sm">
                    {displayCount}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="inline-flex items-center text-xs font-semibold text-blue-700 bg-blue-100 px-2 py-0.5 rounded-md border border-blue-200">
            {donor.bloodType}
          </span>

          {donor.campaignId && (
            <span
              className="inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-md text-white shadow-sm"
              style={{
                backgroundColor:
                  campaigns.find((c) => c.id === donor.campaignId)?.color ||
                  "#6b7280",
              }}
            >
              {campaigns.find((c) => c.id === donor.campaignId)?.name ||
                "Campanha"}
            </span>
          )}

          {currentStatus && (
            <span
              className={`inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-md border ${
                currentStatus === "ativos"
                  ? "text-green-700 bg-green-50 border-green-200"
                  : currentStatus === "aguardando"
                  ? "text-yellow-700 bg-yellow-50 border-yellow-200"
                  : "text-red-700 bg-red-50 border-red-200"
              }`}
            >
              {currentStatus === "ativos"
                ? "Ativo"
                : currentStatus === "aguardando"
                ? "Aguardando"
                : "Inativo"}
            </span>
          )}
        </div>

        <div className="bg-gray-50 rounded-md p-2.5 border border-gray-100">
          <p
            className="text-sm text-gray-700 leading-relaxed overflow-hidden"
            style={{
              display: "-webkit-box",
              WebkitLineClamp: 2,
              WebkitBoxOrient: "vertical" as const,
              maxHeight: "2.6rem",
            }}
          >
            {donor.lastMessage}
          </p>
        </div>

        {/* Info Adicional */}
        <div className="flex items-center justify-between text-xs text-gray-500 pt-1 border-t border-gray-100">
          <span className="font-medium">
            {donor.birthDate
              ? `${calculateAge(donor.birthDate)} anos`
              : "Idade N/I"}
          </span>
          <span>{donor.totalDonations} doações</span>
          <span>Última: {donor.lastDonation}</span>
        </div>
      </div>
    </div>
  );
};
