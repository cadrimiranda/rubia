using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum WhatsAppInstanceStatus
{
    [Description("Não Configurado")]
    NotConfigured,
    
    [Description("Configurando")]
    Configuring,
    
    [Description("Aguardando QR Code")]
    AwaitingQrScan,
    
    [Description("Conectando")]
    Connecting,
    
    [Description("Conectado")]
    Connected,
    
    [Description("Desconectado")]
    Disconnected,
    
    [Description("Erro")]
    Error,
    
    [Description("Suspenso")]
    Suspended
}