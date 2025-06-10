import React, { createContext, useContext, useState } from "react";
import type { ReactNode } from "react";

interface User {
  id: string;
  name: string;
  email: string;
  role?: "ADMIN" | "SUPERVISOR" | "AGENT";
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (
    email: string,
    password: string,
    rememberMe?: boolean
  ) => Promise<boolean>;
  logout: () => void;
}

interface AuthProviderProps {
  children: ReactNode;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const login = async (
    email: string,
    password: string,
    rememberMe: boolean = false
  ): Promise<boolean> => {
    setIsLoading(true);

    try {
      // Simulação de chamada para API de autenticação
      await new Promise((resolve) => setTimeout(resolve, 1500));

      // Validação simplificada para demonstração
      if (email === "admin@centrosangue.com" && password === "123456") {
        const authenticatedUser: User = {
          id: "1",
          name: "Dr. Maria Santos",
          email: email,
          role: "ADMIN",
        };

        setUser(authenticatedUser);

        // Armazenamento da sessão baseado na opção "lembrar de mim"
        if (rememberMe) {
          localStorage.setItem("authToken", "mock-jwt-token");
          localStorage.setItem("user", JSON.stringify(authenticatedUser));
        } else {
          sessionStorage.setItem("authToken", "mock-jwt-token");
          sessionStorage.setItem("user", JSON.stringify(authenticatedUser));
        }

        return true;
      } else {
        throw new Error("Credenciais inválidas");
      }
    } catch (error) {
      console.error("Erro na autenticação:", error);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("authToken");
    localStorage.removeItem("user");
    sessionStorage.removeItem("authToken");
    sessionStorage.removeItem("user");
  };

  const checkStoredAuth = () => {
    const token =
      localStorage.getItem("authToken") || sessionStorage.getItem("authToken");
    const storedUser =
      localStorage.getItem("user") || sessionStorage.getItem("user");

    if (token && storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setUser(parsedUser);
      } catch (error) {
        console.error("Erro ao recuperar dados do usuário:", error);
        logout();
      }
    }
  };

  // Verificar autenticação armazenada na inicialização
  React.useEffect(() => {
    checkStoredAuth();
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
