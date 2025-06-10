import React from "react";
import type { Message as MessageType } from "../../types/types";
import { FileAttachment } from "../FileAttachment";

interface MessageProps {
  message: MessageType;
}

export const Message: React.FC<MessageProps> = ({ message }) => {
  return (
    <div className={`flex ${message.isAI ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[50%] px-4 py-2 rounded-2xl ${
          message.isAI
            ? "bg-white text-gray-800 shadow-sm"
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
