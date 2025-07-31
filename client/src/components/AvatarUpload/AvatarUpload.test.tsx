import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { AvatarUpload } from './index';

// Mock do Ant Design message
jest.mock('antd', () => ({
  ...jest.requireActual('antd'),
  message: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

// Mock do FileReader
const mockFileReader = {
  readAsDataURL: jest.fn(),
  result: '',
  onload: null as any,
  onerror: null as any,
};

Object.defineProperty(window, 'FileReader', {
  writable: true,
  value: jest.fn(() => mockFileReader),
});

describe('AvatarUpload', () => {
  const mockOnChange = jest.fn();
  
  // Avatar base64 de teste (1x1 pixel JPEG)
  const validBase64 = 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A/9k=';

  beforeEach(() => {
    jest.clearAllMocks();
    mockFileReader.result = validBase64;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('deve renderizar sem avatar inicial', () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    // Deve mostrar avatar padrão
    expect(screen.getByRole('img')).toBeInTheDocument();
    expect(screen.getByText('Clique para fazer upload')).toBeInTheDocument();
    expect(screen.getByText('JPG, PNG, GIF')).toBeInTheDocument();
    expect(screen.getByText('Máx. 2MB')).toBeInTheDocument();
  });

  it('deve exibir avatar quando value é fornecido', () => {
    render(<AvatarUpload value={validBase64} onChange={mockOnChange} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveAttribute('src', validBase64);
    
    // Deve mostrar botão de remoção
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('deve processar upload de arquivo JPEG válido', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    // Simular arquivo JPEG válido
    const file = new File(['fake-jpeg-content'], 'avatar.jpg', { type: 'image/jpeg' });
    Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
    
    const input = screen.getByRole('button', { name: /upload/i });
    
    // Simular upload
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    // Simular FileReader onload
    if (mockFileReader.onload) {
      mockFileReader.onload();
    }
    
    await waitFor(() => {
      expect(mockOnChange).toHaveBeenCalledWith(validBase64);
      expect(message.success).toHaveBeenCalledWith('Avatar atualizado com sucesso!');
    });
  });

  it('deve processar upload de arquivo PNG válido', async () => {
    const pngBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==';
    mockFileReader.result = pngBase64;
    
    render(<AvatarUpload onChange={mockOnChange} />);
    
    const file = new File(['fake-png-content'], 'avatar.png', { type: 'image/png' });
    Object.defineProperty(file, 'size', { value: 500 * 1024 }); // 500KB
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    if (mockFileReader.onload) {
      mockFileReader.onload();
    }
    
    await waitFor(() => {
      expect(mockOnChange).toHaveBeenCalledWith(pngBase64);
      expect(message.success).toHaveBeenCalledWith('Avatar atualizado com sucesso!');
    });
  });

  it('deve rejeitar arquivo muito grande', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    // Arquivo de 3MB (acima do limite de 2MB)
    const file = new File(['fake-large-content'], 'large.jpg', { type: 'image/jpeg' });
    Object.defineProperty(file, 'size', { value: 3 * 1024 * 1024 }); // 3MB
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Imagem muito grande! Máximo 2MB permitido.');
      expect(mockOnChange).not.toHaveBeenCalled();
    });
  });

  it('deve rejeitar tipo de arquivo não suportado', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    // Arquivo PDF não suportado
    const file = new File(['fake-pdf-content'], 'document.pdf', { type: 'application/pdf' });
    Object.defineProperty(file, 'size', { value: 1024 }); // 1KB
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Formato não suportado! Use JPG, PNG ou GIF.');
      expect(mockOnChange).not.toHaveBeenCalled();
    });
  });

  it('deve remover avatar quando botão de delete é clicado', () => {
    render(<AvatarUpload value={validBase64} onChange={mockOnChange} />);
    
    const deleteButton = screen.getByRole('button', { name: /delete/i });
    fireEvent.click(deleteButton);
    
    expect(mockOnChange).toHaveBeenCalledWith(null);
    expect(message.success).toHaveBeenCalledWith('Avatar removido');
  });

  it('deve estar desabilitado quando disabled=true', () => {
    render(<AvatarUpload value={validBase64} onChange={mockOnChange} disabled />);
    
    const uploadButton = screen.getByRole('button', { name: /upload/i });
    const deleteButton = screen.queryByRole('button', { name: /delete/i });
    
    expect(uploadButton).toBeDisabled();
    expect(deleteButton).not.toBeInTheDocument(); // Botão de delete não deve aparecer quando disabled
  });

  it('deve mostrar estado de loading durante upload', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    const file = new File(['fake-content'], 'avatar.jpg', { type: 'image/jpeg' });
    Object.defineProperty(file, 'size', { value: 1024 });
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    // Antes do FileReader resolver, deve mostrar loading
    expect(screen.getByText('Processando...')).toBeInTheDocument();
    
    // Simular resolução do FileReader
    if (mockFileReader.onload) {
      mockFileReader.onload();
    }
    
    await waitFor(() => {
      expect(screen.queryByText('Processando...')).not.toBeInTheDocument();
    });
  });

  it('deve lidar com erro no FileReader', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    const file = new File(['fake-content'], 'avatar.jpg', { type: 'image/jpeg' });
    Object.defineProperty(file, 'size', { value: 1024 });
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    // Simular erro no FileReader
    if (mockFileReader.onerror) {
      mockFileReader.onerror();
    }
    
    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Erro ao processar imagem');
      expect(mockOnChange).not.toHaveBeenCalled();
    });
  });

  it('deve usar tamanho personalizado', () => {
    render(<AvatarUpload size={64} onChange={mockOnChange} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar.parentElement).toHaveClass('ant-avatar');
    // O tamanho específico é definido via props do Ant Design
  });

  it('deve usar placeholder personalizado', () => {
    const customPlaceholder = 'Selecionar foto';
    render(<AvatarUpload placeholder={customPlaceholder} onChange={mockOnChange} />);
    
    expect(screen.getByText(customPlaceholder)).toBeInTheDocument();
    expect(screen.queryByText('Clique para fazer upload')).not.toBeInTheDocument();
  });

  it('deve aceitar múltiplos formatos de imagem', async () => {
    // Teste com GIF
    const gifBase64 = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
    mockFileReader.result = gifBase64;
    
    render(<AvatarUpload onChange={mockOnChange} />);
    
    const file = new File(['fake-gif-content'], 'avatar.gif', { type: 'image/gif' });
    Object.defineProperty(file, 'size', { value: 1024 });
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    if (mockFileReader.onload) {
      mockFileReader.onload();
    }
    
    await waitFor(() => {
      expect(mockOnChange).toHaveBeenCalledWith(gifBase64);
      expect(message.success).toHaveBeenCalledWith('Avatar atualizado com sucesso!');
    });
  });

  it('deve limpar estado de loading após erro', async () => {
    render(<AvatarUpload onChange={mockOnChange} />);
    
    // Arquivo inválido
    const file = new File(['fake-content'], 'document.txt', { type: 'text/plain' });
    Object.defineProperty(file, 'size', { value: 1024 });
    
    const input = screen.getByRole('button', { name: /upload/i });
    const uploadInput = input.querySelector('input[type="file"]') as HTMLInputElement;
    
    fireEvent.change(uploadInput, { target: { files: [file] } });
    
    await waitFor(() => {
      // Após erro, não deve estar em loading
      expect(screen.queryByText('Processando...')).not.toBeInTheDocument();
      expect(screen.getByText('Clique para fazer upload')).toBeInTheDocument();
    });
  });
});