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
  
  return (
    <div className={`flex ${isFromCustomer ? "justify-start" : "justify-end"}`}>
      <div
        className={`max-w-[50%] px-4 py-2 rounded-2xl ${
          isFromCustomer
            ? "bg-blue-500 text-white"
            : "bg-white text-gray-800 shadow-sm"
        }`}
      >
        {message.messageType === "audio" && message.mediaUrl ? (
          <AudioMessage
            audioUrl={message.mediaUrl}
            duration={message.audioDuration}
            isFromCustomer={isFromCustomer}
            mimeType={message.mimeType}
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
