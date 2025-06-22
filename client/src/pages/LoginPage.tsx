import React, { useState } from "react";
import { Heart, Mail, Lock, Eye, EyeOff, Loader, UserCheck } from "lucide-react";
import { useAuthStore } from "../store/useAuthStore";
import { useNavigate } from "react-router-dom";
import { mockUsers } from "../mocks/authMock";

interface LoginFormData {
  email: string;
  password: string;
  rememberMe: boolean;
}

const LoginScreen: React.FC = () => {
  const [formData, setFormData] = useState<LoginFormData>({
    email: "",
    password: "",
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<Partial<LoginFormData>>({});
  
  const { login, isLoggingIn, error: authError } = useAuthStore();
  const navigate = useNavigate();
  
  // Verificar se está em modo mock
  const useMockAuth = import.meta.env.VITE_USE_MOCK_AUTH === 'true';
  
  // Função para preencher credenciais de teste
  const fillTestCredentials = (email: string, password: string) => {
    setFormData({
      email,
      password,
      rememberMe: false
    });
    setErrors({});
  };

  const validateForm = (): boolean => {
    const newErrors: Partial<LoginFormData> = {};

    if (!formData.email.trim()) {
      newErrors.email = "Email é obrigatório";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "Email inválido";
    }

    if (!formData.password.trim()) {
      newErrors.password = "Senha é obrigatória";
    } else if (formData.password.length < 6) {
      newErrors.password = "Senha deve ter pelo menos 6 caracteres";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      await login({
        email: formData.email,
        password: formData.password
      });
      
      // Login bem-sucedido, redirecionar para a página principal
      navigate("/");
    } catch (error) {
      console.error("Erro no login:", error);
    }
  };

  const handleInputChange = (
    field: keyof LoginFormData,
    value: string | boolean
  ) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    // Limpa o erro do campo quando o usuário começar a digitar
    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: undefined,
      }));
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <div className="flex justify-center">
            <div className="bg-white p-3 rounded-full shadow-sm">
              <Heart className="w-12 h-12 text-red-500" fill="currentColor" />
            </div>
          </div>
          <h1 className="mt-6 text-3xl font-semibold text-gray-900">Rubia</h1>
          <p className="mt-2 text-sm text-gray-600">
            Sistema de Comunicação com Doadores
          </p>
        </div>

        {/* Credenciais de teste em modo mock */}
        {useMockAuth && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
            <div className="flex items-center gap-2 mb-3">
              <UserCheck className="w-5 h-5 text-blue-600" />
              <h3 className="text-sm font-semibold text-blue-800">Modo Desenvolvimento - Credenciais de Teste</h3>
            </div>
            <div className="space-y-2">
              {Object.entries(mockUsers).map(([email, data]) => (
                <button
                  key={email}
                  onClick={() => fillTestCredentials(email, data.password)}
                  className="w-full text-left p-2 rounded border border-blue-200 hover:bg-blue-100 transition-colors"
                >
                  <div className="text-xs text-blue-700">
                    <div className="font-medium">{data.user.name} ({data.user.role})</div>
                    <div className="text-blue-600">{email} • {data.password}</div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="bg-white py-8 px-6 shadow-sm rounded-lg border border-gray-200">
          <div className="space-y-6">
            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  value={formData.email}
                  onChange={(e) => handleInputChange("email", e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleSubmit(e)}
                  className={`block w-full pl-10 pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.email
                      ? "border-red-300 bg-red-50"
                      : "border-gray-300 hover:border-gray-400"
                  }`}
                  placeholder="seuemail@exemplo.com"
                />
              </div>
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">{errors.email}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Senha
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  value={formData.password}
                  onChange={(e) =>
                    handleInputChange("password", e.target.value)
                  }
                  onKeyPress={(e) => e.key === "Enter" && handleSubmit(e)}
                  className={`block w-full pl-10 pr-10 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.password
                      ? "border-red-300 bg-red-50"
                      : "border-gray-300 hover:border-gray-400"
                  }`}
                  placeholder="Sua senha"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5 text-gray-400 hover:text-gray-600" />
                  ) : (
                    <Eye className="h-5 w-5 text-gray-400 hover:text-gray-600" />
                  )}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-sm text-red-600">{errors.password}</p>
              )}
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  checked={formData.rememberMe}
                  onChange={(e) =>
                    handleInputChange("rememberMe", e.target.checked)
                  }
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label
                  htmlFor="remember-me"
                  className="ml-2 block text-sm text-gray-700"
                >
                  Lembrar de mim
                </label>
              </div>

              <div className="text-sm">
                <button
                  type="button"
                  className="font-medium text-blue-600 hover:text-blue-500 transition-colors"
                >
                  Esqueci minha senha
                </button>
              </div>
            </div>

            <div>
              <button
                onClick={handleSubmit}
                disabled={isLoggingIn}
                className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-lg text-white bg-blue-500 hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-blue-300 disabled:cursor-not-allowed transition-colors"
              >
                {isLoggingIn ? (
                  <div className="flex items-center">
                    <Loader className="w-4 h-4 mr-2 animate-spin" />
                    Entrando...
                  </div>
                ) : (
                  "Entrar"
                )}
              </button>
            </div>
            
            {authError && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-600">{authError}</p>
              </div>
            )}
          </div>
        </div>

        <div className="text-center">
          <p className="text-xs text-gray-400 mt-1">
            © 2025 Rubia. Todos os direitos reservados.
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginScreen;
