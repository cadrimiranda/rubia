using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("ai_agents")]
public class AIAgent : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty; // Nome do agente de IA (ex: "Rubi", "Atendente Inteligente")

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; } // Descrição do agente

    [Column("avatar_base64", TypeName = "TEXT")]
    public string? AvatarBase64 { get; set; } // Base64 da imagem do agente

    [Required]
    [Column("temperament")]
    public string Temperament { get; set; } = string.Empty; // Temperamento/Personalidade (ex: "ENGRAÇADO", "SÉRIO", "NORMAL", "EMPATICO")

    [Column("max_response_length")]
    public int MaxResponseLength { get; set; } = 500; // Limite de caracteres para a resposta da IA

    [Column("temperature")]
    [Column(TypeName = "decimal(3,2)")]
    public decimal Temperature { get; set; } = 0.7m; // Parâmetro de criatividade da IA (0.0 a 1.0)

    [Column("ai_message_limit")]
    [Required]
    public int AiMessageLimit { get; set; } = 10; // Limite de mensagens que este agente pode responder por conversa

    [Column("is_active")]
    public bool IsActive { get; set; } = true; // Se o agente está ativo e disponível para uso

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Column("ai_model_id")]
    [Required]
    public Guid AiModelId { get; set; }
    
    [ForeignKey("AiModelId")]
    public virtual AIModel AiModel { get; set; } = null!; // Modelo de IA associado ao agente

    public virtual ICollection<Message> Messages { get; set; } = new List<Message>();
    public virtual ICollection<ConversationParticipant> ConversationParticipants { get; set; } = new List<ConversationParticipant>();
}