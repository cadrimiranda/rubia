using System.ComponentModel;

namespace Rubia.Server.Enums;

/// <summary>
/// Status da conversa - mapeado como ordinal para PostgreSQL
/// ENTRADA = 0, ESPERANDO = 1, FINALIZADOS = 2
/// </summary>
public enum ConversationStatus
{
    [Description("Entrada")]
    Entrada = 0,  // Nova conversa
    
    [Description("Esperando")]
    Esperando = 1,  // Aguardando atendimento
    
    [Description("Finalizados")]
    Finalizados = 2  // Conversa finalizada
}