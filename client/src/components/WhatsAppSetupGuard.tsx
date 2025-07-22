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

  useEffect(() => {
    checkSetupStatus();
  }, []);

  const checkSetupStatus = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const status = await whatsappSetupApi.getSetupStatus();
      setSetupStatus(status);
      
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
        <Spin size="large" tip="Verificando configuração WhatsApp..." />
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