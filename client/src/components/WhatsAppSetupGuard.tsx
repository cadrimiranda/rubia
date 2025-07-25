import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { whatsappSetupApi } from '../api/services/whatsappSetupApi';
import type { WhatsAppSetupStatus } from '../types';

interface WhatsAppSetupGuardProps {
  children: React.ReactNode;
}

export const WhatsAppSetupGuard: React.FC<WhatsAppSetupGuardProps> = ({ children }) => {
  const [setupStatus, setSetupStatus] = useState<WhatsAppSetupStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadingMessage, setLoadingMessage] = useState('Verificando configuração WhatsApp...');

  useEffect(() => {
    checkSetupStatus();
  }, []);

  const checkSetupStatus = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const status = await whatsappSetupApi.getSetupStatus();
      
      // Forçar verificação de status para instâncias que podem estar obsoletas
      let needsRecheck = false;
      for (const instance of status.instances) {
        if (instance.status === 'CONNECTED' && instance.lastStatusCheck) {
          const lastCheck = new Date(instance.lastStatusCheck);
          const now = new Date();
          const minutesSinceCheck = (now.getTime() - lastCheck.getTime()) / (1000 * 60);
          
          // Se não foi verificado nas últimas 2 horas, forçar verificação
          if (minutesSinceCheck > 120) {
            setLoadingMessage(`Verificando status da instância ${instance.phoneNumber}...`);
            console.log(`🔄 [Guard] Forcing status check for potentially stale instance: ${instance.phoneNumber}`);
            try {
              await whatsappSetupApi.forceStatusCheck(instance.id);
              needsRecheck = true;
            } catch (error) {
              console.error('Error forcing status check in guard:', error);
            }
          }
        }
      }
      
      if (needsRecheck) {
        setLoadingMessage('Atualizando status das instâncias...');
      }
      
      // Se forçamos alguma verificação, recarregar o status
      const finalStatus = needsRecheck ? await whatsappSetupApi.getSetupStatus() : status;
      
      // Verificar se tem instâncias desconectadas
      const disconnectedInstances = finalStatus.instances.filter(i => i.status === 'DISCONNECTED');
      if (disconnectedInstances.length > 0) {
        console.log(`⚠️ [Guard] Found ${disconnectedInstances.length} disconnected instance(s), redirecting to setup`);
        setSetupStatus({ 
          ...finalStatus,
          requiresSetup: true  // Força redirecionamento para setup
        });
      } else {
        setSetupStatus(finalStatus);
      }
      
    } catch (error: unknown) {
      console.error('Error checking WhatsApp setup status:', error);
      
      const errorMessage = error instanceof Error ? error.message : 'Erro ao verificar configuração WhatsApp';
      const errorStatus = error && typeof error === 'object' && 'status' in error ? error.status : null;
      
      // If error is 428 Precondition Required, redirect to setup
      if (errorStatus === 428) {
        setSetupStatus({ 
          requiresSetup: true, 
          hasConfiguredInstance: false,
          hasConnectedInstance: false,
          totalInstances: 0,
          maxAllowedInstances: 1,
          instances: []
        });
      } else {
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  // Show loading spinner
  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Spin size="large" tip={loadingMessage} />
      </div>
    );
  }

  // Show error state
  if (error) {
    console.error('WhatsApp setup guard error:', error);
    // Allow access on error to prevent blocking the app
    return <>{children}</>;
  }

  // Redirect to setup if required
  if (setupStatus?.requiresSetup) {
    return <Navigate to="/whatsapp-setup" replace />;
  }

  // Allow access if setup is complete
  return <>{children}</>;
};