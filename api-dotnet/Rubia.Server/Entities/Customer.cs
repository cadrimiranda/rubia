using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("customers")]
public class Customer : BaseEntity
{
    [Required]
    [Column("phone")]
    [MaxLength(20)]
    public string Phone { get; set; } = string.Empty; // Número de telefone do contato/cliente

    [Column("name")]
    public string? Name { get; set; } // Nome do contato/cliente

    [Column("whatsapp_id")]
    public string? WhatsappId { get; set; } // Se disponível, o ID do WhatsApp

    [Column("profile_url")]
    public string? ProfileUrl { get; set; } // URL da foto de perfil, se disponível

    [Column("is_blocked")]
    public bool IsBlocked { get; set; } = false; // Indica se o cliente está bloqueado para comunicação

    // CAMPOS INTEGRADOS DA ANTIGA ENTIDADE 'CONTACT'
    [Column("source_system_name")]
    public string? SourceSystemName { get; set; } // Nome do sistema de origem

    [Column("source_system_id")]
    public string? SourceSystemId { get; set; } // ID original do contato no sistema de origem

    [Column("imported_at")]
    public DateTime? ImportedAt { get; set; } // Quando o registro foi criado/importado

    [Column("birth_date")]
    public DateOnly? BirthDate { get; set; } // Data de nascimento do cliente

    [Column("last_donation_date")]
    public DateOnly? LastDonationDate { get; set; } // Última data de doação (para doadores)

    [Column("next_eligible_donation_date")]
    public DateOnly? NextEligibleDonationDate { get; set; } // Próxima data elegível para doação

    [Column("blood_type")]
    [MaxLength(10)]
    public string? BloodType { get; set; } // Tipo sanguíneo (A+, B+, AB+, O+, A-, B-, AB-, O-)

    [Column("height")]
    public int? Height { get; set; } // Altura em centímetros

    [Column("weight")]
    public double? Weight { get; set; } // Peso em quilogramas

    // CAMPOS DE ENDEREÇO
    [Column("address_street")]
    public string? AddressStreet { get; set; } // Rua/Avenida

    [Column("address_number")]
    public string? AddressNumber { get; set; } // Número

    [Column("address_complement")]
    public string? AddressComplement { get; set; } // Complemento (apartamento, bloco, etc.)

    [Column("address_postal_code")]
    [MaxLength(10)]
    public string? AddressPostalCode { get; set; } // CEP

    [Column("address_city")]
    public string? AddressCity { get; set; } // Cidade

    [Column("address_state")]
    [MaxLength(20)]
    public string? AddressState { get; set; } // Estado (UF)

    // CAMPOS ADICIONAIS PARA CAMPANHA
    [Column("email")]
    public string? Email { get; set; } // Email do cliente

    [Column("cpf")]
    [MaxLength(11)]
    public string? Cpf { get; set; } // CPF do cliente

    [Column("rg")]
    [MaxLength(20)]
    public string? Rg { get; set; } // RG do cliente

    [Column("rh_factor")]
    [MaxLength(3)]
    public string? RhFactor { get; set; } // Fator RH (+, -)

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;
}