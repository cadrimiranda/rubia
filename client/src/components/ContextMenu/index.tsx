import React from "react";
import { MessageSquare, Calendar, User, Archive, UserX } from "lucide-react";
import type { ContextMenu as ContextMenuType } from "../../types/types";

interface ContextMenuProps {
  contextMenu: ContextMenuType;
  onAction: (action: string, donorId: string) => void;
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
  contextMenu,
  onAction,
}) => {
  if (!contextMenu.show) return null;

  return (
    <div
      className="fixed bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50 min-w-48"
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
