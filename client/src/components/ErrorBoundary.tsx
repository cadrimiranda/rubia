import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Button, Result } from 'antd'
import { RefreshCw, Home, AlertTriangle } from 'lucide-react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
  errorInfo?: ErrorInfo
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  public static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      error
    }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
    
    this.setState({
      error,
      errorInfo
    })

    // Enviar erro para serviço de monitoramento (ex: Sentry)
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('app:error', {
        detail: {
          error: error.message,
          stack: error.stack,
          componentStack: errorInfo.componentStack
        }
      }))
    }
  }

  private handleReload = () => {
    window.location.reload()
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined })
  }

  private handleGoHome = () => {
    // Reset da aplicação
    localStorage.clear()
    window.location.href = '/'
  }

  public render() {
    if (this.state.hasError) {
      // Usar fallback customizado se fornecido
      if (this.props.fallback) {
        return this.props.fallback
      }

      // Fallback padrão
      return (
        <div className="h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100 p-6">
          <div className="max-w-lg w-full">
            <Result
              icon={<AlertTriangle size={64} className="text-red-500 mx-auto" />}
              title="Ops! Algo deu errado"
              subTitle="Ocorreu um erro inesperado na aplicação. Tente recarregar a página ou entre em contato com o suporte."
              extra={[
                <div key="actions" className="space-y-3">
                  <div className="flex flex-col sm:flex-row gap-3 justify-center">
                    <Button
                      type="primary"
                      icon={<RefreshCw size={16} />}
                      onClick={this.handleReload}
                      size="large"
                      className="bg-rose-500 hover:bg-rose-600 border-rose-500"
                    >
                      Recarregar Página
                    </Button>
                    
                    <Button
                      icon={<Home size={16} />}
                      onClick={this.handleGoHome}
                      size="large"
                    >
                      Voltar ao Início
                    </Button>
                  </div>
                  
                  <Button
                    type="link"
                    onClick={this.handleReset}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    Tentar Novamente
                  </Button>
                </div>
              ]}
            />
            
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
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary