using System.ComponentModel.DataAnnotations;
using System.Text.RegularExpressions;

namespace Rubia.Server.DTOs;

public class CreateCustomerDto : IValidatableObject
{
    [Required(ErrorMessage = "Telefone é obrigatório")]
    public string Phone { get; set; } = string.Empty;

    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string? Name { get; set; }

    [StringLength(255, ErrorMessage = "WhatsApp ID não pode exceder 255 caracteres")]
    public string? WhatsappId { get; set; }

    public string? ProfileUrl { get; set; }

    public bool IsBlocked { get; set; } = false;

    public string? SourceSystemName { get; set; }

    public string? SourceSystemId { get; set; }

    public DateTime? ImportedAt { get; set; }

    public DateOnly? BirthDate { get; set; }

    public DateOnly? LastDonationDate { get; set; }

    public DateOnly? NextEligibleDonationDate { get; set; }

    [StringLength(10, ErrorMessage = "Tipo sanguíneo não pode exceder 10 caracteres")]
    public string? BloodType { get; set; }

    public int? Height { get; set; } // Altura em centímetros

    public double? Weight { get; set; } // Peso em quilogramas

    // Campos de endereço
    [StringLength(255, ErrorMessage = "Rua não pode exceder 255 caracteres")]
    public string? AddressStreet { get; set; }

    [StringLength(20, ErrorMessage = "Número não pode exceder 20 caracteres")]
    public string? AddressNumber { get; set; }

    [StringLength(255, ErrorMessage = "Complemento não pode exceder 255 caracteres")]
    public string? AddressComplement { get; set; }

    [StringLength(10, ErrorMessage = "CEP não pode exceder 10 caracteres")]
    public string? AddressPostalCode { get; set; }

    [StringLength(100, ErrorMessage = "Cidade não pode exceder 100 caracteres")]
    public string? AddressCity { get; set; }

    [StringLength(2, ErrorMessage = "Estado deve ter 2 caracteres")]
    public string? AddressState { get; set; }

    // Campos adicionais para campanhas
    [StringLength(255, ErrorMessage = "Email não pode exceder 255 caracteres")]
    [EmailAddress(ErrorMessage = "Email deve ter formato válido")]
    public string? Email { get; set; }

    [StringLength(14, ErrorMessage = "CPF não pode exceder 14 caracteres")]
    public string? Cpf { get; set; }

    [StringLength(20, ErrorMessage = "RG não pode exceder 20 caracteres")]
    public string? Rg { get; set; }

    [StringLength(10, ErrorMessage = "Fator RH não pode exceder 10 caracteres")]
    public string? RhFactor { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        // Validar telefone - formato brasileiro
        if (!string.IsNullOrEmpty(Phone))
        {
            var phoneRegex = new Regex(@"^\+55\d{10,11}$");
            if (!phoneRegex.IsMatch(Phone))
            {
                results.Add(new ValidationResult(
                    "Telefone deve estar no formato +55XXXXXXXXXXX",
                    new[] { nameof(Phone) }));
            }
        }

        // Validar CEP
        if (!string.IsNullOrEmpty(AddressPostalCode))
        {
            var cepRegex = new Regex(@"^\d{5}-?\d{3}$");
            if (!cepRegex.IsMatch(AddressPostalCode))
            {
                results.Add(new ValidationResult(
                    "CEP deve estar no formato XXXXX-XXX",
                    new[] { nameof(AddressPostalCode) }));
            }
        }

        // Validar estado (UF)
        if (!string.IsNullOrEmpty(AddressState))
        {
            var stateRegex = new Regex(@"^[A-Z]{2}$");
            if (!stateRegex.IsMatch(AddressState))
            {
                results.Add(new ValidationResult(
                    "Estado deve ser uma UF válida (ex: SP, RJ)",
                    new[] { nameof(AddressState) }));
            }
        }

        return results;
    }
}