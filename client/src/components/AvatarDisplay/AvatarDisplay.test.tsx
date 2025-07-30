import React from 'react';
import { render, screen } from '@testing-library/react';
import { AvatarDisplay } from './index';

describe('AvatarDisplay', () => {
  // Base64 de teste válido (1x1 pixel JPEG)
  const validBase64 = 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A/9k=';
  
  const validPngBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==';

  it('deve renderizar avatar com base64 válido', () => {
    render(<AvatarDisplay avatarBase64={validBase64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toBeInTheDocument();
    expect(avatar).toHaveAttribute('src', validBase64);
    expect(avatar).toHaveAttribute('alt', 'Avatar');
  });

  it('deve renderizar avatar PNG válido', () => {
    render(<AvatarDisplay avatarBase64={validPngBase64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveAttribute('src', validPngBase64);
  });

  it('deve renderizar ícone padrão quando não há avatar', () => {
    render(<AvatarDisplay />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toBeInTheDocument();
    expect(avatar).not.toHaveAttribute('src');
    
    // Deve ter classe para ícone padrão
    expect(avatar).toHaveClass('bg-gray-200');
  });

  it('deve renderizar ícone padrão para base64 null', () => {
    render(<AvatarDisplay avatarBase64={null} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).not.toHaveAttribute('src');
    expect(avatar).toHaveClass('bg-gray-200');
  });

  it('deve renderizar ícone padrão para base64 vazio', () => {
    render(<AvatarDisplay avatarBase64="" />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).not.toHaveAttribute('src');
    expect(avatar).toHaveClass('bg-gray-200');
  });

  it('deve renderizar ícone padrão para base64 inválido', () => {
    const invalidBase64 = 'invalid-base64-string';
    render(<AvatarDisplay avatarBase64={invalidBase64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).not.toHaveAttribute('src');
    expect(avatar).toHaveClass('bg-gray-200');
  });

  it('deve renderizar ícone padrão para formato não suportado', () => {
    const textBase64 = 'data:text/plain;base64,dGVzdA==';
    render(<AvatarDisplay avatarBase64={textBase64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).not.toHaveAttribute('src');
    expect(avatar).toHaveClass('bg-gray-200');
  });

  it('deve usar tamanho personalizado', () => {
    render(<AvatarDisplay avatarBase64={validBase64} size={64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toBeInTheDocument();
    // O tamanho é controlado pelo Ant Design via props
  });

  it('deve usar tamanho string (small, default, large)', () => {
    render(<AvatarDisplay avatarBase64={validBase64} size="large" />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toBeInTheDocument();
  });

  it('deve aplicar className personalizada', () => {
    const customClass = 'custom-avatar-class';
    render(<AvatarDisplay avatarBase64={validBase64} className={customClass} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveClass(customClass);
  });

  it('deve usar alt personalizado', () => {
    const customAlt = 'Foto do agente IA';
    render(<AvatarDisplay avatarBase64={validBase64} alt={customAlt} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveAttribute('alt', customAlt);
  });

  it('deve usar ícone de fallback personalizado quando fornecido', () => {
    const CustomIcon = () => <span data-testid="custom-icon">Custom</span>;
    
    render(<AvatarDisplay avatarBase64={null} fallbackIcon={<CustomIcon />} />);
    
    expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
  });

  it('deve validar corretamente diferentes formatos de imagem válidos', () => {
    // JPEG
    const jpegBase64 = 'data:image/jpeg;base64,validjpegdata';
    const { rerender } = render(<AvatarDisplay avatarBase64={jpegBase64} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', jpegBase64);

    // JPG
    const jpgBase64 = 'data:image/jpg;base64,validjpgdata';
    rerender(<AvatarDisplay avatarBase64={jpgBase64} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', jpgBase64);

    // PNG
    const pngBase64 = 'data:image/png;base64,validpngdata';
    rerender(<AvatarDisplay avatarBase64={pngBase64} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', pngBase64);

    // GIF
    const gifBase64 = 'data:image/gif;base64,validgifdata';
    rerender(<AvatarDisplay avatarBase64={gifBase64} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', gifBase64);
  });

  it('deve rejeitar formatos de data URL não-imagem', () => {
    const formats = [
      'data:application/pdf;base64,pdfdata',
      'data:text/plain;base64,textdata',
      'data:video/mp4;base64,videodata',
      'data:audio/mp3;base64,audiodata',
    ];

    formats.forEach(format => {
      const { rerender } = render(<AvatarDisplay avatarBase64={format} />);
      const avatar = screen.getByRole('img');
      expect(avatar).not.toHaveAttribute('src');
      expect(avatar).toHaveClass('bg-gray-200');
    });
  });

  it('deve lidar com base64 malformado graciosamente', () => {
    const malformedFormats = [
      'data:image/jpeg;base64', // Sem dados
      'data:image/jpeg', // Sem base64
      'image/jpeg;base64,data', // Sem data: prefix
      'data:image/;base64,data', // Tipo MIME incompleto
      'data:;base64,data', // Sem tipo MIME
    ];

    malformedFormats.forEach(format => {
      const { rerender } = render(<AvatarDisplay avatarBase64={format} />);
      const avatar = screen.getByRole('img');
      expect(avatar).not.toHaveAttribute('src');
      expect(avatar).toHaveClass('bg-gray-200');
    });
  });

  it('deve combinar className padrão com personalizada', () => {
    const customClass = 'my-custom-class';
    render(
      <AvatarDisplay 
        avatarBase64={null} 
        className={customClass}
      />
    );
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveClass(customClass);
    expect(avatar).toHaveClass('bg-gray-200'); // Classe padrão para fallback
  });

  it('deve manter proporção e comportamento responsivo', () => {
    render(<AvatarDisplay avatarBase64={validBase64} size="default" />);
    
    const avatar = screen.getByRole('img');
    expect(avatar.parentElement).toHaveClass('ant-avatar');
  });

  it('deve funcionar com diferentes tamanhos numéricos', () => {
    const sizes = [24, 32, 40, 48, 64, 80, 96, 128];
    
    sizes.forEach(size => {
      const { rerender } = render(<AvatarDisplay avatarBase64={validBase64} size={size} />);
      const avatar = screen.getByRole('img');
      expect(avatar).toBeInTheDocument();
      // O componente deve renderizar sem erros para todos os tamanhos
    });
  });

  it('deve preservar qualidade da imagem base64', () => {
    // Base64 com dados mais longos para simular imagem de qualidade
    const highQualityBase64 = validBase64 + 'additionaldata'.repeat(100);
    
    render(<AvatarDisplay avatarBase64={highQualityBase64} />);
    
    const avatar = screen.getByRole('img');
    expect(avatar).toHaveAttribute('src', highQualityBase64);
    // Deve preservar todos os dados base64
  });
});