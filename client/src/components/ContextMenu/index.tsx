import React from "react";
import { MessageSquare, Calendar, User, Archive, UserX, CheckCircle, Clock, XCircle } from "lucide-react";
import type { ContextMenu as ContextMenuType } from "../../types/types";
import type { ChatStatus } from "../../types/index";

interface ContextMenuProps {
  contextMenu: ContextMenuType;
  onAction: (action: string, donorId: string) => void;
  currentStatus?: ChatStatus;
  onStatusChange?: (donorId: string, newStatus: ChatStatus) => void;
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
  contextMenu,
  onAction,
  currentStatus,
  onStatusChange,
}) => {
  if (!contextMenu.show) return null;

  return (
    <div
      className="fixed bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50 min-w-52"
      style={{ left: contextMenu.x, top: contextMenu.y }}
    >
      <button
        onClick={() => onAction("view-conversation", contextMenu.donorId)}
        className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
      >
        <MessageSquare className="w-4 h-4" />
        Ver conversa
      </button>
      <button
        onClick={() => onAction("schedule-donation", contextMenu.donorId)}
        className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
      >
        <Calendar className="w-4 h-4" />
        Agendar doação
      </button>
      <button
        onClick={() => onAction("view-profile", contextMenu.donorId)}
        className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
      >
        <User className="w-4 h-4" />
        Ver perfil
      </button>

      {onStatusChange && (
        <>
          <div className="border-t border-gray-200 my-1" />
          <div className="px-3 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide">
            Alterar Status
          </div>
          
          {currentStatus !== 'ativos' && (
            <button
              onClick={() => onStatusChange(contextMenu.donorId, 'ativos')}
              className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-green-50 flex items-center gap-2"
            >
              <CheckCircle className="w-4 h-4 text-green-600" />
              <div>
                <div className="font-medium">Marcar como Ativo</div>
                <div className="text-xs text-gray-500">Cliente respondeu</div>
              </div>
            </button>
          )}

          {currentStatus !== 'aguardando' && (
            <button
              onClick={() => onStatusChange(contextMenu.donorId, 'aguardando')}
              className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-yellow-50 flex items-center gap-2"
            >
              <Clock className="w-4 h-4 text-yellow-600" />
              <div>
                <div className="font-medium">Marcar como Aguardando</div>
                <div className="text-xs text-gray-500">Aguardando resposta</div>
              </div>
            </button>
          )}

          {currentStatus !== 'inativo' && (
            <button
              onClick={() => onStatusChange(contextMenu.donorId, 'inativo')}
              className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-red-50 flex items-center gap-2"
            >
              <XCircle className="w-4 h-4 text-red-600" />
              <div>
                <div className="font-medium">Marcar como Inativo</div>
                <div className="text-xs text-gray-500">Sem mais contato</div>
              </div>
            </button>
          )}
        </>
      )}

      <div className="border-t border-gray-200 my-1" />
      <button
        onClick={() => onAction("archive-conversation", contextMenu.donorId)}
        className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
      >
        <Archive className="w-4 h-4" />
        Arquivar conversa
      </button>
      <button
        onClick={() => onAction("block-contact", contextMenu.donorId)}
        className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
      >
        <UserX className="w-4 h-4" />
        Bloquear contato
      </button>
    </div>
  );
};
