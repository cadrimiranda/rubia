using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class CustomerService : ICustomerService
{
    private readonly RubiaDbContext _context;
    private readonly IPhoneService _phoneService;
    private readonly ILogger<CustomerService> _logger;

    public CustomerService(RubiaDbContext context, IPhoneService phoneService, ILogger<CustomerService> logger)
    {
        _context = context;
        _phoneService = phoneService;
        _logger = logger;
    }

    public async Task<CustomerDto> CreateAsync(CreateCustomerDto createDto, Guid companyId)
    {
        _logger.LogInformation("Creating customer with phone: {Phone} for company: {CompanyId}", createDto.Phone, companyId);

        // Buscar a empresa
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == companyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        // Normalizar telefone antes de verificar duplicação
        var normalizedPhone = _phoneService.Normalize(createDto.Phone);

        if (await _context.Customers.AnyAsync(c => c.Phone == normalizedPhone && c.CompanyId == companyId))
        {
            throw new ArgumentException($"Cliente com telefone '{normalizedPhone}' já existe nesta empresa");
        }

        if (!string.IsNullOrEmpty(createDto.WhatsappId) && 
            await _context.Customers.AnyAsync(c => c.WhatsappId == createDto.WhatsappId && c.CompanyId == companyId))
        {
            throw new ArgumentException($"Cliente com WhatsApp ID '{createDto.WhatsappId}' já existe nesta empresa");
        }

        var customer = new Customer
        {
            Phone = normalizedPhone,
            Name = createDto.Name,
            WhatsappId = createDto.WhatsappId,
            ProfileUrl = createDto.ProfileUrl,
            IsBlocked = createDto.IsBlocked,
            SourceSystemName = createDto.SourceSystemName,
            SourceSystemId = createDto.SourceSystemId,
            ImportedAt = createDto.ImportedAt,
            BirthDate = createDto.BirthDate,
            LastDonationDate = createDto.LastDonationDate,
            NextEligibleDonationDate = createDto.NextEligibleDonationDate,
            BloodType = createDto.BloodType,
            Height = createDto.Height,
            Weight = createDto.Weight,
            AddressStreet = createDto.AddressStreet,
            AddressNumber = createDto.AddressNumber,
            AddressComplement = createDto.AddressComplement,
            AddressPostalCode = createDto.AddressPostalCode,
            AddressCity = createDto.AddressCity,
            AddressState = createDto.AddressState,
            Email = createDto.Email,
            Cpf = createDto.Cpf,
            Rg = createDto.Rg,
            RhFactor = createDto.RhFactor,
            CompanyId = companyId
        };

        _context.Customers.Add(customer);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Customer created successfully with id: {Id}", customer.Id);
        return ToDto(customer);
    }

    public async Task<CustomerDto> FindByIdAsync(Guid id, Guid companyId)
    {
        _logger.LogDebug("Finding customer by id: {Id} for company: {CompanyId}", id, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == id);

        if (customer == null)
        {
            throw new ArgumentException("Cliente não encontrado");
        }

        // Validate customer belongs to company
        if (customer.CompanyId != companyId)
        {
            throw new ArgumentException("Cliente não pertence a esta empresa");
        }

        return ToDto(customer);
    }

    public async Task<CustomerDto> UpdateAsync(Guid id, UpdateCustomerDto updateDto, Guid companyId)
    {
        _logger.LogInformation("Updating customer with id: {Id} for company: {CompanyId}", id, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == id);

        if (customer == null)
        {
            throw new ArgumentException("Cliente não encontrado");
        }

        // Validate customer belongs to company
        if (customer.CompanyId != companyId)
        {
            throw new ArgumentException("Cliente não pertence a esta empresa");
        }

        if (updateDto.Phone != null)
        {
            // Normalizar telefone antes de verificar duplicação
            var normalizedPhone = _phoneService.Normalize(updateDto.Phone);
            if (normalizedPhone != customer.Phone)
            {
                if (await _context.Customers.AnyAsync(c => c.Phone == normalizedPhone && c.CompanyId == companyId))
                {
                    throw new ArgumentException($"Cliente com telefone '{normalizedPhone}' já existe nesta empresa");
                }
            }
            customer.Phone = normalizedPhone;
        }

        if (updateDto.Name != null) customer.Name = updateDto.Name;

        if (updateDto.WhatsappId != null)
        {
            if (updateDto.WhatsappId != customer.WhatsappId)
            {
                if (await _context.Customers.AnyAsync(c => c.WhatsappId == updateDto.WhatsappId && c.CompanyId == companyId))
                {
                    throw new ArgumentException($"Cliente com WhatsApp ID '{updateDto.WhatsappId}' já existe nesta empresa");
                }
            }
            customer.WhatsappId = updateDto.WhatsappId;
        }

        if (updateDto.ProfileUrl != null) customer.ProfileUrl = updateDto.ProfileUrl;
        if (updateDto.IsBlocked.HasValue) customer.IsBlocked = updateDto.IsBlocked.Value;
        if (updateDto.SourceSystemName != null) customer.SourceSystemName = updateDto.SourceSystemName;
        if (updateDto.SourceSystemId != null) customer.SourceSystemId = updateDto.SourceSystemId;
        if (updateDto.ImportedAt.HasValue) customer.ImportedAt = updateDto.ImportedAt;
        if (updateDto.BirthDate.HasValue) customer.BirthDate = updateDto.BirthDate;
        if (updateDto.LastDonationDate.HasValue) customer.LastDonationDate = updateDto.LastDonationDate;
        if (updateDto.NextEligibleDonationDate.HasValue) customer.NextEligibleDonationDate = updateDto.NextEligibleDonationDate;
        if (updateDto.BloodType != null) customer.BloodType = updateDto.BloodType;
        if (updateDto.Height.HasValue) customer.Height = updateDto.Height;
        if (updateDto.Weight.HasValue) customer.Weight = updateDto.Weight;
        if (updateDto.AddressStreet != null) customer.AddressStreet = updateDto.AddressStreet;
        if (updateDto.AddressNumber != null) customer.AddressNumber = updateDto.AddressNumber;
        if (updateDto.AddressComplement != null) customer.AddressComplement = updateDto.AddressComplement;
        if (updateDto.AddressPostalCode != null) customer.AddressPostalCode = updateDto.AddressPostalCode;
        if (updateDto.AddressCity != null) customer.AddressCity = updateDto.AddressCity;
        if (updateDto.AddressState != null) customer.AddressState = updateDto.AddressState;
        if (updateDto.Email != null) customer.Email = updateDto.Email;
        if (updateDto.Cpf != null) customer.Cpf = updateDto.Cpf;
        if (updateDto.Rg != null) customer.Rg = updateDto.Rg;
        if (updateDto.RhFactor != null) customer.RhFactor = updateDto.RhFactor;

        await _context.SaveChangesAsync();
        _logger.LogInformation("Customer updated successfully");

        return ToDto(customer);
    }

    public async Task<CustomerDto> BlockCustomerAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Blocking customer with id: {Id} for company: {CompanyId}", id, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == id);

        if (customer == null)
        {
            throw new ArgumentException("Cliente não encontrado");
        }

        // Validate customer belongs to company
        if (customer.CompanyId != companyId)
        {
            throw new ArgumentException("Cliente não pertence a esta empresa");
        }

        customer.IsBlocked = true;
        await _context.SaveChangesAsync();

        _logger.LogInformation("Customer blocked successfully");
        return ToDto(customer);
    }

    public async Task<CustomerDto> UnblockCustomerAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Unblocking customer with id: {Id} for company: {CompanyId}", id, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == id);

        if (customer == null)
        {
            throw new ArgumentException("Cliente não encontrado");
        }

        // Validate customer belongs to company
        if (customer.CompanyId != companyId)
        {
            throw new ArgumentException("Cliente não pertence a esta empresa");
        }

        customer.IsBlocked = false;
        await _context.SaveChangesAsync();

        _logger.LogInformation("Customer unblocked successfully");
        return ToDto(customer);
    }

    public async Task DeleteAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Deleting customer with id: {Id} for company: {CompanyId}", id, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == id);

        if (customer == null)
        {
            throw new ArgumentException("Cliente não encontrado");
        }

        // Validate customer belongs to company
        if (customer.CompanyId != companyId)
        {
            throw new ArgumentException("Cliente não pertence a esta empresa");
        }

        _context.Customers.Remove(customer);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Customer deleted successfully");
    }

    // Company-scoped methods
    public async Task<List<CustomerDto>> FindAllByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Finding all customers for company: {CompanyId}", companyId);

        var customers = await _context.Customers
            .Where(c => c.CompanyId == companyId)
            .OrderBy(c => c.Name ?? c.Phone)
            .ToListAsync();

        return customers.Select(ToDto).ToList();
    }

    public async Task<CustomerDto?> FindByPhoneAndCompanyAsync(string phone, Guid companyId)
    {
        _logger.LogDebug("Finding customer by phone: {Phone} for company: {CompanyId}", phone, companyId);

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Phone == phone && c.CompanyId == companyId);

        return customer != null ? ToDto(customer) : null;
    }

    public async Task<List<CustomerDto>> FindActiveByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Finding active customers for company: {CompanyId}", companyId);

        var customers = await _context.Customers
            .Where(c => !c.IsBlocked && c.CompanyId == companyId)
            .OrderBy(c => c.Name ?? c.Phone)
            .ToListAsync();

        return customers.Select(ToDto).ToList();
    }

    public async Task<List<CustomerDto>> SearchByNameOrPhoneAndCompanyAsync(string searchTerm, Guid companyId)
    {
        _logger.LogDebug("Searching customers by term: {SearchTerm} for company: {CompanyId}", searchTerm, companyId);

        var customers = await _context.Customers
            .Where(c => c.CompanyId == companyId && 
                       (c.Name != null && c.Name.Contains(searchTerm) || 
                        c.Phone.Contains(searchTerm)))
            .OrderBy(c => c.Name ?? c.Phone)
            .ToListAsync();

        return customers.Select(ToDto).ToList();
    }

    public async Task<long> CountActiveByCompanyAsync(Guid companyId)
    {
        return await _context.Customers
            .Where(c => !c.IsBlocked && c.CompanyId == companyId)
            .CountAsync();
    }

    public async Task DeleteAllByCompanyAsync(Guid companyId)
    {
        _logger.LogInformation("Deleting all customers for company: {CompanyId}", companyId);

        var customers = await _context.Customers
            .Where(c => c.CompanyId == companyId)
            .ToListAsync();

        _context.Customers.RemoveRange(customers);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Deleted {Count} customers for company: {CompanyId}", customers.Count, companyId);
    }

    private static CustomerDto ToDto(Customer customer)
    {
        return new CustomerDto
        {
            Id = customer.Id,
            CompanyId = customer.CompanyId,
            Phone = customer.Phone,
            Name = customer.Name,
            WhatsappId = customer.WhatsappId,
            ProfileUrl = customer.ProfileUrl,
            IsBlocked = customer.IsBlocked,
            SourceSystemName = customer.SourceSystemName,
            SourceSystemId = customer.SourceSystemId,
            ImportedAt = customer.ImportedAt,
            BirthDate = customer.BirthDate,
            LastDonationDate = customer.LastDonationDate,
            NextEligibleDonationDate = customer.NextEligibleDonationDate,
            BloodType = customer.BloodType,
            Height = customer.Height,
            Weight = customer.Weight,
            AddressStreet = customer.AddressStreet,
            AddressNumber = customer.AddressNumber,
            AddressComplement = customer.AddressComplement,
            AddressPostalCode = customer.AddressPostalCode,
            AddressCity = customer.AddressCity,
            AddressState = customer.AddressState,
            Email = customer.Email,
            Cpf = customer.Cpf,
            Rg = customer.Rg,
            RhFactor = customer.RhFactor,
            CreatedAt = customer.CreatedAt,
            UpdatedAt = customer.UpdatedAt
        };
    }
}