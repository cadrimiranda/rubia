import React from "react";
import { User, Circle } from "lucide-react";
import type { Donor } from "../../types/types";
import { getStatusColor } from "../../utils";

interface ChatHeaderProps {
  donor: Donor;
  onDonorInfoClick: () => void;
}

export const ChatHeader: React.FC<ChatHeaderProps> = ({
  donor,
  onDonorInfoClick,
}) => {
  return (
    <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <div
        className="flex items-center gap-4 cursor-pointer"
        onClick={onDonorInfoClick}
      >
        <div className="relative">
          <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
            <User className="w-4 h-4 text-white" />
          </div>
          <Circle
            className={`absolute -bottom-1 -right-1 w-3 h-3 ${getStatusColor(
              donor.status
            )} bg-white rounded-full`}
            fill="currentColor"
          />
        </div>

        <div>
          <h2 className="text-base font-medium text-gray-800 m-0 hover:text-blue-600 transition-colors">
            {donor.name}
          </h2>
          <p className="text-xs text-gray-500 m-0">
            Tipo sanguíneo: {donor.bloodType} • Última doação:{" "}
            {donor.lastDonation}
          </p>
        </div>
      </div>
    </div>
  );
};
