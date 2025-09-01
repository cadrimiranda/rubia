using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace Rubia.Server.Entities;

[Table("conversation_participants")]
[Index(nameof(ConversationId), nameof(CustomerId), IsUnique = true, Name = "IX_ConversationParticipants_ConversationId_CustomerId")]
[Index(nameof(ConversationId), nameof(UserId), IsUnique = true, Name = "IX_ConversationParticipants_ConversationId_UserId")]
[Index(nameof(ConversationId), nameof(AiAgentId), IsUnique = true, Name = "IX_ConversationParticipants_ConversationId_AiAgentId")]
public class ConversationParticipant : BaseEntity
{
    [Column("is_active")]
    public bool IsActive { get; set; } = true; // Indica se o participante está ativo na conversa

    [Column("joined_at")]
    [Required]
    public DateTime JoinedAt { get; set; } = DateTime.UtcNow; // Quando o participante entrou na conversa

    [Column("left_at")]
    public DateTime? LeftAt { get; set; } // Quando o participante saiu da conversa (se aplicável)

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Column("conversation_id")]
    [Required]
    public Guid ConversationId { get; set; }
    
    [ForeignKey("ConversationId")]
    public virtual Conversation Conversation { get; set; } = null!;

    // Apenas um dos participantes deve ser não-nulo
    [Column("customer_id")]
    public Guid? CustomerId { get; set; }
    
    [ForeignKey("CustomerId")]
    public virtual Customer? Customer { get; set; }

    [Column("user_id")]
    public Guid? UserId { get; set; }
    
    [ForeignKey("UserId")]
    public virtual User? User { get; set; }

    [Column("ai_agent_id")]
    public Guid? AiAgentId { get; set; }
    
    [ForeignKey("AiAgentId")]
    public virtual AIAgent? AiAgent { get; set; }
}