import React from "react";
import { User, Circle, MoreVertical, Check, Calendar } from "lucide-react";
import { Dropdown, Button } from "antd";
import type { MenuProps } from "antd";
import type { Donor } from "../../types/types";
import type { ChatStatus } from "../../types/index";
import { getStatusColor } from "../../utils";

interface ChatHeaderProps {
  donor: Donor;
  onDonorInfoClick: () => void;
  currentStatus: ChatStatus;
  onStatusChange?: (donorId: string, newStatus: ChatStatus) => void;
  onScheduleClick?: (donorId: string) => void;
}

export const ChatHeader: React.FC<ChatHeaderProps> = ({
  donor,
  onDonorInfoClick,
  currentStatus,
  onStatusChange,
  onScheduleClick,
}) => {
  const getStatusInfo = (status: ChatStatus) => {
    switch (status) {
      case 'ativos':
        return { label: 'Ativo', color: 'bg-green-500', description: 'Cliente respondeu' };
      case 'aguardando':
        return { label: 'Aguardando', color: 'bg-yellow-500', description: 'Aguardando resposta do cliente' };
      case 'inativo':
        return { label: 'Inativo', color: 'bg-red-500', description: 'Cliente não quer mais contato' };
      default:
        return { label: 'Ativo', color: 'bg-green-500', description: 'Cliente respondeu' };
    }
  };

  const currentStatusInfo = getStatusInfo(currentStatus);

  const statusMenuItems: MenuProps['items'] = [
    {
      key: 'ativos',
      label: (
        <div className="flex items-center gap-3 px-2 py-1">
          <div className="w-3 h-3 bg-green-500 rounded-full"></div>
          <div>
            <div className="font-medium">Ativo</div>
            <div className="text-xs text-gray-500">Cliente respondeu</div>
          </div>
          {currentStatus === 'ativos' && <Check className="w-4 h-4 text-green-600 ml-auto" />}
        </div>
      ),
      onClick: () => onStatusChange?.(donor.id, 'ativos'),
    },
    {
      key: 'aguardando',
      label: (
        <div className="flex items-center gap-3 px-2 py-1">
          <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
          <div>
            <div className="font-medium">Aguardando</div>
            <div className="text-xs text-gray-500">Aguardando resposta do cliente</div>
          </div>
          {currentStatus === 'aguardando' && <Check className="w-4 h-4 text-yellow-600 ml-auto" />}
        </div>
      ),
      onClick: () => onStatusChange?.(donor.id, 'aguardando'),
    },
    {
      key: 'inativo',
      label: (
        <div className="flex items-center gap-3 px-2 py-1">
          <div className="w-3 h-3 bg-red-500 rounded-full"></div>
          <div>
            <div className="font-medium">Inativo</div>
            <div className="text-xs text-gray-500">Cliente não quer mais contato</div>
          </div>
          {currentStatus === 'inativo' && <Check className="w-4 h-4 text-red-600 ml-auto" />}
        </div>
      ),
      onClick: () => onStatusChange?.(donor.id, 'inativo'),
    },
  ];

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

      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className={`w-2 h-2 ${currentStatusInfo.color} rounded-full`}></div>
          <span className="text-sm font-medium text-gray-700">{currentStatusInfo.label}</span>
        </div>
        
        {onScheduleClick && (
          <Button
            type="primary"
            size="small"
            icon={<Calendar className="w-4 h-4" />}
            onClick={() => onScheduleClick(donor.id)}
            className="flex items-center gap-1"
          >
            Agendar
          </Button>
        )}
        
        {onStatusChange && (
          <Dropdown
            menu={{ items: statusMenuItems }}
            placement="bottomRight"
            trigger={['click']}
          >
            <Button 
              type="text" 
              icon={<MoreVertical className="w-4 h-4" />}
              className="flex items-center justify-center"
            />
          </Dropdown>
        )}
      </div>
    </div>
  );
};
