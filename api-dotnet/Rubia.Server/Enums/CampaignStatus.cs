using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum CampaignStatus
{
    [Description("Rascunho")]
    Draft,      // Rascunho, ainda em configuração
    
    [Description("Ativo")]
    Active,     // Em execução, enviando mensagens
    
    [Description("Pausado")]
    Paused,     // Pausada temporariamente
    
    [Description("Concluído")]
    Completed,  // Concluída (todas as mensagens enviadas ou data final atingida)
    
    [Description("Cancelado")]
    Canceled    // Cancelada antes de concluir
}