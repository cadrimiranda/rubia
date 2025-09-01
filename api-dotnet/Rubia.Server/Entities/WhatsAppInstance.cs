using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("whatsapp_instances")]
public class WhatsAppInstance : BaseEntity
{
    [Required]
    [Column("phone_number")]
    public string PhoneNumber { get; set; } = string.Empty;

    [Column("display_name")]
    public string? DisplayName { get; set; }

    [Column("provider")]
    [Required]
    public MessagingProvider Provider { get; set; } = MessagingProvider.ZApi;

    [Column("instance_id")]
    public string? InstanceId { get; set; }

    [Column("access_token")]
    public string? AccessToken { get; set; }

    [Column("webhook_url")]
    public string? WebhookUrl { get; set; }

    [Column("is_active")]
    public bool IsActive { get; set; } = true;

    [Column("is_primary")]
    public bool IsPrimary { get; set; } = false;

    [Column("configuration_data", TypeName = "TEXT")]
    public string? ConfigurationData { get; set; }

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    // Helper methods
    public bool IsConfigured() => !string.IsNullOrEmpty(InstanceId) && !string.IsNullOrEmpty(AccessToken);
    
    public bool IsConnected() => IsConfigured() && IsActive;
    
    public bool NeedsConfiguration() => !IsConfigured();
}