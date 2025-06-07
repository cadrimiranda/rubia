import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Button, Alert } from 'antd'
import { RefreshCw, AlertTriangle } from 'lucide-react'

interface Props {
  children: ReactNode
  componentName?: string
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

/**
 * Error Boundary mais leve para componentes específicos
 * Não quebra toda a aplicação, apenas o componente afetado
 */
class ComponentErrorBoundary extends Component<Props, State> {
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
    const componentName = this.props.componentName || 'Component'
    console.error(`Error in ${componentName}:`, error, errorInfo)
    
    this.setState({ error })
  }

  private handleRetry = () => {
    this.setState({ hasError: false, error: undefined })
  }

  public render() {
    if (this.state.hasError) {
      // Usar fallback customizado se fornecido
      if (this.props.fallback) {
        return this.props.fallback
      }

      const componentName = this.props.componentName || 'Este componente'

      // Fallback padrão mais simples
      return (
        <div className="p-4 m-2">
          <Alert
            message={`Erro em ${componentName}`}
            description="Ocorreu um erro inesperado neste componente."
            type="error"
            showIcon
            icon={<AlertTriangle size={16} />}
            action={
              <Button
                size="small"
                type="text"
                icon={<RefreshCw size={14} />}
                onClick={this.handleRetry}
                className="text-red-600 hover:text-red-700"
              >
                Tentar novamente
              </Button>
            }
            className="border-red-200 bg-red-50"
          />
          
          {/* Mostrar erro em desenvolvimento */}
          {import.meta.env.DEV && this.state.error && (
            <details className="mt-3 p-3 bg-red-100 rounded border border-red-200">
              <summary className="text-xs text-red-800 cursor-pointer">
                Detalhes do erro
              </summary>
              <pre className="text-xs text-red-700 mt-2 whitespace-pre-wrap">
                {this.state.error.message}
              </pre>
            </details>
          )}
        </div>
      )
    }

    return this.props.children
  }
}

export default ComponentErrorBoundary