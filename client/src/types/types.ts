export interface Campaign {
  id: string;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  status: 'active' | 'completed' | 'paused' | 'ativa' | 'pausada' | 'concluida';
  color?: string;
  templatesUsed: number;
  totalContacts: number;
  contactsReached: number;
  createdAt: string;
  updatedAt: string;
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

export interface ConversationMedia {
  id: string;
  conversationId: string;
  fileUrl: string;
  mediaType: 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'STICKER' | 'OTHER';
  mimeType: string;
  originalFileName: string;
  fileSizeBytes: number;
  uploadedAt: string;
  uploadedByUser?: string;
  uploadedByCustomer?: string;
}

export interface PendingMedia {
  id: string;
  file: File;
  mediaType: 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'STICKER' | 'OTHER';
  mimeType: string;
  originalFileName: string;
  fileSizeBytes: number;
  previewUrl?: string; // Para preview local
}

export interface Message {
  id: string;
  senderId: string;
  content: string;
  timestamp: Date;
  isFromUser: boolean;
  isAI?: boolean;
  messageType: 'text' | 'image' | 'file' | 'audio';
  status: 'sending' | 'sent' | 'delivered' | 'read';
  mediaUrl?: string;
  mimeType?: string;
  audioDuration?: number;
  attachments?: FileAttachment[];
  media?: ConversationMedia[];
  campaignId?: string;
  externalMessageId?: string;
  isAiGenerated?: boolean;
  aiConfidence?: number;
  deliveredAt?: Date;
  readAt?: Date;
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
  media: ConversationMedia[];
  pendingMedia: PendingMedia[];
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
