using System.ComponentModel;

namespace Rubia.Server.Enums;

public enum MessagingProvider
{
    [Description("Z-API")]
    ZApi,
    
    [Description("Twilio")]
    Twilio,
    
    [Description("WhatsApp Business API")]
    WhatsAppBusinessApi,
    
    [Description("Mock Provider")]
    Mock
}