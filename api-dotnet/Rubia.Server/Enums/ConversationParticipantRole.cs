using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum ConversationParticipantRole
{
    [Description("Membro")]
    Member,
    
    [Description("Moderador")]
    Moderator,
    
    [Description("Administrador")]
    Admin,
    
    [Description("Proprietário")]
    Owner
}