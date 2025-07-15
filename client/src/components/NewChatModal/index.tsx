import React, { useState } from "react";
import { Search, X, User, Circle, Plus, UserPlus, Loader, AlertCircle } from "lucide-react";
import { useForm } from "react-hook-form";
import type { Donor } from "../../types/types";
import { getStatusColor } from "../../utils";
import { customerApi } from "../../api/services/customerApi";
import { customerAdapter } from "../../adapters/customerAdapter";

interface FormData {
  name: string;
  phone: string;
  profileUrl?: string;
  birthDate?: string;
  lastDonationDate?: string;
  nextEligibleDonationDate?: string;
  bloodType?: string;
  height?: number;
  weight?: number;
  addressStreet?: string;
  addressNumber?: string;
  addressComplement?: string;
  addressPostalCode?: string;
  addressCity?: string;
  addressState?: string;
}

interface NewContactData {
  name: string;
  phone: string;
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
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
    reset
  } = useForm<FormData>({
    mode: "onChange",
    defaultValues: {
      name: "",
      phone: "",
      profileUrl: "",
      birthDate: "",
      lastDonationDate: "",
      nextEligibleDonationDate: "",
      bloodType: "",
      height: undefined,
      weight: undefined,
      addressStreet: "",
      addressNumber: "",
      addressComplement: "",
      addressPostalCode: "",
      addressCity: "",
      addressState: "",
    }
  });

  const onSubmit = async (data: FormData) => {
    setIsCreating(true);
    setError(null);

    try {
      // Verificar se já existe um cliente com este telefone
      const normalizedPhone = customerAdapter.normalizePhone(data.phone);
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
          bloodType: "O+",
          email: "",
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
          hasActiveConversation: false,
        };
        
        onNewContactCreate({
          name: data.name,
          phone: data.phone,
          donor: donor
        });
        
        onDonorSelect(donor);
      } else {
        // Criar novo cliente
        const createRequest = {
          phone: normalizedPhone,
          name: data.name,
          profileUrl: data.profileUrl,
          birthDate: data.birthDate,
          lastDonationDate: data.lastDonationDate,
          nextEligibleDonationDate: data.nextEligibleDonationDate,
          bloodType: data.bloodType,
          height: data.height,
          weight: data.weight,
          addressStreet: data.addressStreet,
          addressNumber: data.addressNumber,
          addressComplement: data.addressComplement,
          addressPostalCode: data.addressPostalCode,
          addressCity: data.addressCity,
          addressState: data.addressState,
        };
        
        const newCustomer = await customerApi.create(createRequest);
        
        const user = customerAdapter.toUser(newCustomer);
        const donor: Donor = {
          ...user,
          phone: user.phone || normalizedPhone,
          lastMessage: "",
          timestamp: "",
          unread: 0,
          status: "offline" as const,
          bloodType: "O+",
          email: "",
          lastDonation: "Sem registro",
          totalDonations: 0,
          address: "",
          birthDate: "",
          weight: 0,
          height: 0,
          hasActiveConversation: false,
        };
        
        onNewContactCreate({
          name: data.name,
          phone: data.phone,
          donor: donor
        });
        
        onDonorSelect(donor);
      }

      // Resetar formulário e fechar modal
      reset();
      setActiveTab("existing");
      onClose();

    } catch (error) {
      console.error("Erro ao criar contato:", error);
      setError(error instanceof Error ? error.message : "Erro ao criar contato");
    } finally {
      setIsCreating(false);
    }
  };

  const resetFormAndState = () => {
    reset();
    setActiveTab("existing");
    setError(null);
    setIsCreating(false);
  };

  const handleClose = () => {
    resetFormAndState();
    onClose();
  };

  if (!show) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-[750px] max-h-[90vh] flex flex-col shadow-2xl">
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
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Informações Básicas */}
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Informações Básicas</h3>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Nome completo *
                    </label>
                    <input
                      {...register("name", {
                        required: "Nome é obrigatório",
                        minLength: { value: 2, message: "Nome deve ter pelo menos 2 caracteres" },
                        maxLength: { value: 255, message: "Nome não pode exceder 255 caracteres" },
                        pattern: { 
                          value: /^[a-zA-ZÀ-ÿ\s]+$/, 
                          message: "Nome deve conter apenas letras e espaços" 
                        }
                      })}
                      placeholder="Digite o nome completo"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.name ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.name && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.name.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Telefone *
                    </label>
                    <input
                      {...register("phone", {
                        required: "Telefone é obrigatório",
                        validate: (value) => {
                          if (!customerAdapter.validateBrazilianPhone(value)) {
                            return "Formato inválido. Use: (11) 99999-9999";
                          }
                          return true;
                        }
                      })}
                      type="tel"
                      placeholder="(11) 99999-9999"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.phone ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.phone && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.phone.message}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    URL da foto de perfil
                  </label>
                  <input
                    {...register("profileUrl", {
                      pattern: {
                        value: /^https?:\/\/.+\..+/,
                        message: "URL inválida"
                      }
                    })}
                    type="url"
                    placeholder="https://exemplo.com/foto.jpg"
                    className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                      errors.profileUrl ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                    }`}
                  />
                  {errors.profileUrl && (
                    <div className="flex items-center gap-1 mt-1">
                      <AlertCircle className="w-4 h-4 text-red-500" />
                      <span className="text-sm text-red-600">{errors.profileUrl.message}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Informações Médicas */}
              <div className="bg-red-50 rounded-xl p-5">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Informações Médicas</h3>
                
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Data de nascimento
                    </label>
                    <input
                      {...register("birthDate", {
                        validate: (value) => {
                          if (value) {
                            const birthDate = new Date(value);
                            const today = new Date();
                            let age = today.getFullYear() - birthDate.getFullYear();
                            const monthDiff = today.getMonth() - birthDate.getMonth();
                            const dayDiff = today.getDate() - birthDate.getDate();
                            
                            // Ajuste para idade exata
                            if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
                              age--;
                            }
                            
                            if (age < 16) {
                              return "Idade deve ser maior de 16 anos";
                            }
                          }
                          return true;
                        }
                      })}
                      type="date"
                      min={(() => {
                        const date = new Date();
                        date.setFullYear(date.getFullYear() - 16);
                        return date.toISOString().split('T')[0];
                      })()}
                      max={new Date().toISOString().split('T')[0]}
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.birthDate ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.birthDate && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.birthDate.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Tipo sanguíneo
                    </label>
                    <select
                      {...register("bloodType")}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-white"
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
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Última doação
                    </label>
                    <input
                      {...register("lastDonationDate")}
                      type="date"
                      max={new Date().toISOString().split('T')[0]}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-white"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Próxima doação elegível
                    </label>
                    <input
                      {...register("nextEligibleDonationDate")}
                      type="date"
                      min={new Date().toISOString().split('T')[0]}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-white"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Altura (cm)
                    </label>
                    <input
                      {...register("height", {
                        min: { value: 100, message: "Altura mínima: 100cm" },
                        max: { value: 250, message: "Altura máxima: 250cm" }
                      })}
                      type="number"
                      min="100"
                      max="250"
                      placeholder="170"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.height ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.height && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.height.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Peso (kg)
                    </label>
                    <input
                      {...register("weight", {
                        min: { value: 30, message: "Peso mínimo: 30kg" },
                        max: { value: 200, message: "Peso máximo: 200kg" }
                      })}
                      type="number"
                      min="30"
                      max="200"
                      step="0.1"
                      placeholder="70.5"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.weight ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.weight && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.weight.message}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Endereço */}
              <div className="bg-green-50 rounded-xl p-5">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Endereço</h3>
                
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div className="md:col-span-3">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Rua/Avenida
                    </label>
                    <input
                      {...register("addressStreet", {
                        maxLength: { value: 255, message: "Máximo 255 caracteres" }
                      })}
                      type="text"
                      placeholder="Rua das Flores"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.addressStreet ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.addressStreet && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.addressStreet.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Número
                    </label>
                    <input
                      {...register("addressNumber", {
                        maxLength: { value: 20, message: "Máximo 20 caracteres" }
                      })}
                      type="text"
                      placeholder="123"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.addressNumber ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.addressNumber && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.addressNumber.message}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Complemento
                  </label>
                  <input
                    {...register("addressComplement", {
                      maxLength: { value: 255, message: "Máximo 255 caracteres" }
                    })}
                    type="text"
                    placeholder="Apartamento 45, Bloco B"
                    className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                      errors.addressComplement ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                    }`}
                  />
                  {errors.addressComplement && (
                    <div className="flex items-center gap-1 mt-1">
                      <AlertCircle className="w-4 h-4 text-red-500" />
                      <span className="text-sm text-red-600">{errors.addressComplement.message}</span>
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      CEP
                    </label>
                    <input
                      {...register("addressPostalCode", {
                        pattern: {
                          value: /^\d{5}-?\d{3}$/,
                          message: "Formato: 12345-678"
                        }
                      })}
                      type="text"
                      placeholder="01234-567"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.addressPostalCode ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.addressPostalCode && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.addressPostalCode.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Cidade
                    </label>
                    <input
                      {...register("addressCity", {
                        maxLength: { value: 100, message: "Máximo 100 caracteres" },
                        pattern: {
                          value: /^[a-zA-ZÀ-ÿ\s]+$/,
                          message: "Apenas letras e espaços"
                        }
                      })}
                      type="text"
                      placeholder="São Paulo"
                      className={`w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 ${
                        errors.addressCity ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white focus:border-blue-500'
                      }`}
                    />
                    {errors.addressCity && (
                      <div className="flex items-center gap-1 mt-1">
                        <AlertCircle className="w-4 h-4 text-red-500" />
                        <span className="text-sm text-red-600">{errors.addressCity.message}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Estado
                    </label>
                    <select
                      {...register("addressState")}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 bg-white"
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
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-5 h-5 text-red-500" />
                    <p className="text-sm text-red-700 font-medium">{error}</p>
                  </div>
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
                  disabled={isCreating || !isValid}
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
