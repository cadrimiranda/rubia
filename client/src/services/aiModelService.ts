import { apiClient } from '../api/client';

export interface AIModel {
  id: string;
  name: string;
  displayName: string;
  description: string;
  capabilities: string;
  impactDescription: string;
  costPer1kTokens: number;
  performanceLevel: 'BASICO' | 'INTERMEDIARIO' | 'AVANCADO' | 'PREMIUM';
  provider: string;
  isActive: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export const aiModelService = {
  getActiveModels: async (): Promise<AIModel[]> => {
    return await apiClient.get<AIModel[]>('/api/ai-models/active');
  },

  getAllModels: async (): Promise<AIModel[]> => {
    return await apiClient.get<AIModel[]>('/api/ai-models');
  },

  getModelsByProvider: async (provider: string): Promise<AIModel[]> => {
    return await apiClient.get<AIModel[]>(`/api/ai-models/provider/${provider}`);
  },

  getModelById: async (id: string): Promise<AIModel> => {
    return await apiClient.get<AIModel>(`/api/ai-models/${id}`);
  },
};