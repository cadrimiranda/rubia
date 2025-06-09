import { useState, useEffect } from 'react'
import { Modal, Input, Button, List, Avatar, Typography, Empty, Spin, message } from 'antd'
import { Search, Phone, User, Plus } from 'lucide-react'
import { customerApi } from '../../api/services/customerApi'
import { conversationApi } from '../../api/services/conversationApi'
import { useChatStore } from '../../store/useChatStore'
import type { CustomerDTO } from '../../api/types'

const { Text } = Typography

interface NewConversationModalProps {
  open: boolean
  onClose: () => void
}

export const NewConversationModal: React.FC<NewConversationModalProps> = ({
  open,
  onClose
}) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [customers, setCustomers] = useState<CustomerDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [creating, setCreating] = useState(false)
  const { loadConversations, currentStatus, setActiveChat } = useChatStore()

  // Buscar customers quando modal abrir ou query mudar
  useEffect(() => {
    if (open) {
      searchCustomers()
    }
  }, [open, searchQuery])

  const searchCustomers = async () => {
    try {
      setLoading(true)
      
      if (searchQuery.trim()) {
        // Buscar por telefone ou nome
        const results = await customerApi.search(searchQuery.trim())
        setCustomers(results || [])
      } else {
        // Carregar customers recentes
        const results = await customerApi.getRecent(20)
        setCustomers(results || [])
      }
    } catch (error) {
      console.error('Erro ao buscar customers:', error)
      message.error('Erro ao buscar contatos')
      setCustomers([]) // Garantir que customers seja sempre um array
    } finally {
      setLoading(false)
    }
  }

  const createConversation = async (customer: CustomerDTO) => {
    try {
      setCreating(true)
      
      // Criar nova conversa
      const conversation = await conversationApi.create({
        customerId: customer.id,
        channel: 'WEB'
      })

      // Recarregar lista de conversas
      await loadConversations(currentStatus, 0)
      
      // Abrir a conversa criada
      // setActiveChat(conversationAdapter.toChat(conversation))
      
      message.success(`Conversa iniciada com ${customer.name || customer.phone}`)
      onClose()
      
    } catch (error) {
      console.error('Erro ao criar conversa:', error)
      message.error('Erro ao iniciar conversa')
    } finally {
      setCreating(false)
    }
  }

  const createNewCustomer = async () => {
    const phone = searchQuery.trim()
    
    if (!phone) {
      message.warning('Digite um número de telefone')
      return
    }

    try {
      setCreating(true)
      
      // Criar novo customer
      const customer = await customerApi.create({
        phone,
        name: phone // Usar telefone como nome inicial
      })
      
      // Criar conversa com o novo customer
      await createConversation(customer)
      
    } catch (error) {
      console.error('Erro ao criar customer:', error)
      message.error('Erro ao criar contato')
    } finally {
      setCreating(false)
    }
  }

  const isPhoneNumber = (query: string) => {
    return /^\+?[\d\s\-\(\)]+$/.test(query.trim())
  }

  const handleClose = () => {
    setSearchQuery('')
    setCustomers([])
    onClose()
  }

  // Garantir que customers seja sempre um array
  const safeCustomers = customers || []

  return (
    <Modal
      title="Nova Conversa"
      open={open}
      onCancel={handleClose}
      footer={null}
      width={480}
      destroyOnClose
    >
      <div className="space-y-4">
        {/* Search Input */}
        <Input
          placeholder="Buscar por nome ou telefone..."
          prefix={<Search size={16} className="text-gray-400" />}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          size="large"
          autoFocus
        />

        {/* Create new customer option */}
        {searchQuery.trim() && isPhoneNumber(searchQuery) && safeCustomers.length === 0 && !loading && (
          <div className="border rounded-lg p-3 bg-blue-50">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                  <Phone size={16} className="text-blue-600" />
                </div>
                <div>
                  <Text strong>Novo contato</Text>
                  <div className="text-sm text-gray-500">{searchQuery}</div>
                </div>
              </div>
              <Button
                type="primary"
                icon={<Plus size={16} />}
                onClick={createNewCustomer}
                loading={creating}
                size="small"
              >
                Criar
              </Button>
            </div>
          </div>
        )}

        {/* Results */}
        <div className="max-h-96 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Spin tip="Buscando contatos..." />
            </div>
          ) : safeCustomers.length === 0 ? (
            searchQuery.trim() ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Nenhum contato encontrado"
              />
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Digite para buscar contatos"
              />
            )
          ) : (
            <List
              dataSource={safeCustomers}
              renderItem={(customer) => (
                <List.Item
                  className="hover:bg-gray-50 cursor-pointer rounded-lg px-3 transition-colors"
                  onClick={() => createConversation(customer)}
                >
                  <List.Item.Meta
                    avatar={
                      <Avatar
                        src={customer.profileUrl}
                        icon={<User size={16} />}
                        className="flex-shrink-0"
                      />
                    }
                    title={
                      <div className="flex items-center justify-between">
                        <Text strong>{customer.name || customer.phone}</Text>
                        {creating && <Spin size="small" />}
                      </div>
                    }
                    description={
                      <div className="flex items-center gap-2 text-sm text-gray-500">
                        <Phone size={12} />
                        {customer.phone}
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </div>
      </div>
    </Modal>
  )
}

export default NewConversationModal