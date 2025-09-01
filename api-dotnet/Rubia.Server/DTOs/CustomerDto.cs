namespace Rubia.Server.DTOs;

public class CustomerDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string Phone { get; set; } = string.Empty;
    public string? Name { get; set; }
    public string? WhatsappId { get; set; }
    public string? ProfileUrl { get; set; }
    public bool IsBlocked { get; set; }
    public string? SourceSystemName { get; set; }
    public string? SourceSystemId { get; set; }
    public DateTime? ImportedAt { get; set; }
    public DateOnly? BirthDate { get; set; }
    public DateOnly? LastDonationDate { get; set; }
    public DateOnly? NextEligibleDonationDate { get; set; }
    public string? BloodType { get; set; }
    public int? Height { get; set; }
    public double? Weight { get; set; }
    public string? AddressStreet { get; set; }
    public string? AddressNumber { get; set; }
    public string? AddressComplement { get; set; }
    public string? AddressPostalCode { get; set; }
    public string? AddressCity { get; set; }
    public string? AddressState { get; set; }
    public string? Email { get; set; }
    public string? Cpf { get; set; }
    public string? Rg { get; set; }
    public string? RhFactor { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}