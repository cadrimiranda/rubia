using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("conversation_media")]
public class ConversationMedia : BaseEntity
{
    [Required]
    [Column("file_url")]
    public string FileUrl { get; set; } = string.Empty; // A URL/caminho para o arquivo armazenado

    [Column("media_type")]
    [Required]
    public MediaType MediaType { get; set; } // Tipo de mídia (ex: IMAGE, VIDEO, AUDIO, DOCUMENT)

    [Column("mime_type")]
    [MaxLength(100)]
    public string? MimeType { get; set; } // Tipo MIME do arquivo

    [Column("original_file_name")]
    [MaxLength(255)]
    public string? OriginalFileName { get; set; } // Nome original do arquivo

    [Column("file_size_bytes")]
    public long? FileSizeBytes { get; set; } // Tamanho do arquivo em bytes

    [Column("checksum")]
    [MaxLength(64)]
    public string? Checksum { get; set; } // Para verificação de integridade (ex: SHA-256)

    [Column("uploaded_at")]
    [Required]
    public DateTime UploadedAt { get; set; } = DateTime.UtcNow; // Quando a mídia foi armazenada/registrada

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

    [Column("uploaded_by_user_id")]
    public Guid? UploadedByUserId { get; set; }
    
    [ForeignKey("UploadedByUserId")]
    public virtual User? UploadedByUser { get; set; }

    [Column("uploaded_by_customer_id")]
    public Guid? UploadedByCustomerId { get; set; }
    
    [ForeignKey("UploadedByCustomerId")]
    public virtual Customer? UploadedByCustomer { get; set; }

    public virtual ICollection<Message> Messages { get; set; } = new List<Message>();
}