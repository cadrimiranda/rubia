import { Avatar, Dropdown, Tooltip } from "antd";
import {
  MoreVertical,
  Phone,
  Video,
  Search,
  MessageCircle,
  User,
} from "lucide-react";
import type { Chat } from "../../types";
import { formatLastSeen } from "../../utils/format";
import ChatTags from "../ChatTags";
import MessageOptionsMenu from "../MessageOptionsMenu";

interface ChatHeaderProps {
  chat: Chat;
}

const ChatHeader = ({ chat }: ChatHeaderProps) => {
  const getOnlineStatus = () => {
    if (chat.contact.isOnline) {
      return "Online agora";
    }
    if (chat.contact.lastSeen) {
      return `Visto por último ${formatLastSeen(chat.contact.lastSeen)}`;
    }
    return "Offline";
  };

  return (
    <div
      style={{ borderBottom: "1px solid #F8D3D3" }}
      className="flex items-center justify-between px-4 py-4"
    >
      <div className="flex items-center space-x-3 flex-1 min-w-0">
        <div className="relative flex-shrink-0">
          <Avatar
            src={chat.contact.avatar}
            size={36}
            className="ring-1 ring-neutral-200"
          >
            {chat.contact.name.charAt(0).toUpperCase()}
          </Avatar>

          {/* Status Badge */}
          <div className="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-success-500 rounded-full flex items-center justify-center border-2 border-white">
            <MessageCircle size={7} className="text-white" />
          </div>

          {/* Online Indicator */}
          {chat.contact.isOnline && (
            <div className="absolute top-0 right-0 w-2.5 h-2.5 bg-green-400 border-2 border-white rounded-full"></div>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <h2 className="font-medium text-neutral-900 text-base truncate">
              {chat.contact.name}
            </h2>
            <ChatTags tags={chat.tags} compact />
          </div>
          <div className="flex items-center gap-2">
            <p className="text-xs text-neutral-500 truncate">
              {getOnlineStatus()}
            </p>
            {chat.contact.phone && (
              <span className="text-xs text-neutral-400">
                • {chat.contact.phone}
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="flex items-center space-x-1 flex-shrink-0">
        <Tooltip title="Ver perfil">
          <button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg transition-all duration-200">
            <User size={18} />
          </button>
        </Tooltip>

        <Tooltip title="Ligar">
          <button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg transition-all duration-200">
            <Phone size={18} />
          </button>
        </Tooltip>

        <Tooltip title="Videochamada">
          <button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg transition-all duration-200">
            <Video size={18} />
          </button>
        </Tooltip>

        <Tooltip title="Buscar mensagens">
          <button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg transition-all duration-200">
            <Search size={18} />
          </button>
        </Tooltip>

        <Dropdown
          overlay={<MessageOptionsMenu chat={chat} />}
          trigger={["click"]}
          placement="bottomRight"
        >
          <Tooltip title="Mais opções">
            <button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg transition-all duration-200">
              <MoreVertical size={18} />
            </button>
          </Tooltip>
        </Dropdown>
      </div>
    </div>
  );
};

export default ChatHeader;
