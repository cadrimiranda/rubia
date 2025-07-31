import { apiClient } from '../client';
import type { WhatsAppSetupStatus, WhatsAppInstance, MessagingProvider, WhatsAppInstanceStatus } from '../../types';

export interface CreateInstanceRequest {
  phoneNumber: string;
  displayName: string;
}

export interface ConfigureInstanceRequest {
  instanceId: string;
  accessToken: string;
}

export interface MessagingProviderInfo {
  provider: MessagingProvider;
  name: string;
  description: string;
  available: boolean;
}

export interface ActivateInstanceResponse {
  success: boolean;
  status: WhatsAppInstanceStatus;
  instanceId: string;
  message?: string;
  error?: string;
}

export interface SetPrimaryInstanceResponse {
  success: boolean;
  message: string;
}

export interface DeactivateInstanceResponse {
  success: boolean;
  message: string;
}

export interface ConnectionStatusResponse {
  success: boolean;
  instanceId: string;
  currentStatus: WhatsAppInstanceStatus;
  zapiStatus: {
    connected: boolean;
    error?: string;
    smartphoneConnected?: boolean;
  };
  lastStatusCheck?: string;
  lastConnectedAt?: string;
  error?: string;
}

export interface ReconnectInstanceResponse {
  success: boolean;
  message: string;
  status: WhatsAppInstanceStatus;
  instanceId: string;
  zapiStatus?: {
    connected: boolean;
    error?: string;
  };
  error?: string;
}

export interface PhoneValidationResult {
  valid: boolean;
  formatted?: string;
  error?: string;
}

export const whatsappSetupApi = {
  // Get setup status for current company
  getSetupStatus: async (): Promise<WhatsAppSetupStatus> => {
    return apiClient.get<WhatsAppSetupStatus>('/api/whatsapp-setup/status');
  },

  // Create new WhatsApp instance
  createInstance: async (request: CreateInstanceRequest): Promise<WhatsAppInstance> => {
    return apiClient.post<WhatsAppInstance>('/api/whatsapp-setup/create-instance', request);
  },

  // Configure instance with provider details
  configureInstance: async (instanceId: string, request: ConfigureInstanceRequest): Promise<WhatsAppInstance> => {
    return apiClient.post<WhatsAppInstance>(`/api/whatsapp-setup/${instanceId}/configure`, request);
  },

  // Activate instance (start QR code process)
  activateInstance: async (instanceId: string): Promise<ActivateInstanceResponse> => {
    return apiClient.post<ActivateInstanceResponse>(`/api/whatsapp-setup/${instanceId}/activate`);
  },

  // Set instance as primary
  setPrimaryInstance: async (instanceId: string): Promise<SetPrimaryInstanceResponse> => {
    return apiClient.post<SetPrimaryInstanceResponse>(`/api/whatsapp-setup/${instanceId}/set-primary`);
  },

  // Deactivate instance
  deactivateInstance: async (instanceId: string): Promise<DeactivateInstanceResponse> => {
    return apiClient.delete<DeactivateInstanceResponse>(`/api/whatsapp-setup/${instanceId}`);
  },

  // Check instance connection status
  checkConnectionStatus: async (instanceId: string): Promise<ConnectionStatusResponse> => {
    return apiClient.get<ConnectionStatusResponse>(`/api/whatsapp-setup/${instanceId}/connection-status`);
  },

  // Reconnect instance (show QR code if needed)
  reconnectInstance: async (instanceId: string): Promise<ReconnectInstanceResponse> => {
    return apiClient.post<ReconnectInstanceResponse>(`/api/whatsapp-setup/${instanceId}/reconnect`);
  },

  // Force status check
  forceStatusCheck: async (instanceId: string): Promise<any> => {
    return apiClient.post<any>(`/api/whatsapp-setup/${instanceId}/force-status-check`);
  },

  // Health check
  healthCheck: async (): Promise<any> => {
    return apiClient.get<any>('/api/whatsapp-setup/health-check');
  },

  // Get available providers
  getAvailableProviders: async (): Promise<MessagingProviderInfo[]> => {
    return apiClient.get<MessagingProviderInfo[]>('/api/whatsapp-setup/providers');
  },

  // Validate phone number format
  validatePhoneNumber: (phoneNumber: string): PhoneValidationResult => {
    // Remove all non-digit characters
    const cleaned = phoneNumber.replace(/\D/g, '');
    
    // Check if it's a Brazilian number
    if (cleaned.length === 11 && cleaned.startsWith('11')) {
      // São Paulo mobile: 11 9XXXX-XXXX
      return { valid: true, formatted: `55${cleaned}` };
    } else if (cleaned.length === 11 && cleaned.startsWith('1')) {
      // Other mobile: 1X 9XXXX-XXXX
      return { valid: true, formatted: `55${cleaned}` };
    } else if (cleaned.length === 13 && cleaned.startsWith('55')) {
      // Already formatted Brazilian: 55XX9XXXXXXXX
      return { valid: true, formatted: cleaned };
    } else if (cleaned.length === 10 && cleaned.startsWith('11')) {
      // São Paulo landline: 11 XXXX-XXXX
      return { valid: true, formatted: `55${cleaned}` };
    } else if (cleaned.length >= 10 && cleaned.length <= 15) {
      // International format
      if (cleaned.startsWith('55')) {
        return { valid: true, formatted: cleaned };
      }
      // Assume other international
      return { valid: true, formatted: cleaned };
    }
    
    return { 
      valid: false, 
      error: 'Formato inválido. Use: (11) 99999-9999 ou 5511999999999' 
    };
  },

  // Format phone number for display
  formatPhoneNumber: (phoneNumber: string): string => {
    const cleaned = phoneNumber.replace(/\D/g, '');
    
    if (cleaned.startsWith('55') && cleaned.length === 13) {
      // Brazilian format: 5511999999999 -> +55 (11) 99999-9999
      const countryCode = cleaned.substring(0, 2);
      const areaCode = cleaned.substring(2, 4);
      const number = cleaned.substring(4);
      const part1 = number.substring(0, 5);
      const part2 = number.substring(5);
      
      return `+${countryCode} (${areaCode}) ${part1}-${part2}`;
    }
    
    return phoneNumber;
  }
};

export default whatsappSetupApi;