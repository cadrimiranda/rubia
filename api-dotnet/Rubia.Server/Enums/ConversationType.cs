namespace Rubia.Server.Enums;

/// <summary>
/// Tipo de conversa - mapeado como ordinal para PostgreSQL
/// ONE_TO_ONE = 0, GROUP_CHAT = 1
/// </summary>
public enum ConversationType
{
    OneToOne = 0,   // Conversa individual entre um Customer e a Empresa/Agente
    GroupChat = 1   // Conversa em grupo com múltiplos Customers e/ou Agentes
}