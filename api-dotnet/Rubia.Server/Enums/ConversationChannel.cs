using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum ConversationChannel
{
    [Description("WhatsApp")]
    WhatsApp,
    
    [Description("SMS")]
    SMS,
    
    [Description("Email")]
    Email,
    
    [Description("Telegram")]
    Telegram,
    
    [Description("Webchat")]
    Webchat,
    
    [Description("Sistema")]
    System
}