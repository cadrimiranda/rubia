using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum CampaignContactStatus
{
    [Description("Pendente")]
    Pending,    // Aguardando envio da mensagem
    
    [Description("Enviado")]
    Sent,       // Mensagem enviada
    
    [Description("Entregue")]
    Delivered,  // Mensagem entregue ao WhatsApp
    
    [Description("Lida")]
    Read,       // Mensagem lida pelo contato
    
    [Description("Falhou")]
    Failed,     // Falha no envio da mensagem
    
    [Description("Respondeu")]
    Responded,  // Contato respondeu à campanha
    
    [Description("Convertido")]
    Converted,  // Contato se tornou um cliente ou atingiu o objetivo da campanha
    
    [Description("Opt-out")]
    OptOut      // Contato pediu para não receber mais mensagens da campanha
}