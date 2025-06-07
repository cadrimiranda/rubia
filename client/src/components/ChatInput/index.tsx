import { useState, useEffect, useRef } from "react";
import { Input, Tooltip } from "antd";
import { Smile, Paperclip, Mic, Send, FileText, Plus } from "lucide-react";
import { useChatStore } from "../../store/useChatStore";
import { webSocketEventHandlers } from "../../websocket/eventHandlers";

const ChatInput = () => {
  const [message, setMessage] = useState("");
  const [activeTab, setActiveTab] = useState<"message" | "notes">("message");
  const activeChat = useChatStore((state) => state.activeChat);
  const sendMessage = useChatStore((state) => state.sendMessage);
  const isSending = useChatStore((state) => state.isSending);
  const typingTimeoutRef = useRef<number | undefined>(undefined);
  const isTypingRef = useRef(false);

  const handleSendMessage = async () => {
    if (!message.trim() || !activeChat || isSending) return;

    try {
      // Para de enviar indicador de digitação
      if (isTypingRef.current) {
        webSocketEventHandlers.stopTyping(activeChat.id);
        isTypingRef.current = false;
      }
      
      await sendMessage(activeChat.id, message.trim());
      setMessage("");
    } catch (error) {
      console.error('Erro ao enviar mensagem:', error);
      // A mensagem permanece no input para o usuário tentar novamente
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };
  
  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newMessage = e.target.value;
    setMessage(newMessage);
    
    if (!activeChat) return;
    
    // Gerenciar indicador de digitação
    if (newMessage.trim() && !isTypingRef.current) {
      // Começou a digitar
      webSocketEventHandlers.sendTyping(activeChat.id);
      isTypingRef.current = true;
    }
    
    // Reset do timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    
    // Para de enviar indicador após 3 segundos de inatividade
    typingTimeoutRef.current = setTimeout(() => {
      if (isTypingRef.current) {
        webSocketEventHandlers.stopTyping(activeChat.id);
        isTypingRef.current = false;
      }
    }, 3000);
    
    // Se o input estiver vazio, para imediatamente
    if (!newMessage.trim() && isTypingRef.current) {
      webSocketEventHandlers.stopTyping(activeChat.id);
      isTypingRef.current = false;
    }
  };
  
  // Cleanup do typing indicator quando componente desmonta ou chat muda
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      if (isTypingRef.current && activeChat) {
        webSocketEventHandlers.stopTyping(activeChat.id);
        isTypingRef.current = false;
      }
    };
  }, [activeChat]);

  const handleEmojiClick = () => {
    console.log("Abrir seletor de emoji");
  };

  const handleAttachmentClick = () => {
    console.log("Abrir seletor de anexo");
  };

  const handleAudioClick = () => {
    console.log("Gravar áudio");
  };

  if (!activeChat) {
    return (
      <div className="p-4">
        <div className="bg-white rounded-3xl shadow-lg border border-gray-200 p-6">
          <div className="text-center text-ruby-600">
            <p className="text-sm">Inicie a conversa enviando uma mensagem…</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4">
      <div className="bg-white rounded-3xl shadow-lg border border-gray-200 p-4">
        {/* Tabs Section */}
        <div className="flex items-center justify-between pb-2">
          <div className="flex gap-1">
            <button
              onClick={() => setActiveTab("message")}
              className={`h-8 px-3 text-sm font-medium rounded-xl transition-all duration-200 ${
                activeTab === "message"
                  ? "bg-ruby-500 text-white shadow-sm"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              Mensagem
            </button>
            <button
              onClick={() => setActiveTab("notes")}
              className={`h-8 px-3 text-sm font-medium rounded-xl transition-all duration-200 flex items-center gap-2 ${
                activeTab === "notes"
                  ? "bg-ruby-500 text-white shadow-sm"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              <FileText size={12} />
              Notas
            </button>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-1">
            <Tooltip title="Respostas rápidas">
              <button className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all duration-200">
                <Plus size={16} />
              </button>
            </Tooltip>
          </div>
        </div>

        {/* Input Area = Área do Chat */}
        <div className="bg-white rounded-2xl space-y-3 border border-gray-100">
          {activeTab === "message" ? (
            <>
              <div className="flex items-end gap-3">
                <div className="flex-1">
                  <Input.TextArea
                    value={message}
                    onChange={handleInputChange}
                    onKeyPress={handleKeyPress}
                    placeholder="Digite sua mensagem…"
                    autoSize={{ minRows: 1, maxRows: 4 }}
                    className="!border-0 bg-white resize-none rounded-2xl"
                    style={{
                      fontSize: "14px",
                      lineHeight: "1.5",
                      borderRadius: "20px",
                      padding: "14px 18px",
                      border: "1px solid rgba(0, 0, 0, 0.06)",
                      outline: "none",
                      boxShadow: "none",
                    }}
                  />
                </div>

                {/* Send Button */}
                <div className="flex-shrink-0">
                  {message.trim() && (
                    <Tooltip title={isSending ? "Enviando..." : "Enviar (⏎)"}>
                      <button
                        onClick={handleSendMessage}
                        disabled={isSending}
                        className={`w-10 h-10 text-white rounded-full transition-all duration-200 flex items-center justify-center shadow-md ${
                          isSending 
                            ? "bg-gray-400 cursor-not-allowed" 
                            : "bg-ruby-500 hover:bg-ruby-600 hover:shadow-lg transform hover:scale-105"
                        }`}
                      >
                        {isSending ? (
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <Send size={18} />
                        )}
                      </button>
                    </Tooltip>
                  )}
                </div>
              </div>

              {/* Action Buttons Row */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1">
                  <Tooltip title="Adicionar emoji">
                    <button
                      onClick={handleEmojiClick}
                      className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200"
                    >
                      <Smile size={20} />
                    </button>
                  </Tooltip>

                  <Tooltip title="Anexar arquivo">
                    <button
                      onClick={handleAttachmentClick}
                      className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200"
                    >
                      <Paperclip size={20} />
                    </button>
                  </Tooltip>

                  <Tooltip title="Adicionar GIF">
                    <button className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200">
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <rect
                          x="3"
                          y="3"
                          width="18"
                          height="18"
                          rx="2"
                          ry="2"
                        />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <polyline points="21,15 16,10 5,21" />
                      </svg>
                    </button>
                  </Tooltip>

                  <Tooltip title="Adicionar imagem">
                    <button className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200">
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <rect
                          x="3"
                          y="3"
                          width="18"
                          height="18"
                          rx="2"
                          ry="2"
                        />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <polyline points="21,15 16,10 5,21" />
                      </svg>
                    </button>
                  </Tooltip>

                  <Tooltip title="Adicionar documento">
                    <button className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200">
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14,2 14,8 20,8" />
                        <line x1="16" y1="13" x2="8" y2="13" />
                        <line x1="16" y1="17" x2="8" y2="17" />
                        <polyline points="10,9 9,9 8,9" />
                      </svg>
                    </button>
                  </Tooltip>
                </div>

                {/* Voice Button on opposite side */}
                <div className="flex-shrink-0">
                  <Tooltip title="Gravar áudio">
                    <button
                      onClick={handleAudioClick}
                      className="p-2 text-ruby-500 hover:bg-ruby-500 hover:text-white rounded-lg transition-all duration-200"
                    >
                      <Mic size={20} />
                    </button>
                  </Tooltip>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-8 text-gray-500">
              <FileText size={32} className="mx-auto mb-3 opacity-50" />
              <p className="text-sm font-medium mb-1">Área de notas</p>
              <p className="text-xs">Adicione observações sobre este contato</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ChatInput;
