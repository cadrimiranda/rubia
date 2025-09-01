using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("conversations")]
public class Conversation : BaseEntity
{
    [Column("channel")]
    [Required]
    public Channel Channel { get; set; } // WHATSAPP, etc.

    [Column("status")]
    [Required]
    public ConversationStatus Status { get; set; } // ENTRADA, ESPERANDO, FINALIZADOS

    [Column("priority")]
    public int? Priority { get; set; } // 1-5, etc.

    [Column("conversation_type")]
    [Required]
    public ConversationType ConversationType { get; set; } = ConversationType.OneToOne;

    [Column("chat_lid")]
    public string? ChatLid { get; set; }

    [Column("ai_auto_response_enabled")]
    [Required]
    public bool AiAutoResponseEnabled { get; set; } = true;

    [Column("ai_messages_used")]
    [Required]
    public int AiMessagesUsed { get; set; } = 0;

    [Column("ai_limit_reached_at")]
    public DateTime? AiLimitReachedAt { get; set; }

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Column("assigned_user_id")]
    public Guid? AssignedUserId { get; set; }
    
    [ForeignKey("AssignedUserId")]
    public virtual User? AssignedUser { get; set; } // Usuário humano atribuído a esta conversa

    [Column("owner_user_id")]
    public Guid? OwnerUserId { get; set; }
    
    [ForeignKey("OwnerUserId")]
    public virtual User? OwnerUser { get; set; } // Usuário humano que criou/é o "dono" da conversa

    [Column("campaign_id")]
    public Guid? CampaignId { get; set; }
    
    [ForeignKey("CampaignId")]
    public virtual Campaign? Campaign { get; set; } // Opcional, para indicar se a conversa faz parte de uma campanha

    public virtual ICollection<ConversationParticipant> Participants { get; set; } = new List<ConversationParticipant>();
    public virtual ICollection<Message> Messages { get; set; } = new List<Message>();
}