import { useEffect, useRef, useState } from "react";
import { MessageCircle, ArrowLeft } from "lucide-react";
import Sidebar from "../components/Sidebar";
import ChatHeader from "../components/ChatHeader";
import ChatMessage from "../components/ChatMessage";
import ChatInput from "../components/ChatInput";
import { useChatStore } from "../store/useChatStore";
import { initializeMockData } from "../mocks/data";

const ChatPage = () => {
  const { activeChat, setActiveChat } = useChatStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    // Inicializar dados mock
    const mockData = initializeMockData();
    // Simular carregamento dos chats
    useChatStore.setState({ chats: mockData });
  }, []);

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
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center px-8">
            <div className="w-24 h-24 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-6 shadow-lg">
              <MessageCircle size={40} className="text-rose-500" />
            </div>
            <h3 className="text-xl font-medium text-rose-800 mb-3">
              Bem-vindo ao Chat Corporativo
            </h3>
            <p className="text-rose-600 max-w-sm">
              Selecione uma conversa na lista ao lado para começar a atender
              seus clientes
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
              <div ref={messagesEndRef} />
            </div>
          </div>
        </div>

        <ChatInput />
      </div>
    );
  };

  return (
    <div className="h-screen w-full flex bg-gradient-to-br from-rose-50 to-rose-100">
      {/* Sidebar - Hidden on mobile when chat is active */}
      <div
        className={`${
          isMobile && activeChat ? "hidden" : "block"
        } w-full md:w-80 lg:w-96 xl:w-80 flex-shrink-0`}
      >
        <Sidebar />
      </div>

      {/* Chat Area - Full width on mobile when active */}
      <div
        className={`${
          isMobile && !activeChat ? "hidden" : "flex-1"
        } min-w-0 flex`}
      >
        {renderChatArea()}
      </div>
    </div>
  );
};

export default ChatPage;
