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
}

export interface ContextMenu {
  show: boolean;
  x: number;
  y: number;
  donorId: string;
}

export interface ChatState {
  selectedDonor: Donor | null;
  messages: Message[];
  attachments: FileAttachment[];
  searchTerm: string;
  messageInput: string;
  showNewChatModal: boolean;
  showDonorInfo: boolean;
  newChatSearch: string;
  isDragging: boolean;
  contextMenu: ContextMenu;
}
