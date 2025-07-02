import React, { createContext, useEffect } from "react";
import type { ReactNode } from "react";
import { useAuthStore } from "../../store/useAuthStore";

interface AuthProviderProps {
  children: ReactNode;
}

const AuthContext = createContext({});

export const useAuth = () => {
  return useAuthStore();
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const initialize = useAuthStore(state => state.initialize);

  useEffect(() => {
    console.log('🚀 AuthProvider: Inicializando auth store');
    initialize();
  }, []); // Remover dependency array para garantir que só execute uma vez

  return <AuthContext.Provider value={{}}>{children}</AuthContext.Provider>;
};
