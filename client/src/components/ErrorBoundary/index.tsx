import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";
import { RefreshCw, Home, AlertTriangle } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  public static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      error,
    };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("ErrorBoundary caught an error:", error, errorInfo);

    this.setState({
      error,
      errorInfo,
    });

    // Enviar erro para serviço de monitoramento (ex: Sentry)
    if (typeof window !== "undefined") {
      window.dispatchEvent(
        new CustomEvent("app:error", {
          detail: {
            error: error.message,
            stack: error.stack,
            componentStack: errorInfo.componentStack,
          },
        })
      );
    }
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleReset = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  private handleGoHome = () => {
    // Reset da aplicação
    localStorage.clear();
    window.location.href = "/";
  };

  public render() {
    if (this.state.hasError) {
      // Usar fallback customizado se fornecido
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Fallback padrão
      return (
        <div className="h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100 p-6">
          <div className="max-w-lg w-full">
            <div className="text-center space-y-6">
              <div className="flex justify-center">
                <AlertTriangle size={64} className="text-red-500" />
              </div>

              <div className="space-y-2">
                <h1 className="text-2xl font-semibold text-gray-900">
                  Ops! Algo deu errado
                </h1>
                <p className="text-gray-600">
                  Ocorreu um erro inesperado na aplicação. Tente recarregar a
                  página ou entre em contato com o suporte.
                </p>
              </div>

              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row gap-3 justify-center">
                  <button
                    onClick={this.handleReload}
                    className="inline-flex items-center gap-2 px-6 py-3 bg-rose-500 hover:bg-rose-600 text-white font-medium rounded-lg transition-colors"
                  >
                    <RefreshCw size={16} />
                    Recarregar Página
                  </button>

                  <button
                    onClick={this.handleGoHome}
                    className="inline-flex items-center gap-2 px-6 py-3 border border-gray-300 bg-white hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors"
                  >
                    <Home size={16} />
                    Voltar ao Início
                  </button>
                </div>

                <button
                  onClick={this.handleReset}
                  className="text-gray-500 hover:text-gray-700 font-medium transition-colors"
                >
                  Tentar Novamente
                </button>
              </div>
            </div>

            {/* Detalhes do erro em desenvolvimento */}
            {import.meta.env.DEV && this.state.error && (
              <details className="mt-6 p-4 bg-red-50 rounded-lg border border-red-200">
                <summary className="cursor-pointer text-sm font-medium text-red-800 mb-2">
                  Detalhes do Erro (Desenvolvimento)
                </summary>
                <div className="text-xs text-red-700 space-y-2">
                  <div>
                    <strong>Erro:</strong> {this.state.error.message}
                  </div>
                  {this.state.error.stack && (
                    <div>
                      <strong>Stack:</strong>
                      <pre className="whitespace-pre-wrap text-xs mt-1 p-2 bg-red-100 rounded">
                        {this.state.error.stack}
                      </pre>
                    </div>
                  )}
                  {this.state.errorInfo?.componentStack && (
                    <div>
                      <strong>Component Stack:</strong>
                      <pre className="whitespace-pre-wrap text-xs mt-1 p-2 bg-red-100 rounded">
                        {this.state.errorInfo.componentStack}
                      </pre>
                    </div>
                  )}
                </div>
              </details>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
