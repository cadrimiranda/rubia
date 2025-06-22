import type { Campaign } from "../types/types";

export const mockCampaigns: Campaign[] = [
  {
    id: "camp_1",
    name: "Campanha Junho 2025",
    description: "Campanha especial para reposição de estoque",
    startDate: "2025-06-01",
    endDate: "2025-06-30",
    status: "active",
    color: "#3b82f6",
    templatesUsed: ["1", "3", "5", "7"]
  },
  {
    id: "camp_2", 
    name: "Urgência O-",
    description: "Campanha emergencial para tipo sanguíneo O-",
    startDate: "2025-06-15",
    endDate: "2025-06-25",
    status: "active",
    color: "#dc2626",
    templatesUsed: ["2", "4"]
  },
  {
    id: "camp_3",
    name: "Doadores Corporativos",
    description: "Campanha voltada para empresas parceiras",
    startDate: "2025-05-01",
    endDate: "2025-07-31",
    status: "active", 
    color: "#059669",
    templatesUsed: ["10", "12"]
  },
  {
    id: "camp_4",
    name: "Primeira Doação",
    description: "Campanha de incentivo a novos doadores",
    startDate: "2025-06-01",
    endDate: "2025-08-31",
    status: "active",
    color: "#7c3aed",
    templatesUsed: ["4", "6", "8"]
  },
  {
    id: "camp_5",
    name: "Retorno Maio",
    description: "Campanha finalizada para doadores de retorno",
    startDate: "2025-05-01",
    endDate: "2025-05-31",
    status: "completed",
    color: "#6b7280",
    templatesUsed: ["3", "5", "11"]
  }
];

export const getCampaignById = (campaignId: string): Campaign | undefined => {
  return mockCampaigns.find(campaign => campaign.id === campaignId);
};

export const getActiveCampaigns = (): Campaign[] => {
  return mockCampaigns.filter(campaign => campaign.status === 'active');
};