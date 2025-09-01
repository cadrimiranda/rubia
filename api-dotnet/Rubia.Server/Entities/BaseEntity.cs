using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.Entities;

public abstract class BaseEntity
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    // Métodos utilitários
    public bool IsNew() => Id == Guid.Empty;
    
    public bool BelongsToCompany(Guid companyId, Company? company) 
        => company != null && company.Id == companyId;
}