import { User, X } from "lucide-react";
import type { User as TUser } from "../../types";
import {
  calculateAge,
  formatWeight,
  formatHeight,
  calculateBMI,
  getBMICategory,
} from "../../utils/format";

type TDonorModal = {
  handleClose: () => void;
  donor: TUser;
};

const DonorModal = ({ handleClose, donor }: TDonorModal) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-96 max-h-96 flex flex-col">
        <div className="p-4 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-800">
            Informações do Doador
          </h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 space-y-4 overflow-y-auto">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-blue-500 rounded-full flex items-center justify-center">
              <User className="w-8 h-8 text-white" />
            </div>
            <div>
              <h3 className="font-medium text-gray-800">{donor.name}</h3>
              <p className="text-sm text-gray-500">Doador ativo</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-500">Tipo sanguíneo:</span>
              <p className="font-medium text-gray-800">{donor.bloodType}</p>
            </div>
            <div>
              <span className="text-gray-500">Idade:</span>
              <p className="font-medium text-gray-800">
                {donor.birthDate ? calculateAge(donor.birthDate) : "-"} anos
              </p>
            </div>
            <div>
              <span className="text-gray-500">Peso:</span>
              <p className="font-medium text-gray-800">
                {donor.weight ? formatWeight(donor.weight) : "-"}
              </p>
            </div>
            <div>
              <span className="text-gray-500">Altura:</span>
              <p className="font-medium text-gray-800">
                {donor.height ? formatHeight(donor.height) : "-"}
              </p>
            </div>
            {donor.weight && donor.height && (
              <div>
                <span className="text-gray-500">IMC:</span>
                <p className="font-medium text-gray-800">
                  {calculateBMI(donor.weight, donor.height)} (
                  {getBMICategory(calculateBMI(donor.weight, donor.height))})
                </p>
              </div>
            )}
            <div className="col-span-2">
              <span className="text-gray-500">Total de doações:</span>
              <p className="font-medium text-gray-800">
                {donor.totalDonations} doações
              </p>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">Última doação:</span>
              <p className="font-medium text-gray-800">{donor.lastDonation}</p>
            </div>
          </div>

          <div>
            <span className="text-gray-500 text-sm">Endereço:</span>
            <p className="font-medium text-gray-800 text-sm">{donor.address}</p>
          </div>

          <div>
            <span className="text-gray-500 text-sm">Contato:</span>
            <p className="font-medium text-gray-800 text-sm">{donor.phone}</p>
            <p className="font-medium text-gray-800 text-sm">{donor.email}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export { DonorModal };