import React from "react";
import { Modal, Button } from "antd";
import { AlertTriangle, Archive, UserX } from "lucide-react";

interface ConfirmationModalProps {
  show: boolean;
  title: string;
  message: string;
  type: 'warning' | 'danger' | 'info';
  onConfirm: () => void;
  onCancel: () => void;
  confirmText?: string;
  cancelText?: string;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  show,
  title,
  message,
  type,
  onConfirm,
  onCancel,
  confirmText = "Confirmar",
  cancelText = "Cancelar",
}) => {
  const getIcon = () => {
    switch (type) {
      case 'danger':
        return <UserX className="w-6 h-6 text-red-500" />;
      case 'warning':
        return <Archive className="w-6 h-6 text-yellow-500" />;
      default:
        return <AlertTriangle className="w-6 h-6 text-blue-500" />;
    }
  };

  const getButtonType = () => {
    switch (type) {
      case 'danger':
        return 'primary';
      case 'warning':
        return 'default';
      default:
        return 'primary';
    }
  };

  const getButtonStyle = () => {
    switch (type) {
      case 'danger':
        return { backgroundColor: '#dc2626', borderColor: '#dc2626' };
      case 'warning':
        return { backgroundColor: '#d97706', borderColor: '#d97706', color: 'white' };
      default:
        return {};
    }
  };

  return (
    <Modal
      title={null}
      open={show}
      onCancel={onCancel}
      footer={null}
      width={400}
      centered
    >
      <div className="text-center py-4">
        <div className="flex justify-center mb-4">
          {getIcon()}
        </div>
        
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          {title}
        </h3>
        
        <p className="text-gray-600 mb-6">
          {message}
        </p>
        
        <div className="flex gap-3 justify-center">
          <Button
            size="large"
            onClick={onCancel}
          >
            {cancelText}
          </Button>
          <Button
            type={getButtonType()}
            size="large"
            onClick={onConfirm}
            style={getButtonStyle()}
          >
            {confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
};