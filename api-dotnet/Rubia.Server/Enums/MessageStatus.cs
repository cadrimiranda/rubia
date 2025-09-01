using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum MessageStatus
{
    [Description("Rascunho")]
    Draft,
    
    [Description("Enviando")]
    Sending,
    
    [Description("Enviado")]
    Sent,
    
    [Description("Entregue")]
    Delivered,
    
    [Description("Lido")]
    Read,
    
    [Description("Falhou")]
    Failed
}