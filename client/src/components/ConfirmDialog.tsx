import { Modal } from 'antd'
import { AlertTriangle } from 'lucide-react'

interface ConfirmDialogProps {
  open: boolean
  onConfirm: () => void
  onCancel: () => void
  title: string
  description: string
  type?: 'danger' | 'warning' | 'info'
  confirmText?: string
  cancelText?: string
  loading?: boolean
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  onConfirm,
  onCancel,
  title,
  description,
  type = 'warning',
  confirmText = 'Confirmar',
  cancelText = 'Cancelar',
  loading = false
}) => {
  const getIcon = () => {
    const iconProps = { size: 48, className: 'mx-auto mb-4' }
    
    switch (type) {
      case 'danger':
        return <AlertTriangle {...iconProps} className="text-red-500 mx-auto mb-4" />
      case 'warning':
        return <AlertTriangle {...iconProps} className="text-yellow-500 mx-auto mb-4" />
      default:
        return <AlertTriangle {...iconProps} className="text-blue-500 mx-auto mb-4" />
    }
  }


  return (
    <Modal
      open={open}
      onCancel={onCancel}
      footer={null}
      width={420}
      centered
      maskClosable={!loading}
      closable={!loading}
    >
      <div className="text-center py-6">
        {getIcon()}
        
        <h3 className="text-lg font-semibold text-gray-800 mb-3">
          {title}
        </h3>
        
        <p className="text-gray-600 mb-6 leading-relaxed">
          {description}
        </p>
        
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button
            onClick={onCancel}
            disabled={loading}
            className="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {cancelText}
          </button>
          
          <button
            onClick={onConfirm}
            disabled={loading}
            className={`px-6 py-2 rounded-lg text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors ${
              type === 'danger' 
                ? 'bg-red-500 hover:bg-red-600' 
                : type === 'warning'
                ? 'bg-yellow-500 hover:bg-yellow-600'
                : 'bg-blue-500 hover:bg-blue-600'
            }`}
          >
            {loading ? (
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <span>Processando...</span>
              </div>
            ) : (
              confirmText
            )}
          </button>
        </div>
      </div>
    </Modal>
  )
}

// Hook para usar confirmações
export const useConfirmDialog = () => {
  const showConfirm = (options: Omit<ConfirmDialogProps, 'open'>) => {
    return new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: options.title,
        content: options.description,
        icon: <AlertTriangle size={24} className="text-yellow-500" />,
        okText: options.confirmText || 'Confirmar',
        cancelText: options.cancelText || 'Cancelar',
        okType: options.type === 'danger' ? 'danger' : 'primary',
        centered: true,
        onOk: () => {
          resolve(true)
          if (options.onConfirm) options.onConfirm()
        },
        onCancel: () => {
          resolve(false)
          if (options.onCancel) options.onCancel()
        },
      })
    })
  }

  const confirmDelete = (itemName: string, onConfirm: () => void) => {
    return showConfirm({
      title: 'Confirmar Exclusão',
      description: `Tem certeza que deseja excluir "${itemName}"? Esta ação não pode ser desfeita.`,
      type: 'danger',
      confirmText: 'Excluir',
      cancelText: 'Cancelar',
      onConfirm,
      onCancel: () => {}
    })
  }

  const confirmBlock = (customerName: string, onConfirm: () => void) => {
    return showConfirm({
      title: 'Bloquear Cliente',
      description: `Tem certeza que deseja bloquear "${customerName}"? O cliente não poderá mais enviar mensagens.`,
      type: 'warning',
      confirmText: 'Bloquear',
      cancelText: 'Cancelar',
      onConfirm,
      onCancel: () => {}
    })
  }

  const confirmLogout = (onConfirm: () => void) => {
    return showConfirm({
      title: 'Sair da Conta',
      description: 'Tem certeza que deseja sair? Você precisará fazer login novamente para acessar o sistema.',
      type: 'info',
      confirmText: 'Sair',
      cancelText: 'Cancelar',
      onConfirm,
      onCancel: () => {}
    })
  }

  const confirmFinalize = (conversationWith: string, onConfirm: () => void) => {
    return showConfirm({
      title: 'Finalizar Conversa',
      description: `Tem certeza que deseja finalizar a conversa com "${conversationWith}"? A conversa será movida para "Finalizados".`,
      type: 'warning',
      confirmText: 'Finalizar',
      cancelText: 'Cancelar',
      onConfirm,
      onCancel: () => {}
    })
  }

  return {
    showConfirm,
    confirmDelete,
    confirmBlock,
    confirmLogout,
    confirmFinalize
  }
}

export default ConfirmDialog