import { apiClient } from '../api/client';

export interface EnhanceTemplateRequest {
  companyId: string;
  originalContent: string;
  enhancementType: 'friendly' | 'professional' | 'empathetic' | 'urgent' | 'motivational';
  category: string;
  title?: string;
}

export interface EnhancedTemplateResponse {
  originalContent: string;
  enhancedContent: string;
  enhancementType: string;
  aiModelUsed: string;
  tokensUsed: number;
  creditsConsumed: number;
  explanation: string;
}

export interface SaveTemplateWithAIMetadataRequest {
  templateId: string;
  content: string;
  userId: string;
  aiAgentId?: string;
  aiEnhancementType: string;
  aiTokensUsed?: number;
  aiCreditsConsumed?: number;
  aiModelUsed?: string;
  aiExplanation?: string;
}

export interface MessageTemplateRevisionResponse {
  id: string;
  templateId: string;
  templateName: string;
  revisionNumber: number;
  content: string;
  editedByUserId?: string;
  editedByUserName?: string;
  revisionType: string;
  revisionTimestamp: string;
  createdAt: string;
  updatedAt: string;
  // AI metadata
  aiAgentId?: string;
  aiAgentName?: string;
  aiEnhancementType?: string;
  aiTokensUsed?: number;
  aiCreditsConsumed?: number;
  aiModelUsed?: string;
  aiExplanation?: string;
}

export const templateEnhancementService = {
  enhanceTemplate: async (request: EnhanceTemplateRequest): Promise<EnhancedTemplateResponse> => {
    return await apiClient.post<EnhancedTemplateResponse>('/api/template-enhancement/enhance', request);
  },

  saveTemplateWithAIMetadata: async (request: SaveTemplateWithAIMetadataRequest): Promise<MessageTemplateRevisionResponse> => {
    return await apiClient.post<MessageTemplateRevisionResponse>('/api/template-enhancement/save-with-ai-metadata', request);
  },
};