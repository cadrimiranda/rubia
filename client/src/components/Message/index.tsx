import React from "react";
import type { Message as MessageType } from "../../types/types";
import { FileAttachment } from "../FileAttachment";
import { MediaPreview } from "../MediaPreview";
import { AvatarDisplay } from "../AvatarDisplay";

interface MessageProps {
  message: MessageType;
  agentAvatar?: string; // Base64 do avatar do agente IA
}

export const Message: React.FC<MessageProps> = ({ message, agentAvatar }) => {
  return (
    <div className={`flex gap-3 ${message.isAI ? "justify-start" : "justify-end"}`}>
      {/* Avatar do agente IA (apenas para mensagens da IA) */}
      {message.isAI && (
        <div className="flex-shrink-0">
          <AvatarDisplay
            avatarBase64={agentAvatar}
            size={32}
            className="border border-gray-200"
            alt="Agente IA"
          />
        </div>
      )}
      
      <div
        className={`max-w-[50%] px-4 py-2 rounded-2xl ${
          message.isAI
            ? "bg-white text-gray-800 shadow-sm border border-gray-100"
            : "bg-blue-500 text-white"
        }`}
      >
        {message.content && (
          <p
            className={`m-0 mb-2 ${
              message.isAI ? "text-gray-800" : "text-white"
            }`}
          >
            {message.content}
          </p>
        )}

        {message.attachments && message.attachments.length > 0 && (
          <div className="space-y-2">
            {message.attachments.map((attachment) => (
              <FileAttachment
                key={attachment.id}
                attachment={attachment}
                isAI={message.isAI}
                variant="message"
              />
            ))}
          </div>
        )}

        {message.media && message.media.length > 0 && (
          <div className="space-y-2">
            {message.media.map((mediaItem) => (
              <MediaPreview
                key={mediaItem.id}
                media={mediaItem}
                variant="message"
              />
            ))}
          </div>
        )}

        <div className="mt-1">
          <span
            className={`text-xs ${
              message.isAI ? "text-gray-400" : "text-blue-100"
            }`}
          >
            {message.timestamp}
          </span>
        </div>
      </div>
    </div>
  );
};
