using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum UserRole
{
    [Description("Administrador")]
    Admin,
    
    [Description("Supervisor")]
    Supervisor,
    
    [Description("Agente")]
    Agent
}