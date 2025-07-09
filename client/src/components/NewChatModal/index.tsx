import React, { useState } from "react";
import { Search, X, User, Circle, Plus, UserPlus, Loader } from "lucide-react";
import type { Donor } from "../../types/types";
import { getStatusColor } from "../../utils";
import { customerApi } from "../../api/services/customerApi";
import { customerAdapter } from "../../adapters/customerAdapter";

interface NewContactData {
  name: string;
  phone: string;
  profileUrl?: string;
  birthDate?: string;
  lastDonationDate?: string;
  nextEligibleDonationDate?: string;
  bloodType?: string;
  height?: string;
  weight?: string;
  addressStreet?: string;
  addressNumber?: string;
  addressComplement?: string;
  addressPostalCode?: string;
  addressCity?: string;
  addressState?: string;
  donor?: Donor;
}

interface NewChatModalProps {
  show: boolean;
  searchTerm: string;
  availableDonors: Donor[];
  onClose: () => void;
  onSearchChange: (term: string) => void;
  onDonorSelect: (donor: Donor) => void;
  onNewContactCreate: (contactData: NewContactData) => void;
  isLoadingContacts?: boolean;
}

export const NewChatModal: React.FC<NewChatModalProps> = ({
  show,
  searchTerm,
  availableDonors,
  onClose,
  onSearchChange,
  onDonorSelect,
  onNewContactCreate,
  isLoadingContacts = false,
}) => {
  const [activeTab, setActiveTab] = useState<"existing" | "new">("existing");
  const [newContactData, setNewContactData] = useState<NewContactData>({
    name: "",
    phone: "",
    profileUrl: "",
    birthDate: "",
    lastDonationDate: "",
    nextEligibleDonationDate: "",
    bloodType: "",
    height: "",
    weight: "",
    addressStreet: "",
    addressNumber: "",
    addressComplement: "",
    addressPostalCode: "",
    addressCity: "",
    addressState: "",
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
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
          hasActiveConversation: false, // Assumir que não tem conversa ativa ainda
        };
        
        // Adicionar à lista de doadores se não estiver presente
        onNewContactCreate({
          name: newContactData.name,
          phone: newContactData.phone,
          donor: donor
        });
        
        // Selecionar o donor
        onDonorSelect(donor);
      } else {
        // Criar novo cliente
        const createRequest = {
          phone: customerAdapter.normalizePhone(newContactData.phone),
          name: newContactData.name,
          profileUrl: newContactData.profileUrl,
          birthDate: newContactData.birthDate,
          lastDonationDate: newContactData.lastDonationDate,
          nextEligibleDonationDate: newContactData.nextEligibleDonationDate,
          bloodType: newContactData.bloodType,
          height: newContactData.height ? parseInt(newContactData.height) : undefined,
          weight: newContactData.weight ? parseFloat(newContactData.weight) : undefined,
          addressStreet: newContactData.addressStreet,
          addressNumber: newContactData.addressNumber,
          addressComplement: newContactData.addressComplement,
          addressPostalCode: newContactData.addressPostalCode,
          addressCity: newContactData.addressCity,
          addressState: newContactData.addressState,
        };
        
        const newCustomer = await customerApi.create(createRequest);
        
        // Não criar conversa aqui - será criada quando a primeira mensagem for enviada

        // Converter para Donor e notificar componente pai
        const user = customerAdapter.toUser(newCustomer);
        const donor: Donor = {
          ...user,
          phone: user.phone || customerAdapter.normalizePhone(newContactData.phone),
          lastMessage: "", // Vazio pois ainda não há conversa
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "O+", // Valor padrão
          email: "",
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
          hasActiveConversation: false, // Marcar que não tem conversa ativa ainda
        };
        
        // Primeiro adicionar à lista de doadores via callback
        onNewContactCreate({
          name: newContactData.name,
          phone: newContactData.phone,
          donor: donor // Passar o donor criado para ser adicionado à lista
        });
        
        // Depois selecionar o donor
        onDonorSelect(donor);
      }

      // Resetar formulário e fechar modal
      setNewContactData({ 
        name: "", 
        phone: "",
        profileUrl: "",
        birthDate: "",
        lastDonationDate: "",
        nextEligibleDonationDate: "",
        bloodType: "",
        height: "",
        weight: "",
        addressStreet: "",
        addressNumber: "",
        addressComplement: "",
        addressPostalCode: "",
        addressCity: "",
        addressState: "",
      });
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
    setNewContactData({ 
      name: "", 
      phone: "",
      profileUrl: "",
      birthDate: "",
      lastDonationDate: "",
      nextEligibleDonationDate: "",
      bloodType: "",
      height: "",
      weight: "",
      addressStreet: "",
      addressNumber: "",
      addressComplement: "",
      addressPostalCode: "",
      addressCity: "",
      addressState: "",
    });
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
    <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-[550px] max-h-[85vh] flex flex-col shadow-2xl">
        <div className="p-6 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">Nova Conversa</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-full hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="border-b border-gray-100">
          <div className="flex mx-6">
            <button
              onClick={() => setActiveTab("existing")}
              className={`flex-1 px-4 py-4 text-sm font-medium transition-all duration-200 ${
                activeTab === "existing"
                  ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50"
                  : "text-gray-500 hover:text-gray-700 hover:bg-gray-50"
              }`}
            >
              <User className="w-4 h-4 inline mr-2" />
              Doadores Existentes
            </button>
            <button
              onClick={() => setActiveTab("new")}
              className={`flex-1 px-4 py-4 text-sm font-medium transition-all duration-200 ${
                activeTab === "new"
                  ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50"
                  : "text-gray-500 hover:text-gray-700 hover:bg-gray-50"
              }`}
            >
              <UserPlus className="w-4 h-4 inline mr-2" />
              Novo Contato
            </button>
          </div>
        </div>

        {activeTab === "existing" ? (
          <>
            <div className="p-6">
              <div className="relative mb-6">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder="Buscar doadores..."
                  value={searchTerm}
                  onChange={(e) => onSearchChange(e.target.value)}
                  className="w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors bg-gray-50/50 focus:bg-white"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-6 pb-6">
              {isLoadingContacts ? (
                <div className="flex flex-col items-center justify-center h-40 text-gray-500">
                  <Loader className="w-8 h-8 animate-spin mb-3 text-blue-500" />
                  <span className="text-sm font-medium">Carregando contatos...</span>
                </div>
              ) : availableDonors.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-40 text-gray-500">
                  <User className="w-8 h-8 mb-3" />
                  <span className="text-sm font-medium">
                    {searchTerm 
                      ? "Nenhum contato encontrado para sua busca" 
                      : "Nenhum contato disponível"
                    }
                  </span>
                </div>
              ) : (
                availableDonors.map((donor) => (
                  <div
                    key={donor.id}
                    onClick={() => onDonorSelect(donor)}
                    className="p-4 mb-2 rounded-xl cursor-pointer hover:bg-gray-50 transition-all duration-200 border border-transparent hover:border-gray-200"
                  >
                    <div className="flex items-center gap-4">
                      <div className="relative">
                        <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center">
                          <User className="w-5 h-5 text-white" />
                        </div>
                        <Circle
                          className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 ${getStatusColor(
                            donor.status
                          )} bg-white rounded-full border-2 border-white`}
                          fill="currentColor"
                        />
                      </div>

                      <div className="flex-1">
                        <div className="font-semibold text-gray-900 text-sm">
                          {donor.name}
                        </div>
                        <div className="text-xs text-gray-500 mt-1">
                          {donor.phone && `${donor.phone} • `}
                          {donor.bloodType} • Última doação: {donor.lastDonation}
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        ) : (
          <div className="p-6 flex-1 overflow-y-auto">
            <form onSubmit={handleNewContactSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-3">
                  Nome completo *
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
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-3">
                  Número de telefone *
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
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-3">
                  URL da foto de perfil
                </label>
                <input
                  type="url"
                  value={newContactData.profileUrl}
                  onChange={(e) =>
                    setNewContactData((prev) => ({
                      ...prev,
                      profileUrl: e.target.value,
                    }))
                  }
                  placeholder="https://exemplo.com/foto.jpg"
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-3">
                  Data de nascimento
                </label>
                <input
                  type="date"
                  value={newContactData.birthDate}
                  onChange={(e) =>
                    setNewContactData((prev) => ({
                      ...prev,
                      birthDate: e.target.value,
                    }))
                  }
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Última doação
                  </label>
                  <input
                    type="date"
                    value={newContactData.lastDonationDate}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        lastDonationDate: e.target.value,
                      }))
                    }
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Próxima doação elegível
                  </label>
                  <input
                    type="date"
                    value={newContactData.nextEligibleDonationDate}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        nextEligibleDonationDate: e.target.value,
                      }))
                    }
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Tipo sanguíneo
                  </label>
                  <select
                    value={newContactData.bloodType}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        bloodType: e.target.value,
                      }))
                    }
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  >
                    <option value="">Selecione</option>
                    <option value="A+">A+</option>
                    <option value="A-">A-</option>
                    <option value="B+">B+</option>
                    <option value="B-">B-</option>
                    <option value="AB+">AB+</option>
                    <option value="AB-">AB-</option>
                    <option value="O+">O+</option>
                    <option value="O-">O-</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Altura (cm)
                  </label>
                  <input
                    type="number"
                    min="100"
                    max="250"
                    value={newContactData.height}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        height: e.target.value,
                      }))
                    }
                    placeholder="170"
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Peso (kg)
                  </label>
                  <input
                    type="number"
                    min="30"
                    max="200"
                    step="0.1"
                    value={newContactData.weight}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        weight: e.target.value,
                      }))
                    }
                    placeholder="70.5"
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Endereço</h3>
                
                <div className="grid grid-cols-3 gap-4 mb-4">
                  <div className="col-span-2">
                    <label className="block text-sm font-semibold text-gray-900 mb-3">
                      Rua/Avenida
                    </label>
                    <input
                      type="text"
                      value={newContactData.addressStreet}
                      onChange={(e) =>
                        setNewContactData((prev) => ({
                          ...prev,
                          addressStreet: e.target.value,
                        }))
                      }
                      placeholder="Rua das Flores"
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-900 mb-3">
                      Número
                    </label>
                    <input
                      type="text"
                      value={newContactData.addressNumber}
                      onChange={(e) =>
                        setNewContactData((prev) => ({
                          ...prev,
                          addressNumber: e.target.value,
                        }))
                      }
                      placeholder="123"
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                    />
                  </div>
                </div>

                <div className="mb-4">
                  <label className="block text-sm font-semibold text-gray-900 mb-3">
                    Complemento
                  </label>
                  <input
                    type="text"
                    value={newContactData.addressComplement}
                    onChange={(e) =>
                      setNewContactData((prev) => ({
                        ...prev,
                        addressComplement: e.target.value,
                      }))
                    }
                    placeholder="Apartamento 45, Bloco B"
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-gray-900 mb-3">
                      CEP
                    </label>
                    <input
                      type="text"
                      value={newContactData.addressPostalCode}
                      onChange={(e) =>
                        setNewContactData((prev) => ({
                          ...prev,
                          addressPostalCode: e.target.value,
                        }))
                      }
                      placeholder="01234-567"
                      pattern="[0-9]{5}-?[0-9]{3}"
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-900 mb-3">
                      Cidade
                    </label>
                    <input
                      type="text"
                      value={newContactData.addressCity}
                      onChange={(e) =>
                        setNewContactData((prev) => ({
                          ...prev,
                          addressCity: e.target.value,
                        }))
                      }
                      placeholder="São Paulo"
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-900 mb-3">
                      Estado
                    </label>
                    <select
                      value={newContactData.addressState}
                      onChange={(e) =>
                        setNewContactData((prev) => ({
                          ...prev,
                          addressState: e.target.value,
                        }))
                      }
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-gray-50/50 focus:bg-white"
                    >
                      <option value="">Selecione</option>
                      <option value="AC">AC</option>
                      <option value="AL">AL</option>
                      <option value="AP">AP</option>
                      <option value="AM">AM</option>
                      <option value="BA">BA</option>
                      <option value="CE">CE</option>
                      <option value="DF">DF</option>
                      <option value="ES">ES</option>
                      <option value="GO">GO</option>
                      <option value="MA">MA</option>
                      <option value="MT">MT</option>
                      <option value="MS">MS</option>
                      <option value="MG">MG</option>
                      <option value="PA">PA</option>
                      <option value="PB">PB</option>
                      <option value="PR">PR</option>
                      <option value="PE">PE</option>
                      <option value="PI">PI</option>
                      <option value="RJ">RJ</option>
                      <option value="RN">RN</option>
                      <option value="RS">RS</option>
                      <option value="RO">RO</option>
                      <option value="RR">RR</option>
                      <option value="SC">SC</option>
                      <option value="SP">SP</option>
                      <option value="SE">SE</option>
                      <option value="TO">TO</option>
                    </select>
                  </div>
                </div>
              </div>

              {error && (
                <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                  <p className="text-sm text-red-700 font-medium">{error}</p>
                </div>
              )}

              <div className="flex gap-3 pt-6 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setActiveTab("existing")}
                  disabled={isCreating}
                  className="flex-1 px-6 py-3 text-gray-700 border border-gray-300 rounded-xl hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed transition-all duration-200 font-medium"
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
                  className="flex-1 px-6 py-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-xl hover:from-blue-600 hover:to-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-all duration-200 font-medium shadow-lg hover:shadow-xl disabled:shadow-none"
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
