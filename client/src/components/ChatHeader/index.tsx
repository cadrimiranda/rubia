import { User, Circle } from "lucide-react";
import type { Chat } from "../../types";
import { useState } from "react";
import { DonorModal } from "../DonorModal";

interface ChatHeaderProps {
  chat: Chat;
}

const ChatHeader = ({ chat }: ChatHeaderProps) => {
  const { contact } = chat;
  const [showDonorInfo, setShowDonorInfo] = useState(false);

  const getStatusColor = (status: boolean) => {
    return status ? "text-green-500" : "text-gray-400";
  };

  return (
    <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <div
        className="flex items-center gap-4 cursor-pointer"
        onClick={() => setShowDonorInfo(true)}
      >
        <div className="relative">
          <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
            <User className="w-4 h-4 text-white" />
          </div>
          <Circle
            className={`absolute -bottom-1 -right-1 w-3 h-3 ${getStatusColor(
              contact.isOnline
            )} bg-white rounded-full`}
            fill="currentColor"
          />
        </div>

        <div>
          <h2 className="text-base font-medium text-gray-800 m-0 hover:text-blue-600 transition-colors">
            {contact.name}
          </h2>
          <p className="text-xs text-gray-500 m-0">
            Tipo sanguíneo: {contact.bloodType} • Última doação:{" "}
            {contact.lastDonation}
          </p>
        </div>
      </div>
      {showDonorInfo && (
        <DonorModal
          handleClose={() => setShowDonorInfo(false)}
          donor={contact}
        />
      )}
    </div>
  );
};

export default ChatHeader;
