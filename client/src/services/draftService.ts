import { apiClient } from '../api/client';

// Types for Message Drafts
export interface MessageDraft {
  id: string;
  companyId: string;
  conversationId: string;
  content: string;
  aiModel?: string;
  confidence?: number;
  status: DraftStatus;
  sourceType?: 'FAQ' | 'TEMPLATE' | 'AI_GENERATED' | 'AI_CONTEXTUAL';
  sourceId?: string;
  createdById?: string;
  createdByName?: string;
  reviewedById?: string;
  reviewedByName?: string;
  reviewedAt?: string;
  originalMessage?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export type DraftStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EDITED';

export interface CreateMessageDraftRequest {
  companyId: string;
  conversationId: string;
  content: string;
  aiModel?: string;
  confidence?: number;
  sourceType?: 'FAQ' | 'TEMPLATE' | 'AI_GENERATED' | 'AI_CONTEXTUAL';
  sourceId?: string;
  originalMessage?: string;
}

export interface GenerateDraftRequest {
  conversationId: string;
  userMessage: string;
}

export interface DraftReviewRequest {
  action: DraftStatus;
  editedContent?: string;
  rejectionReason?: string;
  sendImmediately?: boolean;
}

export interface DraftStats {
  companyId: string;
  totalDrafts: number;
  pendingDrafts: number;
  approvedDrafts: number;
  rejectedDrafts: number;
  editedDrafts: number;
  approvalRate: number;
  avgConfidence: number;
  mostUsedAiModel?: string;
  mostUsedSourceType?: string;
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
}

export interface Message {
  id: string;
  conversationId: string;
  content: string;
  senderPhone?: string;
  senderName?: string;
  fromOperator: boolean;
  sentAt: string;
  readAt?: string;
  messageType?: string;
  mediaUrl?: string;
  operatorId?: string;
  operatorName?: string;
}

class DraftService {
  
  /**
   * Gera draft automaticamente baseado na mensagem do usuário
   */
  async generateDraft(request: GenerateDraftRequest): Promise<MessageDraft | null> {
    try {
      const response = await apiClient.post('/api/drafts/generate', request);
      return response as MessageDraft | null;
    } catch (error: any) {
      if (error.status === 204) {
        // No content - nenhum draft foi gerado
        return null;
      }
      throw error;
    }
  }
  
  /**
   * Cria draft manualmente
   */
  async createDraft(request: CreateMessageDraftRequest): Promise<MessageDraft> {
    const response = await apiClient.post('/api/drafts', request);
    return response as MessageDraft;
  }
  
  /**
   * Lista drafts por conversa
   */
  async getDraftsByConversation(conversationId: string): Promise<MessageDraft[]> {
    const response = await apiClient.get(`/api/drafts/conversation/${conversationId}`);
    return response as MessageDraft[];
  }
  
  /**
   * Lista drafts pendentes paginados
   */
  async getPendingDrafts(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  } = {}): Promise<PagedResponse<MessageDraft>> {
    const searchParams = new URLSearchParams();
    
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    if (params.sortBy) searchParams.append('sortBy', params.sortBy);
    if (params.sortDir) searchParams.append('sortDir', params.sortDir);
    
    const response = await apiClient.get(`/api/drafts/pending?${searchParams.toString()}`);
    
    // Adapt backend response structure to frontend expectations
    return {
      content: (response as any).content,
      totalElements: (response as any).page?.totalElements || 0,
      totalPages: (response as any).page?.totalPages || 0,
      page: (response as any).page
    };
  }
  
  /**
   * Aprova um draft
   */
  async approveDraft(draftId: string, sendImmediately: boolean = true): Promise<Message | null> {
    const response = await apiClient.post(`/api/drafts/${draftId}/approve`, {
      action: 'APPROVED',
      sendImmediately
    });
    return response as Message | null;
  }
  
  /**
   * Edita e aprova um draft
   */
  async editAndApproveDraft(
    draftId: string, 
    editedContent: string, 
    sendImmediately: boolean = true
  ): Promise<Message | null> {
    const response = await apiClient.post(`/api/drafts/${draftId}/edit`, {
      action: 'EDITED',
      editedContent,
      sendImmediately
    });
    return response as Message | null;
  }
  
  /**
   * Rejeita um draft
   */
  async rejectDraft(draftId: string, rejectionReason?: string): Promise<void> {
    await apiClient.post(`/api/drafts/${draftId}/reject`, {
      action: 'REJECTED',
      rejectionReason
    });
  }
  
  /**
   * Obtém estatísticas de drafts
   */
  async getDraftStats(): Promise<DraftStats> {
    const response = await apiClient.get('/api/drafts/stats');
    return response as DraftStats;
  }
  
  /**
   * Formatters para exibição
   */
  formatConfidence(confidence?: number): string {
    if (!confidence) return '0%';
    return Math.round(confidence * 100) + '%';
  }
  
  formatStatus(status: DraftStatus): string {
    switch (status) {
      case 'PENDING': return 'Pendente';
      case 'APPROVED': return 'Aprovado';
      case 'REJECTED': return 'Rejeitado';
      case 'EDITED': return 'Editado';
      default: return status;
    }
  }
  
  getStatusColor(status: DraftStatus): string {
    switch (status) {
      case 'PENDING': return 'orange';
      case 'APPROVED': return 'green';
      case 'REJECTED': return 'red';
      case 'EDITED': return 'blue';
      default: return 'gray';
    }
  }
  
  /**
   * Helper para determinar se draft pode ser revisado
   */
  canReview(draft: MessageDraft): boolean {
    return draft.status === 'PENDING';
  }
  
  /**
   * Helper para obter ícone baseado na fonte
   */
  getSourceIcon(sourceType?: string): string {
    switch (sourceType) {
      case 'FAQ': return '❓';
      case 'TEMPLATE': return '📝';
      case 'AI_GENERATED': return '🤖';
      case 'AI_CONTEXTUAL': return '🧠';
      default: return '💬';
    }
  }
}

export const draftService = new DraftService();
export default draftService;