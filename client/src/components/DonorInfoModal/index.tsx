import React from "react";
import { X, User, MapPin, Phone, Heart, Calendar, Ruler, Weight } from "lucide-react";
import type { Donor } from "../../types/types";
import { calculateAge } from "../../utils";

interface DonorInfoModalProps {
  show: boolean;
  donor: Donor | null;
  onClose: () => void;
}

export const DonorInfoModal: React.FC<DonorInfoModalProps> = ({
  show,
  donor,
  onClose,
}) => {
  if (!show || !donor) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-[600px] max-h-[90vh] flex flex-col shadow-2xl">
        <div className="p-6 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">
            Informações do Doador
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors p-2 rounded-full hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6 overflow-y-auto">
          {/* Header com foto e nome */}
          <div className="flex items-center gap-6">
            <div className="w-20 h-20 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center shadow-lg">
              <User className="w-10 h-10 text-white" />
            </div>
            <div>
              <h3 className="text-2xl font-semibold text-gray-900">{donor.name}</h3>
              <div className="flex items-center gap-2 mt-1">
                <Heart className="w-4 h-4 text-red-500" />
                <span className="text-gray-600">Doador cadastrado</span>
              </div>
            </div>
          </div>

          {/* Seção Informações Médicas */}
          <div className="bg-gradient-to-r from-red-50 to-pink-50 rounded-xl p-5 border border-red-100">
            <div className="flex items-center gap-2 mb-4">
              <Heart className="w-5 h-5 text-red-600" />
              <h4 className="text-lg font-semibold text-gray-900">Informações Médicas</h4>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-white rounded-lg p-4 shadow-sm">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                  <span className="text-sm font-medium text-gray-600">Tipo Sanguíneo</span>
                </div>
                <p className="text-xl font-bold text-gray-900">{donor.bloodType || 'Não informado'}</p>
              </div>

              <div className="bg-white rounded-lg p-4 shadow-sm">
                <div className="flex items-center gap-2 mb-2">
                  <Calendar className="w-3 h-3 text-blue-500" />
                  <span className="text-sm font-medium text-gray-600">Idade</span>
                </div>
                <p className="text-xl font-bold text-gray-900">
                  {donor.birthDate ? `${calculateAge(donor.birthDate)} anos` : 'Não informado'}
                </p>
              </div>

              <div className="bg-white rounded-lg p-4 shadow-sm">
                <div className="flex items-center gap-2 mb-2">
                  <Weight className="w-3 h-3 text-green-500" />
                  <span className="text-sm font-medium text-gray-600">Peso</span>
                </div>
                <p className="text-xl font-bold text-gray-900">
                  {donor.weight && donor.weight > 0 ? `${donor.weight} kg` : 'Não informado'}
                </p>
              </div>

              <div className="bg-white rounded-lg p-4 shadow-sm">
                <div className="flex items-center gap-2 mb-2">
                  <Ruler className="w-3 h-3 text-purple-500" />
                  <span className="text-sm font-medium text-gray-600">Altura</span>
                </div>
                <p className="text-xl font-bold text-gray-900">
                  {donor.height && donor.height > 0 ? `${donor.height} cm` : 'Não informado'}
                </p>
              </div>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-4">
              <div className="bg-white rounded-lg p-4 shadow-sm">
                <span className="text-sm font-medium text-gray-600">Total de Doações</span>
                <p className="text-lg font-bold text-gray-900 mt-1">
                  {donor.totalDonations || 0} doações
                </p>
              </div>

              <div className="bg-white rounded-lg p-4 shadow-sm">
                <span className="text-sm font-medium text-gray-600">Última Doação</span>
                <p className="text-lg font-bold text-gray-900 mt-1">
                  {donor.lastDonation || 'Sem registro'}
                </p>
              </div>
            </div>
          </div>

          {/* Seção Contato */}
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-5 border border-blue-100">
            <div className="flex items-center gap-2 mb-4">
              <Phone className="w-5 h-5 text-blue-600" />
              <h4 className="text-lg font-semibold text-gray-900">Contato</h4>
            </div>
            
            <div className="bg-white rounded-lg p-4 shadow-sm">
              <div className="flex items-center gap-2 mb-2">
                <Phone className="w-4 h-4 text-blue-500" />
                <span className="text-sm font-medium text-gray-600">Telefone</span>
              </div>
              <p className="text-lg font-semibold text-gray-900">{donor.phone}</p>
              {donor.email && (
                <div className="mt-3">
                  <span className="text-sm font-medium text-gray-600">E-mail</span>
                  <p className="text-lg font-semibold text-gray-900 mt-1">{donor.email}</p>
                </div>
              )}
            </div>
          </div>

          {/* Seção Endereço */}
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-xl p-5 border border-green-100">
            <div className="flex items-center gap-2 mb-4">
              <MapPin className="w-5 h-5 text-green-600" />
              <h4 className="text-lg font-semibold text-gray-900">Endereço</h4>
            </div>
            
            <div className="bg-white rounded-lg p-4 shadow-sm">
              {donor.address && donor.address !== 'Não informado' ? (
                <div className="flex items-start gap-3">
                  <MapPin className="w-5 h-5 text-green-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-gray-900 font-medium leading-relaxed">{donor.address}</p>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-3 text-gray-500">
                  <MapPin className="w-5 h-5" />
                  <span>Endereço não informado</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
