export interface Campaign {
  id: string;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  status: 'active' | 'completed' | 'paused';
  color?: string;
  templatesUsed: string[];
}

export interface CampaignData {
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  sourceSystem: string;
  file?: File;
}

export interface ConversationTemplate {
  id: string;
  title: string;
  content: string;
  selected: boolean;
  category?: string;
  isCustom?: boolean;
}

export interface Donor {
  id: string;
  name: string;
  avatar?: string;
  lastMessage: string;
  timestamp: string;
  unread: number;
  status: "online" | "offline";
  bloodType: string;
  phone: string;
  email: string;
  lastDonation: string;
  totalDonations: number;
  address: string;
  birthDate: string;
  weight: number;
  height: number;
  hasActiveConversation?: boolean;
  conversationStatus?: string; // Para indicar o status da conversa
  campaignId?: string; // ID da campanha atual
  campaigns?: string[]; // Lista de todas as campanhas em que participa
  conversationId?: string; // ID da conversa ativa
}

export interface FileAttachment {
  id: string;
  name: string;
  size: number;
  type: string;
  url: string;
}

export interface Message {
  id: string;
  senderId: string;
  content: string;
  timestamp: string;
  isAI: boolean;
  attachments?: FileAttachment[];
  campaignId?: string; // ID da campanha
}

export interface ContextMenu {
  show: boolean;
  x: number;
  y: number;
  donorId: string;
}

export type ViewMode = 'full' | 'compact';

export interface ChatState {
  selectedDonor: Donor | null;
  selectedCampaign: Campaign | null;
  messages: Message[];
  attachments: FileAttachment[];
  searchTerm: string;
  messageInput: string;
  showNewChatModal: boolean;
  showDonorInfo: boolean;
  newChatSearch: string;
  isDragging: boolean;
  contextMenu: ContextMenu;
  showConfiguration: boolean;
  showScheduleModal: boolean;
  scheduleTarget: Donor | null;
  showConfirmationModal: boolean;
  confirmationData: {
    title: string;
    message: string;
    type: 'warning' | 'danger' | 'info';
    onConfirm: () => void;
    confirmText?: string;
  } | null;
  viewMode: ViewMode;
}
