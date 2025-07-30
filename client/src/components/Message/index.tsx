import React from "react";
import type { Message as MessageType } from "../../types/types";
import { FileAttachment } from "../FileAttachment";
import { MediaPreview } from "../MediaPreview";
import { AudioMessage } from "../AudioMessage";

interface MessageProps {
  message: MessageType;
}

export const Message: React.FC<MessageProps> = ({ message }) => {
  // isAI = true significa mensagem recebida do cliente (esquerda, azul)  
  // isAI = false significa mensagem enviada por mim/sistema (direita, branco)
  const isFromCustomer = message.isAI;
  
  // Debug log para investigar mensagens de áudio
  if (message.messageType?.toLowerCase() === "audio" || message.mediaUrl || (message.media && message.media.length > 0)) {
    console.log("🎵 Audio message detected:", {
      messageType: message.messageType,
      messageTypeLower: message.messageType?.toLowerCase(),
      mediaUrl: message.mediaUrl,
      mimeType: message.mimeType,
      media: message.media,
      content: message.content,
      isAudioConditionMet: (message.messageType && message.messageType.toLowerCase() === "audio" && message.mediaUrl)
    });
  }
  
  return (
    <div className={`flex ${isFromCustomer ? "justify-start" : "justify-end"}`}>
      <div
        className={`max-w-[50%] px-4 py-2 rounded-2xl ${
          isFromCustomer
            ? "bg-blue-500 text-white"
            : "bg-white text-gray-800 shadow-sm"
        }`}
      >
        {/* Check if it's an audio message (either direct or in media array) */}
        {(message.messageType && message.messageType.toLowerCase() === "audio" && message.mediaUrl) || 
         (message.media && message.media.length > 0 && message.media[0].mediaType === "AUDIO") ? (
          <AudioMessage
            audioUrl={message.mediaUrl || message.media?.[0]?.fileUrl || ""}
            duration={message.audioDuration}
            isFromCustomer={isFromCustomer}
            mimeType={message.mimeType || message.media?.[0]?.mimeType}
          />
        ) : (
          <>
            {message.content && (
              <p
                className={`m-0 mb-2 ${
                  isFromCustomer ? "text-white" : "text-gray-800"
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
                    isAI={isFromCustomer}
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
          </>
        )}

        <div className="mt-1">
          <span
            className={`text-xs ${
              isFromCustomer ? "text-blue-100" : "text-gray-400"
            }`}
          >
            {message.timestamp}
          </span>
        </div>
      </div>
    </div>
  );
};
