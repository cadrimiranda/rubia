import { Check, CheckCheck, Clock } from "lucide-react";
import type { Message } from "../../types";
import { formatMessageTime } from "../../utils/format";

interface ChatMessageProps {
  message: Message;
  showTime?: boolean;
}

const ChatMessage = ({ message }: ChatMessageProps) => {
  const isFromUser = message.isFromUser;

  const getStatusIcon = () => {
    if (!isFromUser) return null;

    switch (message.status) {
      case "sending":
        return <Clock size={12} className="text-white" />;
      case "sent":
        return <Check size={12} className="text-white" />;
      case "delivered":
        return <CheckCheck size={12} className="text-white" />;
      case "read":
        return <CheckCheck size={12} className="text-white" />;
      default:
        return null;
    }
  };

  return (
    <div
      className={`flex ${
        isFromUser ? "justify-end" : "justify-start"
      } mb-3 px-1`}
    >
      <div
        className={`relative max-w-xs lg:max-w-md px-4 py-3 shadow-md transition-all duration-200 ${
          isFromUser
            ? "bg-emerald-600 text-white rounded-2xl rounded-br-md border border-rose-700"
            : "bg-white text-gray-700 rounded-2xl rounded-bl-md border border-gray-200"
        }`}
      >
        <p className="text-sm leading-relaxed whitespace-pre-wrap">
          {message.content}
        </p>

        <div className="flex items-center justify-end mt-2 gap-2">
          <span
            className={`text-xs font-medium ${
              isFromUser ? "text-white/90" : "text-gray-400"
            }`}
          >
            {formatMessageTime(message.timestamp)}
          </span>
          {getStatusIcon()}
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;
