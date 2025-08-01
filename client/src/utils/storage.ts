// Utilitários para integração com sistemas de storage (S3/GCS)

export interface StorageConfig {
  provider: 'S3' | 'GCS' | 'LOCAL';
  bucket?: string;
  region?: string;
  accessKey?: string;
  secretKey?: string;
  endpoint?: string;
}

export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}

export interface UploadResult {
  fileUrl: string;
  fileKey: string;
  publicUrl?: string;
}

export class StorageService {
  private config: StorageConfig;

  constructor(config: StorageConfig) {
    this.config = config;
  }

  // Upload de arquivo para storage
  async uploadFile(
    file: File,
    fileName: string,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<UploadResult> {
    switch (this.config.provider) {
      case 'S3':
        return this.uploadToS3(file, fileName, onProgress);
      case 'GCS':
        return this.uploadToGCS(file, fileName, onProgress);
      case 'LOCAL':
        return this.uploadToLocal(file, fileName, onProgress);
      default:
        throw new Error('Provider de storage não suportado');
    }
  }

  // Upload para Amazon S3
  private async uploadToS3(
    file: File,
    fileName: string,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<UploadResult> {
    if (!this.config.bucket || !this.config.accessKey || !this.config.secretKey) {
      throw new Error('Configuração S3 incompleta');
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileName', fileName);
    formData.append('bucket', this.config.bucket);
    formData.append('region', this.config.region || 'us-east-1');

    const xhr = new XMLHttpRequest();
    
    return new Promise((resolve, reject) => {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress({
            loaded: e.loaded,
            total: e.total,
            percentage: Math.round((e.loaded / e.total) * 100)
          });
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const result = JSON.parse(xhr.responseText);
          resolve({
            fileUrl: result.fileUrl,
            fileKey: result.fileKey,
            publicUrl: result.publicUrl
          });
        } else {
          reject(new Error(`Erro no upload S3: ${xhr.statusText}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Erro na conexão com S3'));
      });

      xhr.open('POST', '/api/storage/s3/upload');
      xhr.setRequestHeader('Authorization', `Bearer ${this.getAuthToken()}`);
      xhr.send(formData);
    });
  }

  // Upload para Google Cloud Storage
  private async uploadToGCS(
    file: File,
    fileName: string,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<UploadResult> {
    if (!this.config.bucket) {
      throw new Error('Bucket GCS não configurado');
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileName', fileName);
    formData.append('bucket', this.config.bucket);

    const xhr = new XMLHttpRequest();
    
    return new Promise((resolve, reject) => {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress({
            loaded: e.loaded,
            total: e.total,
            percentage: Math.round((e.loaded / e.total) * 100)
          });
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const result = JSON.parse(xhr.responseText);
          resolve({
            fileUrl: result.fileUrl,
            fileKey: result.fileKey,
            publicUrl: result.publicUrl
          });
        } else {
          reject(new Error(`Erro no upload GCS: ${xhr.statusText}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Erro na conexão com GCS'));
      });

      xhr.open('POST', '/api/storage/gcs/upload');
      xhr.setRequestHeader('Authorization', `Bearer ${this.getAuthToken()}`);
      xhr.send(formData);
    });
  }

  // Upload para storage local
  private async uploadToLocal(
    file: File,
    fileName: string,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<UploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileName', fileName);

    const xhr = new XMLHttpRequest();
    
    return new Promise((resolve, reject) => {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress({
            loaded: e.loaded,
            total: e.total,
            percentage: Math.round((e.loaded / e.total) * 100)
          });
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const result = JSON.parse(xhr.responseText);
          resolve({
            fileUrl: result.fileUrl,
            fileKey: result.fileKey,
            publicUrl: result.publicUrl
          });
        } else {
          reject(new Error(`Erro no upload local: ${xhr.statusText}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Erro no upload local'));
      });

      xhr.open('POST', '/api/storage/local/upload');
      xhr.setRequestHeader('Authorization', `Bearer ${this.getAuthToken()}`);
      xhr.send(formData);
    });
  }

  // Deletar arquivo do storage
  async deleteFile(fileKey: string): Promise<void> {
    const response = await fetch(`/api/storage/${this.config.provider.toLowerCase()}/delete`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.getAuthToken()}`
      },
      body: JSON.stringify({
        fileKey,
        bucket: this.config.bucket
      })
    });

    if (!response.ok) {
      throw new Error(`Erro ao deletar arquivo: ${response.statusText}`);
    }
  }

  // Gerar URL pré-assinada para download
  async generateDownloadUrl(fileKey: string, expiresIn: number = 3600): Promise<string> {
    const response = await fetch(`/api/storage/${this.config.provider.toLowerCase()}/download-url`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.getAuthToken()}`
      },
      body: JSON.stringify({
        fileKey,
        bucket: this.config.bucket,
        expiresIn
      })
    });

    if (!response.ok) {
      throw new Error(`Erro ao gerar URL de download: ${response.statusText}`);
    }

    const result = await response.json();
    return result.downloadUrl;
  }

  // Obter informações do arquivo
  async getFileInfo(fileKey: string): Promise<{
    size: number;
    lastModified: string;
    contentType: string;
    etag: string;
  }> {
    const response = await fetch(`/api/storage/${this.config.provider.toLowerCase()}/info`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.getAuthToken()}`
      },
      body: JSON.stringify({
        fileKey,
        bucket: this.config.bucket
      })
    });

    if (!response.ok) {
      throw new Error(`Erro ao obter informações do arquivo: ${response.statusText}`);
    }

    return response.json();
  }

  private getAuthToken(): string {
    // Implementar lógica para obter token de autenticação
    // Por exemplo, do localStorage ou context de autenticação
    return localStorage.getItem('auth_token') || '';
  }
}

// Instância singleton do serviço de storage
let storageService: StorageService | null = null;

export const getStorageService = (): StorageService => {
  if (!storageService) {
    // Configuração padrão - pode ser alterada via environment variables
    const config: StorageConfig = {
      provider: (import.meta.env.VITE_STORAGE_PROVIDER as 'S3' | 'GCS' | 'LOCAL') || 'LOCAL',
      bucket: import.meta.env.VITE_STORAGE_BUCKET,
      region: import.meta.env.VITE_STORAGE_REGION,
      accessKey: import.meta.env.VITE_STORAGE_ACCESS_KEY,
      secretKey: import.meta.env.VITE_STORAGE_SECRET_KEY,
      endpoint: import.meta.env.VITE_STORAGE_ENDPOINT
    };

    storageService = new StorageService(config);
  }

  return storageService;
};

// Utilitários para formatação
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const generateUniqueFileName = (originalName: string, prefix?: string): string => {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8);
  const extension = originalName.split('.').pop();
  const nameWithoutExtension = originalName.replace(`.${extension}`, '');
  
  return `${prefix ? prefix + '_' : ''}${nameWithoutExtension}_${timestamp}_${random}.${extension}`;
};