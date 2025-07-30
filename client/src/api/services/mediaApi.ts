import type { ConversationMedia } from '../../types/types';
import { apiClient } from '../client';

export interface MediaUploadRequest {
  conversationId: string;
  file: File;
  mediaType: 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'STICKER' | 'OTHER';
}

export interface MediaUploadResponse {
  id: string;
  fileUrl: string;
  mediaType: string;
  mimeType: string;
  originalFileName: string;
  fileSizeBytes: number;
  uploadedAt: string;
}

export const mediaApi = {
  // Testar se a conversa existe e se temos acesso
  testConversationAccess: async (conversationId: string): Promise<boolean> => {
    try {
      // Tentar acessar a conversa primeiro
      const response = await apiClient.get(`/api/conversations/${conversationId}`);
      console.log('✅ Conversation access test passed:', response);
      return true;
    } catch (error) {
      console.error('❌ Conversation access test failed:', error);
      return false;
    }
  },

  // Upload de arquivo
  upload: async (request: MediaUploadRequest): Promise<MediaUploadResponse> => {
    // Primeiro, testar se temos acesso à conversa
    const hasAccess = await mediaApi.testConversationAccess(request.conversationId);
    if (!hasAccess) {
      throw new Error('Sem acesso à conversa. Verifique se a conversa existe e se você tem permissão.');
    }

    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('mediaType', request.mediaType);
    
    // Debug FormData contents
    console.log('📋 FormData contents:');
    for (const [key, value] of formData.entries()) {
      console.log(`  ${key}:`, value instanceof File ? `File(${value.name}, ${value.size} bytes)` : value);
    }
    
    console.log('📤 Uploading file:', {
      fileName: request.file.name,
      fileSize: request.file.size,
      mediaType: request.mediaType,
      conversationId: request.conversationId
    });
    
    // Debug auth info
    console.log('🔑 Auth info:', {
      hasToken: !!localStorage.getItem('auth_token'),
      companySlug: localStorage.getItem('auth_company_slug'),
      hostname: window.location.hostname
    });
    
    try {
      const response = await apiClient.post<MediaUploadResponse>(`/api/conversations/${request.conversationId}/media`, formData);
      return response;
    } catch (error) {
      console.error('❌ Media upload failed, trying alternative endpoint...');
      
      // Tentar endpoint alternativo sem /conversations/
      try {
        const altFormData = new FormData();
        altFormData.append('file', request.file);
        altFormData.append('mediaType', request.mediaType);
        altFormData.append('conversationId', request.conversationId);
        
        const altResponse = await apiClient.post<MediaUploadResponse>(`/api/media/upload`, altFormData);
        return altResponse;
      } catch (altError) {
        console.error('❌ Alternative endpoint also failed:', altError);
        throw error; // Re-throw original error
      }
    }
  },

  // Listar mídias de uma conversa
  getByConversation: async (conversationId: string): Promise<ConversationMedia[]> => {
    const response = await apiClient.get<ConversationMedia[]>(`/api/conversations/${conversationId}/media`);
    return response;
  },

  // Remover mídia
  delete: async (mediaId: string): Promise<void> => {
    await apiClient.delete(`/api/media/${mediaId}`);
  },

  // URL para download
  getDownloadUrl: (mediaId: string): string => {
    const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080";
    return `${baseURL}/api/media/${mediaId}/download`;
  },

  // Determinar tipo de mídia baseado no arquivo
  getMediaType: (file: File): 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'OTHER' => {
    const { type } = file;
    
    if (type.startsWith('image/')) return 'IMAGE';
    if (type.startsWith('video/')) return 'VIDEO';
    if (type.startsWith('audio/')) return 'AUDIO';
    if (type.includes('pdf') || type.includes('document') || type.includes('text')) return 'DOCUMENT';
    
    return 'OTHER';
  },

  // Validar arquivo
  validateFile: (file: File): { valid: boolean; error?: string } => {
    const maxSizes = {
      'image': 5 * 1024 * 1024,  // 5MB para imagens
      'video': 50 * 1024 * 1024, // 50MB para vídeos
      'audio': 10 * 1024 * 1024, // 10MB para áudios
      'document': 25 * 1024 * 1024 // 25MB para documentos
    };

    const allowedTypes = {
      'image': ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'],
      'video': ['video/mp4', 'video/webm', 'video/quicktime', 'video/x-msvideo'],
      'audio': ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp4', 'audio/webm', 'audio/x-m4a'],
      'document': [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain',
        'text/csv'
      ]
    };

    // Determinar categoria do arquivo
    let category: string | null = null;
    let maxSize = 10 * 1024 * 1024; // Default 10MB

    for (const [cat, types] of Object.entries(allowedTypes)) {
      if (types.includes(file.type)) {
        category = cat;
        maxSize = maxSizes[cat as keyof typeof maxSizes];
        break;
      }
    }

    if (!category) {
      return { valid: false, error: 'Tipo de arquivo não permitido.' };
    }

    if (file.size > maxSize) {
      const sizeInMB = Math.round(maxSize / (1024 * 1024));
      return { 
        valid: false, 
        error: `Arquivo muito grande. Máximo ${sizeInMB}MB para ${category === 'image' ? 'imagens' : category === 'video' ? 'vídeos' : category === 'audio' ? 'áudios' : 'documentos'}.` 
      };
    }

    // Validações específicas por tipo
    if (category === 'image' && file.size < 1024) {
      return { valid: false, error: 'Imagem muito pequena. Mínimo 1KB.' };
    }

    if (category === 'video' && file.size < 100 * 1024) {
      return { valid: false, error: 'Vídeo muito pequeno. Mínimo 100KB.' };
    }

    if (category === 'audio' && file.size < 10 * 1024) {
      return { valid: false, error: 'Áudio muito pequeno. Mínimo 10KB.' };
    }

    return { valid: true };
  },

  // Validar múltiplos arquivos
  validateFiles: (files: File[]): { valid: boolean; errors: string[] } => {
    const errors: string[] = [];
    const maxFiles = 5;

    if (files.length > maxFiles) {
      errors.push(`Máximo ${maxFiles} arquivos por vez.`);
    }

    let totalSize = 0;
    const maxTotalSize = 100 * 1024 * 1024; // 100MB total

    for (const file of files) {
      totalSize += file.size;
      const validation = mediaApi.validateFile(file);
      
      if (!validation.valid) {
        errors.push(`${file.name}: ${validation.error}`);
      }
    }

    if (totalSize > maxTotalSize) {
      errors.push('Tamanho total dos arquivos excede 100MB.');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
};