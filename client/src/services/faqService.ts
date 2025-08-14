import { apiClient } from '../api/client';

// Types matching backend DTOs
export interface FAQ {
  id: string;
  companyId: string;
  question: string;
  answer: string;
  keywords: string[];
  triggers: string[];
  usageCount: number;
  successRate: number;
  isActive: boolean;
  createdById: string;
  createdByName: string;
  lastEditedById?: string;
  lastEditedByName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFAQRequest {
  companyId: string;
  question: string;
  answer: string;
  keywords?: string[];
  triggers?: string[];
  isActive?: boolean;
}

export interface UpdateFAQRequest {
  question?: string;
  answer?: string;
  keywords?: string[];
  triggers?: string[];
  isActive?: boolean;
}

export interface FAQSearchRequest {
  companyId?: string;
  searchTerm?: string;
  userMessage?: string;
  limit?: number;
  offset?: number;
  minConfidenceScore?: number;
}

export interface FAQMatch {
  faq: FAQ;
  confidenceScore: number;
  matchReason: string;
  matchedText: string;
}

export interface FAQStats {
  companyId: string;
  totalFAQs: number;
  activeFAQs: number;
  inactiveFAQs: number;
  averageSuccessRate: number;
  totalUsageCount: number;
  mostUsedFAQ?: FAQ;
  topPerformingFAQ?: FAQ;
}

export interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
  totalElements?: number; // For backward compatibility
  totalPages?: number;    // For backward compatibility
  last?: boolean;
  first?: boolean;
  numberOfElements?: number;
  size?: number;
  number?: number;
  empty?: boolean;
}

class FAQService {
  // Get paginated FAQs with search and filtering
  async searchFAQs(params: {
    searchTerm?: string;
    isActive?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  }): Promise<PagedResponse<FAQ>> {
    const searchParams = new URLSearchParams();
    
    if (params.searchTerm) searchParams.append('searchTerm', params.searchTerm);
    if (params.isActive !== undefined) searchParams.append('isActive', params.isActive.toString());
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    if (params.sortBy) searchParams.append('sortBy', params.sortBy);
    if (params.sortDir) searchParams.append('sortDir', params.sortDir);

    const response = await apiClient.get(`/api/faqs/search?${searchParams.toString()}`);
    
    // Adapt backend response structure to frontend expectations
    return {
      content: (response as any).content,
      page: (response as any).page,
      totalElements: (response as any).page?.totalElements || 0,
      totalPages: (response as any).page?.totalPages || 0,
      size: (response as any).page?.size || 10,
      number: (response as any).page?.number || 0,
      first: ((response as any).page?.number || 0) === 0,
      last: ((response as any).page?.number || 0) === ((response as any).page?.totalPages || 1) - 1,
      empty: ((response as any).content?.length || 0) === 0,
      numberOfElements: (response as any).content?.length || 0
    };
  }

  // Get all active FAQs (simplified)
  async getActiveFAQs(): Promise<FAQ[]> {
    const response = await apiClient.get('/api/faqs/active');
    return response as FAQ[];
  }

  // Get FAQ by ID
  async getFAQById(id: string): Promise<FAQ> {
    const response = await apiClient.get(`/api/faqs/${id}`);
    return response as FAQ;
  }

  // Create new FAQ
  async createFAQ(data: CreateFAQRequest): Promise<FAQ> {
    const response = await apiClient.post('/api/faqs', data);
    return response as FAQ;
  }

  // Update FAQ
  async updateFAQ(id: string, data: UpdateFAQRequest): Promise<FAQ> {
    const response = await apiClient.put(`/api/faqs/${id}`, data);
    return response as FAQ;
  }

  // Soft delete FAQ
  async deleteFAQ(id: string): Promise<void> {
    await apiClient.delete(`/api/faqs/${id}/soft`);
  }

  // Toggle FAQ active status
  async toggleFAQStatus(id: string, isActive: boolean): Promise<FAQ> {
    const response = await apiClient.put(`/api/faqs/${id}`, { isActive });
    return response as FAQ;
  }

  // Search relevant FAQs for AI (future use)
  async searchRelevantFAQs(searchRequest: FAQSearchRequest): Promise<FAQMatch[]> {
    const response = await apiClient.post('/api/faqs/search/relevant', searchRequest);
    return response as FAQMatch[];
  }

  // Record FAQ usage for AI learning
  async recordFAQUsage(id: string, userMessage: string, wasApproved: boolean): Promise<void> {
    const params = new URLSearchParams({
      userMessage,
      wasApproved: wasApproved.toString()
    });
    await apiClient.post(`/api/faqs/${id}/usage?${params.toString()}`);
  }

  // Get FAQ statistics
  async getFAQStats(): Promise<FAQStats> {
    const response = await apiClient.get('/api/faqs/stats');
    return response as FAQStats;
  }

  // Get deleted FAQs
  async getDeletedFAQs(): Promise<FAQ[]> {
    const response = await apiClient.get('/api/faqs/deleted');
    return response as FAQ[];
  }

  // Restore deleted FAQ
  async restoreFAQ(id: string): Promise<FAQ> {
    const response = await apiClient.post(`/api/faqs/${id}/restore`);
    return response as FAQ;
  }

  // Test FAQ search (for frontend testing)
  async testFAQSearch(message: string): Promise<FAQMatch[]> {
    const response = await apiClient.post('/api/faqs/test-search', { message });
    return response as FAQMatch[];
  }
}

export const faqService = new FAQService();