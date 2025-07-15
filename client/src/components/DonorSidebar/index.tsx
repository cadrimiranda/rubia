import React from "react";
import {
  Search,
  Plus,
  User,
  Circle,
  Heart,
  Loader,
  AlertCircle,
  RefreshCw,
  Settings,
  Filter,
  Grid,
  List,
} from "lucide-react";
import { Select } from "antd";
import type { Donor, Campaign, ViewMode } from "../../types/types";
import type { ChatStatus } from "../../types/index";
import { getStatusColor, calculateAge } from "../../utils";

const { Option } = Select;

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
  onStatusChange?: (status: ChatStatus) => void;
  campaigns?: Campaign[];
  selectedCampaign?: Campaign | null;
  onCampaignChange?: (campaign: Campaign | null) => void;
  viewMode?: ViewMode;
  onViewModeChange?: (mode: ViewMode) => void;
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
}) => {
  const activeDonors = donors.filter(
    (d) => d.lastMessage || d.hasActiveConversation
  );
  const filteredDonors = activeDonors.filter((donor) =>
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
          <div className="mb-3 p-2 bg-gray-50 rounded-md border border-gray-200">
            <div className="flex items-center gap-2 mb-2">
              <Filter className="w-3.5 h-3.5 text-blue-600" />
              <span className="text-xs font-semibold text-gray-800">
                Filtrar por Campanha
              </span>
            </div>
            <Select
              value={selectedCampaign?.id || "all"}
              onChange={(value) => {
                if (onCampaignChange) {
                  const campaign =
                    value === "all"
                      ? null
                      : campaigns.find((c) => c.id === value) || null;
                  onCampaignChange(campaign);
                }
              }}
              className="w-full"
              size="small"
            >
              <Option value="all">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-gray-400"></div>
                  <span>Todas as campanhas</span>
                </div>
              </Option>
              {campaigns.map((campaign) => (
                <Option key={campaign.id} value={campaign.id}>
                  <div className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: campaign.color }}
                    ></div>
                    <span>{campaign.name}</span>
                  </div>
                </Option>
              ))}
            </Select>
          </div>
        )}

        <div className="bg-gray-50 rounded-md p-2 border border-gray-200 mb-3">
          <div className="text-xs font-semibold text-gray-600 mb-2">
            STATUS DAS CONVERSAS
          </div>
          <div className="flex gap-1">
            <button
              onClick={() => onStatusChange?.("ativos")}
              className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                currentStatus === "ativos"
                  ? "bg-green-500 text-white"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
            >
              Ativos
            </button>
            <button
              onClick={() => onStatusChange?.("aguardando")}
              className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                currentStatus === "aguardando"
                  ? "bg-yellow-500 text-white"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
            >
              Aguardando
            </button>
            <button
              onClick={() => onStatusChange?.("inativo")}
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
        <div className="space-y-2">
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

          {/* Toggle de Visualização */}
          {onViewModeChange && (
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
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2 bg-gray-50">
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
          filteredDonors.map((donor) =>
            viewMode === "compact" ? (
              // Visualização Compacta
              <div
                key={donor.id}
                onClick={() => onDonorSelect(donor)}
                onContextMenu={(e) => onContextMenu(e, donor.id)}
                className={`p-2.5 mb-1.5 rounded-md cursor-pointer transition-all duration-200 shadow-sm border ${
                  selectedDonor?.id === donor.id
                    ? "bg-blue-50 border-blue-200 shadow-md"
                    : "bg-white border-gray-200 hover:bg-gray-50 hover:shadow-md hover:border-gray-300"
                }`}
              >
                <div className="flex items-center gap-2.5">
                  {/* Avatar compacto */}
                  <div className="relative flex-shrink-0">
                    <div className="w-9 h-9 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center shadow-sm">
                      <User className="w-4 h-4 text-white" />
                    </div>
                    <Circle
                      className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 ${getStatusColor(
                        donor.status
                      )} bg-white rounded-full border-2 border-white`}
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
                        {donor.unread > 0 && (
                          <div className="bg-red-500 text-white text-xs rounded-full min-w-[16px] h-3.5 flex items-center justify-center px-1 font-semibold">
                            {donor.unread}
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
                              campaigns.find((c) => c.id === donor.campaignId)
                                ?.color || "#6b7280",
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
            ) : (
              // Visualização Completa (original)
              <div
                key={donor.id}
                onClick={() => onDonorSelect(donor)}
                onContextMenu={(e) => onContextMenu(e, donor.id)}
                className={`p-3 mb-2 rounded-lg cursor-pointer transition-all duration-200 shadow-sm border ${
                  selectedDonor?.id === donor.id
                    ? "bg-blue-50 border-blue-200 shadow-md"
                    : "bg-white border-gray-200 hover:bg-gray-50 hover:shadow-md hover:border-gray-300"
                }`}
              >
                <div className="space-y-2.5">
                  {/* Header: Avatar + Nome + Timestamp + Unread */}
                  <div className="flex items-center gap-2.5">
                    <div className="relative flex-shrink-0">
                      <div className="w-11 h-11 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center shadow-sm">
                        <User className="w-5 h-5 text-white" />
                      </div>
                      <Circle
                        className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 ${getStatusColor(
                          donor.status
                        )} bg-white rounded-full border-2 border-white`}
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
                          {donor.unread > 0 && (
                            <div className="bg-red-500 text-white text-xs rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5 font-semibold shadow-sm">
                              {donor.unread}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Badges: Status + Campanha + Tipo Sanguíneo */}
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="inline-flex items-center text-xs font-semibold text-blue-700 bg-blue-100 px-2 py-0.5 rounded-md border border-blue-200">
                      {donor.bloodType}
                    </span>

                    {donor.campaignId && (
                      <span
                        className="inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-md text-white shadow-sm"
                        style={{
                          backgroundColor:
                            campaigns.find((c) => c.id === donor.campaignId)
                              ?.color || "#6b7280",
                        }}
                      >
                        {campaigns.find((c) => c.id === donor.campaignId)
                          ?.name || "Campanha"}
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

                  {/* Última Mensagem */}
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
                      {calculateAge(donor.birthDate)} anos
                    </span>
                    <span>{donor.totalDonations} doações</span>
                    <span>Última: {donor.lastDonation}</span>
                  </div>
                </div>
              </div>
            )
          )
        )}
      </div>
    </div>
  );
};
