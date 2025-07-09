import { apiClient } from '../api/client';

export interface CreateMessageTemplateRequest {
  companyId: string;
  name: string;
  content: string;
  isAiGenerated: boolean;
  aiAgentId?: string;
  tone?: string;
}

export interface UpdateMessageTemplateRequest {
  name?: string;
  content?: string;
  tone?: string;
}

export interface MessageTemplateResponse {
  id: string;
  companyId: string;
  companyName: string;
  name: string;
  content: string;
  isAiGenerated: boolean;
  createdByUserId?: string;
  createdByUserName?: string;
  aiAgentId?: string;
  aiAgentName?: string;
  tone?: string;
  lastEditedByUserId?: string;
  lastEditedByUserName?: string;
  editCount: number;
  createdAt: string;
  updatedAt: string;
}

export const messageTemplateService = {
  async create(data: CreateMessageTemplateRequest): Promise<MessageTemplateResponse> {
    try {
      const response = await apiClient.post<MessageTemplateResponse>('/api/message-templates', data);
      return response;
    } catch (error) {
      console.error('Error creating message template:', error);
      throw error;
    }
  },

  async getAll(): Promise<MessageTemplateResponse[]> {
    try {
      const response = await apiClient.get<MessageTemplateResponse[]>('/api/message-templates');
      return response;
    } catch (error) {
      console.error('Error fetching message templates:', error);
      throw error;
    }
  },

  async getById(id: string): Promise<MessageTemplateResponse> {
    try {
      const response = await apiClient.get<MessageTemplateResponse>(`/api/message-templates/${id}`);
      return response;
    } catch (error) {
      console.error('Error fetching message template:', error);
      throw error;
    }
  },

  async update(id: string, data: UpdateMessageTemplateRequest): Promise<MessageTemplateResponse> {
    try {
      const response = await apiClient.put<MessageTemplateResponse>(`/api/message-templates/${id}`, data);
      return response;
    } catch (error) {
      console.error('Error updating message template:', error);
      throw error;
    }
  },

  async delete(id: string): Promise<void> {
    try {
      await apiClient.delete(`/api/message-templates/${id}`);
    } catch (error) {
      console.error('Error deleting message template:', error);
      throw error;
    }
  },

  async getByCompany(companyId: string): Promise<MessageTemplateResponse[]> {
    try {
      const response = await apiClient.get<MessageTemplateResponse[]>(`/api/message-templates/company/${companyId}`);
      return response;
    } catch (error) {
      console.error('Error fetching company message templates:', error);
      throw error;
    }
  },

  async search(term: string): Promise<MessageTemplateResponse[]> {
    try {
      const response = await apiClient.get<MessageTemplateResponse[]>(`/api/message-templates/search?term=${encodeURIComponent(term)}`);
      return response;
    } catch (error) {
      console.error('Error searching message templates:', error);
      throw error;
    }
  },

  async getRevisionHistory(templateId: string): Promise<MessageTemplateRevision[]> {
    try {
      const response = await apiClient.get<MessageTemplateRevision[]>(`/api/message-template-revisions/template/${templateId}`);
      return response;
    } catch (error) {
      console.error('Error fetching template revision history:', error);
      throw error;
    }
  },

  async getDeleted(): Promise<MessageTemplateResponse[]> {
    try {
      const response = await apiClient.get<MessageTemplateResponse[]>('/api/message-templates', { 
        deleted: 'true' 
      });
      return response;
    } catch (error) {
      console.error('Error fetching deleted message templates:', error);
      throw error;
    }
  },

  async restore(id: string): Promise<MessageTemplateResponse> {
    try {
      const response = await apiClient.post<MessageTemplateResponse>(`/api/message-templates/${id}/restore`);
      return response;
    } catch (error) {
      console.error('Error restoring message template:', error);
      throw error;
    }
  }
};

export type RevisionType = 'CREATE' | 'EDIT' | 'DELETE' | 'RESTORE';

export interface MessageTemplateRevision {
  id: string;
  templateId: string;
  revisionNumber: number;
  content: string;
  editedByUserId?: string;
  editedByUserName?: string;
  revisionType: RevisionType;
  revisionTimestamp: string;
  createdAt: string;
  updatedAt: string;
}