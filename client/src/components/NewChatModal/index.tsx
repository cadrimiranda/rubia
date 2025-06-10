import React, { useState } from "react";
import { Search, X, User, Circle, Plus, UserPlus, Loader } from "lucide-react";
import type { Donor } from "../../types/types";
import { getStatusColor } from "../../utils";
import { customerApi } from "../../api/services/customerApi";
import { conversationApi } from "../../api/services/conversationApi";
import { customerAdapter } from "../../adapters/customerAdapter";

interface NewContactData {
  name: string;
  phone: string;
}

interface NewChatModalProps {
  show: boolean;
  searchTerm: string;
  availableDonors: Donor[];
  onClose: () => void;
  onSearchChange: (term: string) => void;
  onDonorSelect: (donor: Donor) => void;
  onNewContactCreate: (contactData: NewContactData) => void;
}

export const NewChatModal: React.FC<NewChatModalProps> = ({
  show,
  searchTerm,
  availableDonors,
  onClose,
  onSearchChange,
  onDonorSelect,
  onNewContactCreate,
}) => {
  const [activeTab, setActiveTab] = useState<"existing" | "new">("existing");
  const [newContactData, setNewContactData] = useState<NewContactData>({
    name: "",
    phone: "",
  });
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleNewContactSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContactData.name.trim() || !newContactData.phone.trim()) {
      return;
    }

    setIsCreating(true);
    setError(null);

    try {
      // Validar formato do telefone
      if (!customerAdapter.validateBrazilianPhone(newContactData.phone)) {
        throw new Error("Formato de telefone inválido. Use o formato brasileiro (11) 99999-9999");
      }

      // Verificar se já existe um cliente com este telefone
      const normalizedPhone = customerAdapter.normalizePhone(newContactData.phone);
      const existingCustomer = await customerApi.findByPhone(normalizedPhone);
      
      if (existingCustomer) {
        // Se já existe, usar o cliente existente
        const user = customerAdapter.toUser(existingCustomer);
        const donor: Donor = {
          ...user,
          phone: user.phone || normalizedPhone,
          lastMessage: "",
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "O+", // Valor padrão, será atualizado pelo backend
          email: "",
          lastDonation: "",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
        };
        onDonorSelect(donor);
      } else {
        // Criar novo cliente
        const createRequest = customerAdapter.toCreateRequest(
          newContactData.phone,
          newContactData.name
        );
        
        const newCustomer = await customerApi.create(createRequest);
        
        // Criar nova conversa para este cliente
        await conversationApi.create({
          customerId: newCustomer.id,
          channel: 'WEB'
        });

        // Converter para Donor e notificar componente pai
        const user = customerAdapter.toUser(newCustomer);
        const donor: Donor = {
          ...user,
          phone: user.phone || customerAdapter.normalizePhone(newContactData.phone),
          lastMessage: "",
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "O+", // Valor padrão
          email: "",
          lastDonation: "",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
        };
        onDonorSelect(donor);

        // Chamar callback original para compatibilidade
        onNewContactCreate(newContactData);
      }

      // Resetar formulário e fechar modal
      setNewContactData({ name: "", phone: "" });
      setActiveTab("existing");
      onClose();

    } catch (error) {
      console.error("Erro ao criar contato:", error);
      setError(error instanceof Error ? error.message : "Erro ao criar contato");
    } finally {
      setIsCreating(false);
    }
  };

  const resetForm = () => {
    setNewContactData({ name: "", phone: "" });
    setActiveTab("existing");
    setError(null);
    setIsCreating(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  if (!show) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-96 max-h-96 flex flex-col">
        <div className="p-4 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-800">Nova Conversa</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="border-b border-gray-200">
          <div className="flex">
            <button
              onClick={() => setActiveTab("existing")}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "existing"
                  ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <User className="w-4 h-4 inline mr-2" />
              Doadores Existentes
            </button>
            <button
              onClick={() => setActiveTab("new")}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "new"
                  ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <UserPlus className="w-4 h-4 inline mr-2" />
              Novo Contato
            </button>
          </div>
        </div>

        {activeTab === "existing" ? (
          <>
            <div className="p-4">
              <div className="relative mb-4">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <input
                  type="text"
                  placeholder="Buscar doadores..."
                  value={searchTerm}
                  onChange={(e) => onSearchChange(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 pb-4">
              {availableDonors.map((donor) => (
                <div
                  key={donor.id}
                  onClick={() => onDonorSelect(donor)}
                  className="p-3 mb-1 rounded-lg cursor-pointer hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="relative">
                      <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                        <User className="w-4 h-4 text-white" />
                      </div>
                      <Circle
                        className={`absolute -bottom-1 -right-1 w-2.5 h-2.5 ${getStatusColor(
                          donor.status
                        )} bg-white rounded-full`}
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
          </>
        ) : (
          <div className="p-4 flex-1">
            <form onSubmit={handleNewContactSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Nome completo
                </label>
                <input
                  type="text"
                  value={newContactData.name}
                  onChange={(e) =>
                    setNewContactData((prev) => ({
                      ...prev,
                      name: e.target.value,
                    }))
                  }
                  placeholder="Digite o nome do contato"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Número de telefone
                </label>
                <input
                  type="tel"
                  value={newContactData.phone}
                  onChange={(e) =>
                    setNewContactData((prev) => ({
                      ...prev,
                      phone: e.target.value,
                    }))
                  }
                  placeholder="(11) 99999-9999"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                />
              </div>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-sm text-red-600">{error}</p>
                </div>
              )}

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setActiveTab("existing")}
                  disabled={isCreating}
                  className="flex-1 px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={
                    !newContactData.name.trim() || 
                    !newContactData.phone.trim() || 
                    isCreating
                  }
                  className="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                >
                  {isCreating ? (
                    <>
                      <Loader className="w-4 h-4 inline mr-2 animate-spin" />
                      Criando...
                    </>
                  ) : (
                    <>
                      <Plus className="w-4 h-4 inline mr-2" />
                      Criar Contato
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};
