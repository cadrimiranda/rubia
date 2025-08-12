import { Filter } from "lucide-react";
import { Select } from "antd";
import type { Campaign } from "../../types/types";

const { Option } = Select;

interface CampaignSelectorProps {
  campaigns: Campaign[];
  selectedCampaign: Campaign | null;
  onCampaignChange: (campaign: Campaign | null) => void;
}

const CampaignSelector: React.FC<CampaignSelectorProps> = ({
  campaigns,
  selectedCampaign,
  onCampaignChange
}) => {
  return (
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
          const campaign =
            value === "all"
              ? null
              : campaigns.find((c) => c.id === value) || null;
          onCampaignChange(campaign);
        }}
        className="w-full"
        size="small"
      >
        <Option value="all">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-xl bg-gray-400"></div>
            <span>Todas as campanhas</span>
          </div>
        </Option>
        {campaigns.map((campaign) => (
          <Option key={campaign.id} value={campaign.id}>
            <div className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-xl"
                style={{ backgroundColor: campaign.color }}
              ></div>
              <span>{campaign.name}</span>
            </div>
          </Option>
        ))}
      </Select>
    </div>
  );
};

export { CampaignSelector };
