import { apiClient } from '../client';
import type { Campaign } from '../../types/types';

export interface CampaignResponse {
  id: string;
  name: string;
  description?: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELED';
  startDate?: string;
  endDate?: string;
  totalContacts: number;
  contactsReached: number;
  createdAt: string;
  updatedAt: string;
}

export const campaignApi = {
  /**
   * Busca campanhas ativas de uma empresa
   */
  async getActiveCampaigns(companyId: string): Promise<Campaign[]> {
    try {
      const response = await apiClient.get<CampaignResponse[]>(`/api/campaigns/company/${companyId}/active`);
      
      // Converter response do backend para o formato esperado pelo frontend
      return response.map((campaign, index) => ({
        id: campaign.id,
        name: campaign.name,
        color: generateCampaignColor(index), // Gerar cores consistentes
        status: campaign.status.toLowerCase() as 'ativa' | 'pausada' | 'concluida',
        totalContacts: campaign.totalContacts,
        contactsReached: campaign.contactsReached,
        startDate: campaign.startDate || '',
        endDate: campaign.endDate || '',
        description: campaign.description,
        createdAt: campaign.createdAt,
        updatedAt: campaign.updatedAt,
        templatesUsed: 1 // Valor padrão
      }));
    } catch (error) {
      console.error('Erro ao buscar campanhas ativas:', error);
      throw error;
    }
  },

  /**
   * Busca estatísticas de uma campanha específica
   */
  async getCampaignStatistics(campaignId: string): Promise<any> {
    try {
      const response = await apiClient.get(`/api/campaigns/${campaignId}/statistics`);
      return response;
    } catch (error) {
      console.error('Erro ao buscar estatísticas da campanha:', error);
      throw error;
    }
  }
};

/**
 * Gera cores consistentes para campanhas baseado no índice
 */
function generateCampaignColor(index: number): string {
  const colors = [
    '#3B82F6', // blue-500
    '#10B981', // emerald-500
    '#F59E0B', // amber-500
    '#EF4444', // red-500
    '#8B5CF6', // violet-500
    '#06B6D4', // cyan-500
    '#84CC16', // lime-500
    '#F97316', // orange-500
    '#EC4899', // pink-500
    '#6366F1', // indigo-500
  ];
  
  return colors[index % colors.length];
}