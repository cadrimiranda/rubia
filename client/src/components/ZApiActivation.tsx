import React, { useState, useEffect } from 'react';
import { Card, Button, Radio, Input, Alert, Space, Typography, Divider } from 'antd';
import { QrcodeOutlined, PhoneOutlined, ReloadOutlined, DisconnectOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

interface ZApiStatus {
  connected: boolean;
  session: string;
  smartphoneConnected: boolean;
  needsQrCode: boolean;
  error?: string;
}

interface QrCodeResult {
  success: boolean;
  data: string;
  type: string;
  error?: string;
}

interface PhoneCodeResult {
  success: boolean;
  code: string;
  phoneNumber: string;
  error?: string;
}

const ZApiActivation: React.FC = () => {
  const [status, setStatus] = useState<ZApiStatus | null>(null);
  const [qrCodeImage, setQrCodeImage] = useState<string>('');
  const [phoneCode, setPhoneCode] = useState<PhoneCodeResult | null>(null);
  const [phoneNumber, setPhoneNumber] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [activationMethod, setActivationMethod] = useState<'qr' | 'phone'>('qr');

  const API_BASE = '/api/zapi/activation';

  const checkStatus = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/status`);
      const data: ZApiStatus = await response.json();
      setStatus(data);
    } catch (error) {
      console.error('Error checking status:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadQrCode = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/qr-code/image`);
      const data: QrCodeResult = await response.json();
      
      if (data.success) {
        setQrCodeImage(data.data);
      } else {
        console.error('Error loading QR code:', data.error);
      }
    } catch (error) {
      console.error('Error loading QR code:', error);
    } finally {
      setLoading(false);
    }
  };

  const generatePhoneCode = async () => {
    if (!phoneNumber) return;
    
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/phone-code/${phoneNumber}`);
      const data: PhoneCodeResult = await response.json();
      setPhoneCode(data);
    } catch (error) {
      console.error('Error generating phone code:', error);
    } finally {
      setLoading(false);
    }
  };

  const restartInstance = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/restart`, { method: 'POST' });
      const data = await response.json();
      
      if (data.success) {
        await checkStatus();
      }
    } catch (error) {
      console.error('Error restarting instance:', error);
    } finally {
      setLoading(false);
    }
  };

  const disconnectInstance = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE}/disconnect`, { method: 'POST' });
      const data = await response.json();
      
      if (data.success) {
        await checkStatus();
        setQrCodeImage('');
        setPhoneCode(null);
      }
    } catch (error) {
      console.error('Error disconnecting instance:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkStatus();
  }, []);

  useEffect(() => {
    let interval: number;
    
    if (status?.needsQrCode && activationMethod === 'qr') {
      loadQrCode();
      interval = setInterval(checkStatus, 5000);
    }
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [status?.needsQrCode, activationMethod]);

  return (
    <Card className="max-w-lg mx-auto">
      <Title level={3} className="text-center mb-6">
        Ativação Z-API
      </Title>

      <Space direction="vertical" className="w-full" size="large">
        {/* Status Section */}
        <div>
          <Space className="w-full justify-between mb-4">
            <Text strong>Status:</Text>
            <span className={`px-3 py-1 rounded text-sm font-semibold ${
              status?.connected 
                ? 'bg-green-100 text-green-800' 
                : 'bg-red-100 text-red-800'
            }`}>
              {status?.connected ? 'Conectado' : 'Desconectado'}
            </span>
          </Space>
          
          <Button
            onClick={checkStatus}
            loading={loading}
            className="w-full"
            type="primary"
            ghost
          >
            Verificar Status
          </Button>
        </div>

        {/* Activation Section */}
        {status?.needsQrCode && (
          <div>
            <Radio.Group
              value={activationMethod}
              onChange={(e) => setActivationMethod(e.target.value)}
              className="w-full mb-4"
              buttonStyle="solid"
            >
              <Radio.Button value="qr" className="flex-1 text-center">
                <QrcodeOutlined /> QR Code
              </Radio.Button>
              <Radio.Button value="phone" className="flex-1 text-center">
                <PhoneOutlined /> Código
              </Radio.Button>
            </Radio.Group>

            {activationMethod === 'qr' && (
              <div className="text-center">
                {qrCodeImage ? (
                  <Space direction="vertical" className="w-full">
                    <img 
                      src={`data:image/png;base64,${qrCodeImage}`}
                      alt="QR Code para ativação"
                      className="mx-auto border rounded max-w-full"
                      style={{ maxWidth: '300px' }}
                    />
                    <Text type="secondary" className="block mb-4">
                      Escaneie o QR code com o WhatsApp
                    </Text>
                    <Button
                      onClick={loadQrCode}
                      loading={loading}
                      type="primary"
                      icon={<ReloadOutlined />}
                    >
                      Gerar Novo QR
                    </Button>
                  </Space>
                ) : (
                  <Button
                    onClick={loadQrCode}
                    loading={loading}
                    type="primary"
                    icon={<QrcodeOutlined />}
                    size="large"
                  >
                    Carregar QR Code
                  </Button>
                )}
              </div>
            )}

            {activationMethod === 'phone' && (
              <Space direction="vertical" className="w-full">
                <Input
                  placeholder="Número do telefone (5511999999999)"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  prefix={<PhoneOutlined />}
                />
                
                <Button
                  onClick={generatePhoneCode}
                  disabled={loading || !phoneNumber}
                  loading={loading}
                  type="primary"
                  className="w-full"
                >
                  Gerar Código
                </Button>
                
                {phoneCode?.success && (
                  <Alert
                    type="success"
                    showIcon
                    message="Código gerado:"
                    description={
                      <div className="text-center">
                        <Text code className="text-2xl font-bold block mb-2">
                          {phoneCode.code}
                        </Text>
                        <Text type="secondary" className="text-xs">
                          Digite este código no WhatsApp em "Conectar com número de telefone"
                        </Text>
                      </div>
                    }
                  />
                )}
              </Space>
            )}
          </div>
        )}

        {/* Connected Status */}
        {status?.connected && (
          <Alert
            type="success"
            showIcon
            message="WhatsApp Conectado"
            description={
              <Text type="secondary">
                Smartphone: {status.smartphoneConnected ? 'Conectado' : 'Desconectado'}
              </Text>
            }
          />
        )}

        <Divider />

        {/* Action Buttons */}
        <Space className="w-full">
          <Button
            onClick={restartInstance}
            loading={loading}
            icon={<ReloadOutlined />}
            className="flex-1"
          >
            Reiniciar
          </Button>
          
          <Button
            onClick={disconnectInstance}
            loading={loading}
            danger
            icon={<DisconnectOutlined />}
            className="flex-1"
          >
            Desconectar
          </Button>
        </Space>
      </Space>
    </Card>
  );
};

export default ZApiActivation;