import { apiClient } from '../api/client';

export interface CampaignData {
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  sourceSystem?: string;
  templateIds: string[];
}

export interface CampaignResponse {
  id: string;
  name: string;
  description?: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELED';
  startDate: string;
  endDate: string;
  totalContacts: number;
  contactsReached: number;
  sourceSystemName?: string;
  sourceSystemId?: string;
  companyId: string;
  createdBy?: string;
  initialMessageTemplateId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CampaignProcessingResult {
  success: boolean;
  campaign: CampaignResponse;
  statistics: {
    processed: number;
    created: number;
    duplicates: number;
    errors: number;
  };
  errors: string[];
}

export interface CampaignStatistics {
  campaignId: string;
  campaignName: string;
  status: string;
  totalContacts: number;
  contactsReached: number;
  contactStatistics: {
    pending: number;
    sent: number;
    responded: number;
    converted: number;
    failed: number;
    optOut: number;
  };
  responseRate: number;
  conversionRate: number;
}

class CampaignService {
  private readonly baseURL = '/api/campaigns';

  async processExcelAndCreateCampaign(
    file: File,
    campaignData: CampaignData,
    companyId: string,
    userId: string
  ): Promise<CampaignProcessingResult> {
    const formData = new FormData();
    formData.append('file', file);
    
    const processingData = {
      companyId,
      userId,
      name: campaignData.name,
      description: campaignData.description,
      startDate: campaignData.startDate,
      endDate: campaignData.endDate,
      sourceSystem: campaignData.sourceSystem,
      templateIds: campaignData.templateIds,
    };
    
    formData.append('data', new Blob([JSON.stringify(processingData)], {
      type: 'application/json'
    }));

    const response = await apiClient.post<CampaignProcessingResult>(
      `${this.baseURL}/process`,
      formData
    );

    return response;
  }

  async getCampaigns(companyId: string): Promise<CampaignResponse[]> {
    const response = await apiClient.get<CampaignResponse[]>(
      `${this.baseURL}/company/${companyId}`
    );
    return response.data;
  }

  async getActiveCampaigns(companyId: string): Promise<CampaignResponse[]> {
    const response = await apiClient.get<CampaignResponse[]>(
      `${this.baseURL}/company/${companyId}/active`
    );
    return response.data;
  }

  async getCampaignById(id: string): Promise<CampaignResponse> {
    const response = await apiClient.get<CampaignResponse>(`${this.baseURL}/${id}`);
    return response.data;
  }

  async pauseCampaign(id: string): Promise<CampaignResponse> {
    const response = await apiClient.put<CampaignResponse>(`${this.baseURL}/${id}/pause`);
    return response.data;
  }

  async resumeCampaign(id: string): Promise<CampaignResponse> {
    const response = await apiClient.put<CampaignResponse>(`${this.baseURL}/${id}/resume`);
    return response.data;
  }

  async completeCampaign(id: string): Promise<CampaignResponse> {
    const response = await apiClient.put<CampaignResponse>(`${this.baseURL}/${id}/complete`);
    return response.data;
  }

  async getCampaignStatistics(id: string): Promise<CampaignStatistics> {
    const response = await apiClient.get<CampaignStatistics>(`${this.baseURL}/${id}/statistics`);
    return response.data;
  }

  async deleteCampaign(id: string): Promise<void> {
    await apiClient.delete(`${this.baseURL}/${id}`);
  }
}

export const campaignService = new CampaignService();