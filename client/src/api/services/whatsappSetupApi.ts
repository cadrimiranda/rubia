import { apiClient } from '../client';
import type { WhatsAppSetupStatus, WhatsAppInstance, WhatsAppInstanceWithStatus, MessagingProvider, WhatsAppInstanceStatus } from '../../types';

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
    const baseStatus = await apiClient.get<{
      requiresSetup: boolean;
      hasConfiguredInstance: boolean;  
      hasConnectedInstance: boolean;
      totalInstances: number;
      maxAllowedInstances: number;
      instances: WhatsAppInstance[];
    }>('/api/whatsapp-setup/status');

    // For now, just mark all active instances as needing configuration
    // The real status will be checked when needed
    const instancesWithStatus: WhatsAppInstanceWithStatus[] = baseStatus.instances.map((instance): WhatsAppInstanceWithStatus => {
      if (!instance.isActive) {
        return {
          ...instance,
          status: 'SUSPENDED',
          connected: false,
          error: 'Instance is inactive'
        };
      }

      // If instance has no display name or provider, it's likely not configured
      if (!instance.displayName) {
        return {
          ...instance,
          status: 'NOT_CONFIGURED',
          connected: false,
          error: 'Instance not configured'
        };
      }

      // For active instances with display name, assume they need status check
      return {
        ...instance,
        status: 'DISCONNECTED', // We'll assume disconnected until verified
        connected: false,
        error: 'Status needs verification'
      };
    });

    return {
      ...baseStatus,
      instances: instancesWithStatus
    };
  },

  // Get setup status with real-time Z-API status check (now handled by backend)
  getSetupStatusWithRealTimeCheck: async (): Promise<WhatsAppSetupStatus> => {
    // Backend now automatically checks Z-API status, so we can use the basic method
    return whatsappSetupApi.getSetupStatus();
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

  // Validate phone number format (Brazilian only - auto-adds 55)
  validatePhoneNumber: (phoneNumber: string): PhoneValidationResult => {
    // Remove all non-digit characters
    const cleaned = phoneNumber.replace(/\D/g, '');
    
    // Valid Brazilian DDDs
    const validDDDs = [
      '11', '12', '13', '14', '15', '16', '17', '18', '19', // SP
      '21', '22', '24', // RJ
      '27', '28', // ES
      '31', '32', '33', '34', '35', '37', '38', // MG
      '41', '42', '43', '44', '45', '46', // PR
      '47', '48', '49', // SC
      '51', '53', '54', '55', // RS
      '61', // DF
      '62', '64', // GO
      '63', // TO
      '65', '66', // MT
      '67', // MS
      '68', // AC
      '69', // RO
      '71', '73', '74', '75', '77', // BA
      '79', // SE
      '81', '87', // PE
      '82', // AL
      '83', // PB
      '84', // RN
      '85', '88', // CE
      '86', '89', // PI
      '91', '93', '94', // PA
      '92', '97', // AM
      '95', // RR
      '96', // AP
      '98', '99' // MA
    ];
    
    // Expected format: DDD + number (10-11 digits total)
    if (cleaned.length === 11) {
      // Mobile: DD + 9XXXXXXXX
      const ddd = cleaned.substring(0, 2);
      const number = cleaned.substring(2);
      
      if (!validDDDs.includes(ddd)) {
        return { 
          valid: false, 
          error: `DDD ${ddd} inválido. Use um DDD brasileiro válido.` 
        };
      }
      
      if (number.length === 9 && number.startsWith('9')) {
        // Valid mobile number - add 55 prefix
        return { valid: true, formatted: `55${cleaned}` };
      } else {
        return { 
          valid: false, 
          error: 'Número de celular deve ter 9 dígitos começando com 9.' 
        };
      }
    }
    
    if (cleaned.length === 10) {
      // Landline: DD + XXXXXXXX
      const ddd = cleaned.substring(0, 2);
      const number = cleaned.substring(2);
      
      if (!validDDDs.includes(ddd)) {
        return { 
          valid: false, 
          error: `DDD ${ddd} inválido. Use um DDD brasileiro válido.` 
        };
      }
      
      if (number.length === 8 && !number.startsWith('9')) {
        // Valid landline number - add 55 prefix
        return { valid: true, formatted: `55${cleaned}` };
      } else {
        return { 
          valid: false, 
          error: 'Número fixo deve ter 8 dígitos e não pode começar com 9.' 
        };
      }
    }
    
    // If user included 55 prefix, validate and keep it
    if (cleaned.length === 13 && cleaned.startsWith('55')) {
      const ddd = cleaned.substring(2, 4);
      const number = cleaned.substring(4);
      
      if (!validDDDs.includes(ddd)) {
        return { 
          valid: false, 
          error: `DDD ${ddd} inválido. Use um DDD brasileiro válido.` 
        };
      }
      
      if (number.length === 9 && number.startsWith('9')) {
        return { valid: true, formatted: cleaned };
      } else if (number.length === 8 && !number.startsWith('9')) {
        return { valid: true, formatted: cleaned };
      }
    }
    
    if (cleaned.length === 12 && cleaned.startsWith('55')) {
      const ddd = cleaned.substring(2, 4);
      const number = cleaned.substring(4);
      
      if (!validDDDs.includes(ddd)) {
        return { 
          valid: false, 
          error: `DDD ${ddd} inválido. Use um DDD brasileiro válido.` 
        };
      }
      
      if (number.length === 8 && !number.startsWith('9')) {
        return { valid: true, formatted: cleaned };
      }
    }
    
    return { 
      valid: false, 
      error: 'Formato inválido. Use: (48) 99999-9999 ou 48999999999' 
    };
  },

  // Format phone number for input (Brazilian format)
  formatPhoneForInput: (phoneNumber: string): string => {
    const cleaned = phoneNumber.replace(/\D/g, '');
    
    if (cleaned.length === 11) {
      // Mobile: 48999999999 -> (48) 99999-9999
      const ddd = cleaned.substring(0, 2);
      const part1 = cleaned.substring(2, 7);
      const part2 = cleaned.substring(7);
      return `(${ddd}) ${part1}-${part2}`;
    }
    
    if (cleaned.length === 10) {
      // Landline: 4833334444 -> (48) 3333-4444
      const ddd = cleaned.substring(0, 2);
      const part1 = cleaned.substring(2, 6);
      const part2 = cleaned.substring(6);
      return `(${ddd}) ${part1}-${part2}`;
    }
    
    return phoneNumber;
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