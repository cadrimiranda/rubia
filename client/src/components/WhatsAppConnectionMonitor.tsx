import React, { useEffect, useState } from 'react';
import { Modal, Alert, Button, Spin } from 'antd';
import { QrcodeOutlined } from '@ant-design/icons';
import { whatsappSetupApi } from '../api/services/whatsappSetupApi';
import ZApiActivation from './ZApiActivation';
import type { WhatsAppInstance } from '../types';

interface WhatsAppConnectionMonitorProps {
  checkInterval?: number; // em milissegundos, padrão 5 minutos
}

export const WhatsAppConnectionMonitor: React.FC<WhatsAppConnectionMonitorProps> = ({ 
  checkInterval = 300000 // 5 minutos
}) => {
  const [disconnectedInstances, setDisconnectedInstances] = useState<WhatsAppInstance[]>([]);
  const [showReconnectModal, setShowReconnectModal] = useState(false);
  const [currentInstance, setCurrentInstance] = useState<WhatsAppInstance | null>(null);
  const [isReconnecting, setIsReconnecting] = useState(false);

  useEffect(() => {
    // Verificação inicial
    checkInstancesStatus();
    
    // Configurar verificação periódica
    const interval = setInterval(checkInstancesStatus, checkInterval);
    
    return () => clearInterval(interval);
  }, [checkInterval]);

  const checkInstancesStatus = async () => {
    try {
      const status = await whatsappSetupApi.getSetupStatus();
      const disconnected = status.instances.filter(i => i.status === 'DISCONNECTED');
      
      if (disconnected.length > 0 && disconnectedInstances.length === 0) {
        // Nova desconexão detectada
        console.log('🚨 [Monitor] Detected disconnected instances:', disconnected.map(i => i.phoneNumber));
        setDisconnectedInstances(disconnected);
        setCurrentInstance(disconnected[0]); // Mostrar a primeira
        setShowReconnectModal(true);
      } else if (disconnected.length === 0 && disconnectedInstances.length > 0) {
        // Instâncias reconectadas
        console.log('✅ [Monitor] All instances reconnected');
        setDisconnectedInstances([]);
        setShowReconnectModal(false);
        setCurrentInstance(null);
      }
    } catch (error) {
      console.error('Error checking instances status in monitor:', error);
    }
  };

  const handleReconnect = async () => {
    if (!currentInstance) return;

    try {
      setIsReconnecting(true);
      
      const result = await whatsappSetupApi.reconnectInstance(currentInstance.id);
      
      if (result.success) {
        if (result.status === 'CONNECTED') {
          // Já conectado
          setShowReconnectModal(false);
          setDisconnectedInstances([]);
          setCurrentInstance(null);
        }
        // Se status for AWAITING_QR_SCAN, o modal permanece aberto mostrando o QR
      }
      
    } catch (error) {
      console.error('Error reconnecting instance:', error);
    } finally {
      setIsReconnecting(false);
    }
  };

  const handleDismiss = () => {
    setShowReconnectModal(false);
    // Não limpar disconnectedInstances para que o modal apareça novamente na próxima verificação
  };

  const formatPhoneNumber = (phone: string) => {
    return whatsappSetupApi.formatPhoneNumber(phone);
  };

  if (!showReconnectModal || !currentInstance) {
    return null;
  }

  return (
    <Modal
      title="Instância WhatsApp Desconectada"
      open={showReconnectModal}
      onCancel={handleDismiss}
      footer={[
        <Button key="dismiss" onClick={handleDismiss}>
          Dispensar
        </Button>,
        <Button 
          key="reconnect" 
          type="primary" 
          icon={<QrcodeOutlined />}
          onClick={handleReconnect}
          loading={isReconnecting}
        >
          Reconectar Agora
        </Button>
      ]}
      width={800}
      maskClosable={false}
    >
      <div className="space-y-4">
        <Alert
          type="warning"
          message="Conexão WhatsApp Perdida"
          description={`A instância ${formatPhoneNumber(currentInstance.phoneNumber)} foi desconectada. Para continuar enviando e recebendo mensagens, é necessário reconectar escaneando o QR Code.`}
          showIcon
        />

        {disconnectedInstances.length > 1 && (
          <Alert
            type="info"
            message={`${disconnectedInstances.length} instâncias desconectadas`}
            description="Múltiplas instâncias foram desconectadas. Você pode reconectá-las individualmente."
            showIcon
          />
        )}

        <div className="bg-gray-50 p-4 rounded-lg">
          <h4 className="font-medium mb-2">Para reconectar:</h4>
          <ol className="list-decimal list-inside space-y-1 text-sm text-gray-600">
            <li>Clique em "Reconectar Agora"</li>
            <li>Abra o WhatsApp no seu celular ({formatPhoneNumber(currentInstance.phoneNumber)})</li>
            <li>Vá em Configurações → Aparelhos conectados</li>
            <li>Escaneie o QR Code que aparecerá abaixo</li>
          </ol>
        </div>

        {isReconnecting && (
          <div className="text-center py-4">
            <Spin size="large" />
            <p className="mt-2 text-gray-600">Verificando status da conexão...</p>
          </div>
        )}

        {/* O QR Code aparecerá aqui quando necessário */}
        <div id="qr-code-container">
          <ZApiActivation />
        </div>
      </div>
    </Modal>
  );
};

export default WhatsAppConnectionMonitor;