import { useState } from 'react'
import { Modal, Form, Input, Button, Alert, Divider } from 'antd'
import { Mail, Lock, Eye, EyeOff, MessageCircle } from 'lucide-react'
import { useAuthStore } from '../store/useAuthStore'

const LoginModal = () => {
  const [form] = Form.useForm()
  const [showPassword, setShowPassword] = useState(false)
  
  const { 
    showLoginModal, 
    hideLogin, 
    login, 
    isLoggingIn, 
    error,
    clearError 
  } = useAuthStore()

  const handleLogin = async (values: { email: string; password: string }) => {
    try {
      await login(values)
      form.resetFields()
    } catch (error) {
      // Erro já tratado no store
      console.error('Login failed:', error)
    }
  }

  const handleCancel = () => {
    clearError()
    form.resetFields()
    hideLogin()
  }

  return (
    <Modal
      open={showLoginModal}
      onCancel={handleCancel}
      footer={null}
      width={480}
      centered
      maskClosable={false}
      closable={!isLoggingIn}
      className="login-modal"
    >
      <div className="px-6 py-4">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <MessageCircle size={32} className="text-rose-500" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">
            Bem-vindo ao Rubia
          </h1>
          <p className="text-gray-600">
            Entre com sua conta para acessar o chat corporativo
          </p>
        </div>

        {/* Error Alert */}
        {error && (
          <Alert
            message="Erro no Login"
            description={error}
            type="error"
            showIcon
            className="mb-6"
            onClose={clearError}
            closable
          />
        )}

        {/* Login Form */}
        <Form
          form={form}
          layout="vertical"
          onFinish={handleLogin}
          disabled={isLoggingIn}
        >
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Por favor, insira seu email' },
              { type: 'email', message: 'Por favor, insira um email válido' }
            ]}
          >
            <Input
              prefix={<Mail size={18} className="text-gray-400" />}
              placeholder="seu.email@empresa.com"
              size="large"
              className="h-12"
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="Senha"
            rules={[
              { required: true, message: 'Por favor, insira sua senha' },
              { min: 6, message: 'A senha deve ter pelo menos 6 caracteres' }
            ]}
          >
            <Input
              prefix={<Lock size={18} className="text-gray-400" />}
              suffix={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-gray-400 hover:text-gray-600 focus:outline-none"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              size="large"
              className="h-12"
            />
          </Form.Item>

          <Form.Item className="mb-6">
            <Button
              type="primary"
              htmlType="submit"
              loading={isLoggingIn}
              size="large"
              className="w-full h-12 bg-rose-500 hover:bg-rose-600 border-rose-500 hover:border-rose-600 font-medium"
            >
              {isLoggingIn ? 'Entrando...' : 'Entrar'}
            </Button>
          </Form.Item>
        </Form>

        <Divider className="my-6">
          <span className="text-gray-400 text-sm">Informações do Sistema</span>
        </Divider>

        {/* System Info */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-2">
            Níveis de Acesso:
          </h3>
          <ul className="text-xs text-gray-600 space-y-1">
            <li><strong>AGENT:</strong> Atendimento de conversas básicas</li>
            <li><strong>SUPERVISOR:</strong> Gestão de equipe + AGENT</li>
            <li><strong>ADMIN:</strong> Configurações gerais + SUPERVISOR</li>
          </ul>
        </div>

        {/* Footer */}
        <div className="text-center mt-6">
          <p className="text-xs text-gray-500">
            Rubia Chat v1.0 - Sistema de Atendimento Corporativo
          </p>
        </div>
      </div>
    </Modal>
  )
}

export default LoginModal