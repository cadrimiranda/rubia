import React, { useState, useRef, useEffect } from "react";
import {
  Search,
  Send,
  User,
  Heart,
  Circle,
  Plus,
  X,
  Paperclip,
  Image,
  FileText,
  Download,
  Upload,
  Calendar,
  Archive,
  MessageSquare,
  UserX,
} from "lucide-react";

interface Donor {
  id: string;
  name: string;
  avatar?: string;
  lastMessage: string;
  timestamp: string;
  unread: number;
  status: "online" | "offline" | "away";
  bloodType: string;
  phone: string;
  email: string;
  lastDonation: string;
  totalDonations: number;
  address: string;
  birthDate: string;
  weight: number;
  height: number;
}

interface FileAttachment {
  id: string;
  name: string;
  size: number;
  type: string;
  url: string;
}

interface Message {
  id: string;
  senderId: string;
  content: string;
  timestamp: string;
  isAI: boolean;
  attachments?: FileAttachment[];
}

interface ContextMenu {
  show: boolean;
  x: number;
  y: number;
  donorId: string;
}

const BloodCenterChat: React.FC = () => {
  const [selectedDonor, setSelectedDonor] = useState<Donor | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [messageInput, setMessageInput] = useState("");
  const [messages, setMessages] = useState<Message[]>([]);
  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [showDonorInfo, setShowDonorInfo] = useState(false);
  const [newChatSearch, setNewChatSearch] = useState("");
  const [attachments, setAttachments] = useState<FileAttachment[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [contextMenu, setContextMenu] = useState<ContextMenu>({
    show: false,
    x: 0,
    y: 0,
    donorId: "",
  });
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const donors: Donor[] = [
    {
      id: "1",
      name: "Maria Silva",
      lastMessage: "Obrigada pela lembrança! Posso doar na próxima semana.",
      timestamp: "14:30",
      unread: 1,
      status: "online",
      bloodType: "O+",
      phone: "(11) 99999-9999",
      email: "maria.silva@email.com",
      lastDonation: "12/03/2025",
      totalDonations: 8,
      address: "Rua das Flores, 123 - São Paulo, SP",
      birthDate: "15/08/1985",
      weight: 65,
      height: 165,
    },
    {
      id: "2",
      name: "João Santos",
      lastMessage: "Preciso reagendar minha doação.",
      timestamp: "13:45",
      unread: 0,
      status: "away",
      bloodType: "A-",
      phone: "(11) 88888-8888",
      email: "joao.santos@email.com",
      lastDonation: "08/02/2025",
      totalDonations: 15,
      address: "Av. Paulista, 456 - São Paulo, SP",
      birthDate: "22/03/1990",
      weight: 75,
      height: 178,
    },
    {
      id: "3",
      name: "Ana Costa",
      lastMessage: "Qual o horário disponível para amanhã?",
      timestamp: "12:20",
      unread: 2,
      status: "offline",
      bloodType: "B+",
      phone: "(11) 77777-7777",
      email: "ana.costa@email.com",
      lastDonation: "15/01/2025",
      totalDonations: 3,
      address: "Rua Augusta, 789 - São Paulo, SP",
      birthDate: "10/12/1992",
      weight: 58,
      height: 160,
    },
    {
      id: "4",
      name: "Carlos Oliveira",
      lastMessage: "",
      timestamp: "",
      unread: 0,
      status: "online",
      bloodType: "AB+",
      phone: "(11) 66666-6666",
      email: "carlos.oliveira@email.com",
      lastDonation: "20/12/2024",
      totalDonations: 12,
      address: "Rua da Consolação, 321 - São Paulo, SP",
      birthDate: "05/07/1988",
      weight: 80,
      height: 182,
    },
    {
      id: "5",
      name: "Patricia Lima",
      lastMessage: "",
      timestamp: "",
      unread: 0,
      status: "offline",
      bloodType: "O-",
      phone: "(11) 55555-5555",
      email: "patricia.lima@email.com",
      lastDonation: "10/11/2024",
      totalDonations: 6,
      address: "Alameda Santos, 654 - São Paulo, SP",
      birthDate: "18/01/1987",
      weight: 62,
      height: 170,
    },
  ];

  const sampleMessages: Message[] = [
    {
      id: "1",
      senderId: "1",
      content: "Olá! Gostaria de agendar uma doação de sangue.",
      timestamp: "14:25",
      isAI: false,
    },
    {
      id: "2",
      senderId: "ai",
      content:
        "Olá Maria! Fico feliz em ajudá-la. Temos horários disponíveis esta semana. Qual seria o melhor dia para você?",
      timestamp: "14:26",
      isAI: true,
    },
    {
      id: "3",
      senderId: "1",
      content: "Segue meu exame médico recente para análise.",
      timestamp: "14:28",
      isAI: false,
      attachments: [
        {
          id: "att1",
          name: "exame_sangue_2025.pdf",
          size: 245760,
          type: "application/pdf",
          url: "#",
        },
      ],
    },
    {
      id: "4",
      senderId: "1",
      content: "Obrigada pela lembrança! Posso doar na próxima semana.",
      timestamp: "14:30",
      isAI: false,
    },
  ];

  const activeDonors = donors.filter((d) => d.lastMessage);
  const availableDonors = donors.filter((d) => !d.lastMessage);
  const filteredDonors = activeDonors.filter((donor) =>
    donor.name.toLowerCase().includes(searchTerm.toLowerCase())
  );
  const filteredAvailableDonors = availableDonors.filter((donor) =>
    donor.name.toLowerCase().includes(newChatSearch.toLowerCase())
  );

  const getStatusColor = (status: string) => {
    switch (status) {
      case "online":
        return "text-green-500";
      case "away":
        return "text-yellow-500";
      case "offline":
        return "text-gray-400";
      default:
        return "text-gray-400";
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const getFileIcon = (type: string) => {
    if (type.startsWith("image/")) return <Image className="w-4 h-4" />;
    return <FileText className="w-4 h-4" />;
  };

  const calculateAge = (birthDate: string) => {
    const today = new Date();
    const birth = new Date(birthDate.split("/").reverse().join("-"));
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (
      monthDiff < 0 ||
      (monthDiff === 0 && today.getDate() < birth.getDate())
    ) {
      age--;
    }
    return age;
  };

  const handleDonorSelect = (donor: Donor) => {
    setSelectedDonor(donor);
    setShowNewChatModal(false);
    setShowDonorInfo(false);
    if (donor.id === "1") {
      setMessages(sampleMessages);
    } else {
      setMessages([]);
    }
  };

  const handleContextMenu = (e: React.MouseEvent, donorId: string) => {
    e.preventDefault();
    setContextMenu({
      show: true,
      x: e.clientX,
      y: e.clientY,
      donorId,
    });
  };

  const handleFileUpload = (files: FileList | null) => {
    if (!files) return;

    const newAttachments: FileAttachment[] = Array.from(files).map((file) => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
    }));

    setAttachments((prev) => [...prev, ...newAttachments]);
  };

  const removeAttachment = (id: string) => {
    setAttachments((prev) => prev.filter((att) => att.id !== id));
  };

  const handleSendMessage = () => {
    if ((!messageInput.trim() && attachments.length === 0) || !selectedDonor)
      return;

    const newMessage: Message = {
      id: Date.now().toString(),
      senderId: "ai",
      content: messageInput,
      timestamp: new Date().toLocaleTimeString("pt-BR", {
        hour: "2-digit",
        minute: "2-digit",
      }),
      isAI: true,
      attachments: attachments.length > 0 ? [...attachments] : undefined,
    };

    setMessages((prev) => [...prev, newMessage]);
    setMessageInput("");
    setAttachments([]);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    handleFileUpload(e.dataTransfer.files);
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const handleClickOutside = () => {
      setContextMenu({ show: false, x: 0, y: 0, donorId: "" });
    };

    if (contextMenu.show) {
      document.addEventListener("click", handleClickOutside);
      return () => document.removeEventListener("click", handleClickOutside);
    }
  }, [contextMenu.show]);

  return (
    <div className="flex h-screen bg-white">
      <input
        type="file"
        ref={fileInputRef}
        onChange={(e) => handleFileUpload(e.target.files)}
        multiple
        accept="image/*,.pdf,.doc,.docx,.txt"
        className="hidden"
      />

      {/* Context Menu */}
      {contextMenu.show && (
        <div
          className="fixed bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50 min-w-48"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          <button className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2">
            <MessageSquare className="w-4 h-4" />
            Ver conversa
          </button>
          <button className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2">
            <Calendar className="w-4 h-4" />
            Agendar doação
          </button>
          <button className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2">
            <User className="w-4 h-4" />
            Ver perfil
          </button>
          <div className="border-t border-gray-200 my-1"></div>
          <button className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2">
            <Archive className="w-4 h-4" />
            Arquivar conversa
          </button>
          <button className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2">
            <UserX className="w-4 h-4" />
            Bloquear contato
          </button>
        </div>
      )}

      {/* Sidebar */}
      <div className="w-80 bg-gray-50 border-r border-gray-200 flex flex-col">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center gap-3 mb-4">
            <Heart className="text-red-500 text-2xl" fill="currentColor" />
            <h1 className="text-lg font-semibold text-gray-800 m-0">
              Centro de Sangue
            </h1>
          </div>

          <div className="flex gap-2 mb-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Buscar conversas..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <button
              onClick={() => setShowNewChatModal(true)}
              className="bg-blue-500 hover:bg-blue-600 text-white p-2 rounded-lg transition-colors"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-2">
          {filteredDonors.map((donor) => (
            <div
              key={donor.id}
              onClick={() => handleDonorSelect(donor)}
              onContextMenu={(e) => handleContextMenu(e, donor.id)}
              className={`p-3 mb-1 rounded-lg cursor-pointer transition-all duration-200 ${
                selectedDonor?.id === donor.id
                  ? "bg-blue-50 border border-blue-200"
                  : "hover:bg-gray-100"
              }`}
            >
              <div className="flex items-start gap-3">
                <div className="relative">
                  <div className="w-10 h-10 bg-blue-500 rounded-xl flex items-center justify-center">
                    <User className="w-5 h-5 text-white" />
                  </div>
                  <Circle
                    className={`absolute -bottom-1 -right-1 w-3 h-3 ${getStatusColor(
                      donor.status
                    )} bg-white rounded-xl`}
                    fill="currentColor"
                  />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-medium text-gray-800 text-sm truncate">
                      {donor.name}
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-gray-500">
                        {donor.timestamp}
                      </span>
                      {donor.unread > 0 && (
                        <div className="bg-red-500 text-white text-xs rounded-xl w-5 h-5 flex items-center justify-center">
                          {donor.unread}
                        </div>
                      )}
                    </div>
                  </div>

                  <p className="text-xs text-gray-500 truncate mb-1 m-0">
                    {donor.lastMessage}
                  </p>

                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-1 rounded">
                      {donor.bloodType}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* New Chat Modal */}
      {showNewChatModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-96 max-h-96 flex flex-col">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-lg font-medium text-gray-800">
                Nova Conversa
              </h2>
              <button
                onClick={() => setShowNewChatModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4">
              <div className="relative mb-4">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <input
                  type="text"
                  placeholder="Buscar doadores..."
                  value={newChatSearch}
                  onChange={(e) => setNewChatSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 pb-4">
              {filteredAvailableDonors.map((donor) => (
                <div
                  key={donor.id}
                  onClick={() => handleDonorSelect(donor)}
                  className="p-3 mb-1 rounded-lg cursor-pointer hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="relative">
                      <div className="w-8 h-8 bg-blue-500 rounded-xl flex items-center justify-center">
                        <User className="w-4 h-4 text-white" />
                      </div>
                      <Circle
                        className={`absolute -bottom-1 -right-1 w-2.5 h-2.5 ${getStatusColor(
                          donor.status
                        )} bg-white rounded-xl`}
                        fill="currentColor"
                      />
                    </div>

                    <div className="flex-1">
                      <div className="font-medium text-gray-800 text-sm">
                        {donor.name}
                      </div>
                      <div className="text-xs text-gray-500">
                        {donor.bloodType} • Última doação: {donor.lastDonation}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Donor Info Modal */}
      {showDonorInfo && selectedDonor && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-96 max-h-96 flex flex-col">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-lg font-medium text-gray-800">
                Informações do Doador
              </h2>
              <button
                onClick={() => setShowDonorInfo(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 space-y-4 overflow-y-auto">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-blue-500 rounded-xl flex items-center justify-center">
                  <User className="w-8 h-8 text-white" />
                </div>
                <div>
                  <h3 className="font-medium text-gray-800">
                    {selectedDonor.name}
                  </h3>
                  <p className="text-sm text-gray-500">Doador ativo</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-500">Tipo sanguíneo:</span>
                  <p className="font-medium text-gray-800">
                    {selectedDonor.bloodType}
                  </p>
                </div>
                <div>
                  <span className="text-gray-500">Idade:</span>
                  <p className="font-medium text-gray-800">
                    {calculateAge(selectedDonor.birthDate)} anos
                  </p>
                </div>
                <div>
                  <span className="text-gray-500">Peso:</span>
                  <p className="font-medium text-gray-800">
                    {selectedDonor.weight} kg
                  </p>
                </div>
                <div>
                  <span className="text-gray-500">Altura:</span>
                  <p className="font-medium text-gray-800">
                    {selectedDonor.height} cm
                  </p>
                </div>
                <div className="col-span-2">
                  <span className="text-gray-500">Total de doações:</span>
                  <p className="font-medium text-gray-800">
                    {selectedDonor.totalDonations} doações
                  </p>
                </div>
                <div className="col-span-2">
                  <span className="text-gray-500">Última doação:</span>
                  <p className="font-medium text-gray-800">
                    {selectedDonor.lastDonation}
                  </p>
                </div>
              </div>

              <div>
                <span className="text-gray-500 text-sm">Endereço:</span>
                <p className="font-medium text-gray-800 text-sm">
                  {selectedDonor.address}
                </p>
              </div>

              <div>
                <span className="text-gray-500 text-sm">Contato:</span>
                <p className="font-medium text-gray-800 text-sm">
                  {selectedDonor.phone}
                </p>
                <p className="font-medium text-gray-800 text-sm">
                  {selectedDonor.email}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {selectedDonor ? (
          <>
            {/* Header */}
            <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
              <div
                className="flex items-center gap-4 cursor-pointer"
                onClick={() => setShowDonorInfo(true)}
              >
                <div className="relative">
                  <div className="w-8 h-8 bg-blue-500 rounded-xl flex items-center justify-center">
                    <User className="w-4 h-4 text-white" />
                  </div>
                  <Circle
                    className={`absolute -bottom-1 -right-1 w-3 h-3 ${getStatusColor(
                      selectedDonor.status
                    )} bg-white rounded-xl`}
                    fill="currentColor"
                  />
                </div>

                <div>
                  <h2 className="text-base font-medium text-gray-800 m-0 hover:text-blue-600 transition-colors">
                    {selectedDonor.name}
                  </h2>
                  <p className="text-xs text-gray-500 m-0">
                    Tipo sanguíneo: {selectedDonor.bloodType} • Última doação:{" "}
                    {selectedDonor.lastDonation}
                  </p>
                </div>
              </div>
            </div>

            {/* Messages */}
            <div
              className={`flex-1 overflow-y-auto p-4 bg-gray-50 ${
                isDragging
                  ? "bg-blue-50 border-2 border-dashed border-blue-300"
                  : ""
              }`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              {isDragging && (
                <div className="absolute inset-4 flex items-center justify-center bg-blue-50 bg-opacity-90 rounded-lg">
                  <div className="text-center">
                    <Upload className="w-12 h-12 text-blue-500 mx-auto mb-2" />
                    <p className="text-blue-600 font-medium">
                      Solte os arquivos aqui
                    </p>
                  </div>
                </div>
              )}

              <div className="max-w-4xl mx-auto space-y-3">
                {messages.map((message) => (
                  <div
                    key={message.id}
                    className={`flex ${
                      message.isAI ? "justify-start" : "justify-end"
                    }`}
                  >
                    <div
                      className={`max-w-xs lg:max-w-md px-4 py-2 rounded-2xl ${
                        message.isAI
                          ? "bg-white text-gray-800 shadow-sm"
                          : "bg-blue-500 text-white"
                      }`}
                    >
                      {message.content && (
                        <p
                          className={`m-0 mb-2 ${
                            message.isAI ? "text-gray-800" : "text-white"
                          }`}
                        >
                          {message.content}
                        </p>
                      )}

                      {message.attachments &&
                        message.attachments.length > 0 && (
                          <div className="space-y-2">
                            {message.attachments.map((attachment) => (
                              <div
                                key={attachment.id}
                                className={`flex items-center gap-2 p-2 rounded-lg ${
                                  message.isAI ? "bg-gray-50" : "bg-blue-600"
                                }`}
                              >
                                {getFileIcon(attachment.type)}
                                <div className="flex-1 min-w-0">
                                  <p
                                    className={`text-xs font-medium truncate m-0 ${
                                      message.isAI
                                        ? "text-gray-800"
                                        : "text-white"
                                    }`}
                                  >
                                    {attachment.name}
                                  </p>
                                  <p
                                    className={`text-xs ${
                                      message.isAI
                                        ? "text-gray-500"
                                        : "text-blue-100"
                                    }`}
                                  >
                                    {formatFileSize(attachment.size)}
                                  </p>
                                </div>
                                <button
                                  className={`${
                                    message.isAI
                                      ? "text-gray-600 hover:text-gray-800"
                                      : "text-blue-100 hover:text-white"
                                  }`}
                                >
                                  <Download className="w-3 h-3" />
                                </button>
                              </div>
                            ))}
                          </div>
                        )}

                      <div className="mt-1">
                        <span
                          className={`text-xs ${
                            message.isAI ? "text-gray-400" : "text-blue-100"
                          }`}
                        >
                          {message.timestamp}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            </div>

            {/* Message Input */}
            <div className="border-t border-gray-200 bg-white p-4">
              {attachments.length > 0 && (
                <div className="max-w-4xl mx-auto mb-3">
                  <div className="flex flex-wrap gap-2">
                    {attachments.map((attachment) => (
                      <div
                        key={attachment.id}
                        className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg"
                      >
                        {getFileIcon(attachment.type)}
                        <span className="text-xs text-gray-700 max-w-32 truncate">
                          {attachment.name}
                        </span>
                        <button
                          onClick={() => removeAttachment(attachment.id)}
                          className="text-gray-400 hover:text-gray-600"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="max-w-4xl mx-auto flex gap-3">
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="text-gray-600 hover:text-gray-800 p-2 hover:bg-gray-100 rounded-lg transition-colors self-end"
                >
                  <Paperclip className="w-4 h-4" />
                </button>

                <textarea
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Digite sua mensagem..."
                  rows={1}
                  className="flex-1 resize-none border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  style={{ minHeight: "40px", maxHeight: "120px" }}
                />

                <button
                  onClick={handleSendMessage}
                  disabled={!messageInput.trim() && attachments.length === 0}
                  className="bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white p-2 rounded-lg transition-colors self-end"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <Heart className="w-16 h-16 text-gray-300 mb-4 mx-auto" />
              <h3 className="text-xl text-gray-400 mb-2 font-medium">
                Selecione um doador
              </h3>
              <p className="text-gray-400 m-0">
                Escolha uma conversa existente ou inicie uma nova
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BloodCenterChat;
