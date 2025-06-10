import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Heart } from "lucide-react";
import Sidebar from "../components/Sidebar";
import ChatHeader from "../components/ChatHeader";
import ChatMessage from "../components/ChatMessage";
import ChatInput from "../components/ChatInput";
import ChatDataProvider from "../components/ChatDataProvider";
import UserHeader from "../components/UserHeader";
import { OfflineBanner } from "../components/ConnectionStatus";
import TypingIndicator from "../components/TypingIndicator";
import { useChatStore } from "../store/useChatStore";
import { useNotifications } from "../hooks/useNotifications";

const ChatPage = () => {
  const { activeChat, setActiveChat } = useChatStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [isMobile, setIsMobile] = useState(false);

  // Inicializar sistema de notificações
  useNotifications();

  useEffect(() => {
    // Auto scroll para a última mensagem
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [activeChat?.messages]);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener("resize", checkMobile);
    return () => window.removeEventListener("resize", checkMobile);
  }, []);

  const handleBackToList = () => {
    setActiveChat(null);
  };

  const renderChatArea = () => {
    if (!activeChat) {
      return (
        <div className="flex-1 flex items-center justify-center bg-gray-50">
          <div className="text-center">
            <Heart className="w-16 h-16 text-gray-300 mb-4 mx-auto" />
            <h3 className="text-xl text-gray-400 mb-2 font-medium">
              Selecione um doador
            </h3>
            <p className="text-gray-400 m-0">
              Escolha uma conversa existente ou inicie uma nova
            </p>
          </div>
        </div>
      );
    }

    return (
      <div className="flex-1 flex flex-col">
        {/* Mobile Header with Back Button */}
        {isMobile && (
          <div className="flex items-center px-4 py-2 bg-white border-b border-neutral-200 md:hidden">
            <button
              onClick={handleBackToList}
              className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg mr-2"
            >
              <ArrowLeft size={20} />
            </button>
            <span className="font-medium text-neutral-700">Voltar</span>
          </div>
        )}

        <ChatHeader chat={activeChat} />

        <div className="flex-1 overflow-y-auto">
          <div className="w-full max-w-none px-4 py-6">
            <div className="space-y-2">
              {activeChat.messages.map((message) => (
                <ChatMessage key={message.id} message={message} />
              ))}
              <TypingIndicator conversationId={activeChat.id} />
              <div ref={messagesEndRef} />
            </div>
          </div>
        </div>

        <ChatInput />
      </div>
    );
  };

  return (
    <ChatDataProvider>
      <div className="h-screen w-full flex bg-gradient-to-br from-rose-50 to-rose-100">
        {/* Sidebar - Full height on the left */}
        <div
          className={`${
            isMobile && activeChat ? "hidden" : "block"
          } w-full md:w-80 lg:w-96 xl:w-80 flex-shrink-0 h-full`}
        >
          <Sidebar />
        </div>

        {/* Main Content Area - Right side */}
        <div
          className={`${
            isMobile && !activeChat ? "hidden" : "flex-1"
          } min-w-0 flex flex-col h-full`}
        >
          {/* User Header - Only in main area */}
          <UserHeader />

          {/* Offline Banner */}
          <OfflineBanner />

          {/* Chat Area */}
          <div className="flex-1 overflow-hidden">
            {renderChatArea()}
          </div>
        </div>
      </div>
    </ChatDataProvider>
  );
};

export default ChatPage;
