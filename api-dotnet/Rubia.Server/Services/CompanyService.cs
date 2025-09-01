using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class CompanyService : ICompanyService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<CompanyService> _logger;

    public CompanyService(RubiaDbContext context, ILogger<CompanyService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<List<CompanyDto>> FindAllAsync()
    {
        var companies = await _context.Companies
            .Include(c => c.CompanyGroup)
            .OrderBy(c => c.Name)
            .ToListAsync();

        return companies.Select(ToDto).ToList();
    }

    public async Task<List<CompanyDto>> FindActiveCompaniesAsync()
    {
        var companies = await _context.Companies
            .Include(c => c.CompanyGroup)
            .Where(c => c.IsActive)
            .OrderBy(c => c.Name)
            .ToListAsync();

        return companies.Select(ToDto).ToList();
    }

    public async Task<CompanyDto?> FindByIdAsync(Guid id)
    {
        var company = await _context.Companies
            .Include(c => c.CompanyGroup)
            .FirstOrDefaultAsync(c => c.Id == id);

        return company != null ? ToDto(company) : null;
    }

    public async Task<CompanyDto?> FindBySlugAsync(string slug)
    {
        var company = await _context.Companies
            .Include(c => c.CompanyGroup)
            .FirstOrDefaultAsync(c => c.Slug == slug);

        return company != null ? ToDto(company) : null;
    }

    public async Task<CompanyDto> CreateAsync(CreateCompanyDto createDto)
    {
        _logger.LogInformation("Creating company with slug: {Slug}", createDto.Slug);

        if (await _context.Companies.AnyAsync(c => c.Slug == createDto.Slug))
        {
            throw new ArgumentException($"Company with slug already exists: {createDto.Slug}");
        }

        var companyGroup = await _context.CompanyGroups
            .FirstOrDefaultAsync(cg => cg.Id == createDto.CompanyGroupId);

        if (companyGroup == null)
        {
            throw new ArgumentException($"Company Group not found: {createDto.CompanyGroupId}");
        }

        var company = new Company
        {
            Name = createDto.Name,
            Slug = createDto.Slug,
            Description = createDto.Description,
            ContactEmail = createDto.ContactEmail,
            ContactPhone = createDto.ContactPhone,
            LogoUrl = createDto.LogoUrl,
            IsActive = createDto.IsActive,
            PlanType = createDto.PlanType,
            MaxUsers = createDto.MaxUsers,
            MaxWhatsappNumbers = createDto.MaxWhatsappNumbers,
            MaxAiAgents = createDto.MaxAiAgents,
            CompanyGroupId = createDto.CompanyGroupId
        };

        _context.Companies.Add(company);
        await _context.SaveChangesAsync();

        // Reload with CompanyGroup for DTO conversion
        await _context.Entry(company)
            .Reference(c => c.CompanyGroup)
            .LoadAsync();

        _logger.LogInformation("Company created successfully with id: {Id}", company.Id);
        return ToDto(company);
    }

    public async Task<CompanyDto> UpdateAsync(Guid id, UpdateCompanyDto updateDto)
    {
        _logger.LogInformation("Updating company with id: {Id}", id);

        var existingCompany = await _context.Companies
            .Include(c => c.CompanyGroup)
            .FirstOrDefaultAsync(c => c.Id == id);

        if (existingCompany == null)
        {
            throw new ArgumentException($"Company not found: {id}");
        }

        if (updateDto.Name != null) existingCompany.Name = updateDto.Name;
        if (updateDto.Description != null) existingCompany.Description = updateDto.Description;
        if (updateDto.ContactEmail != null) existingCompany.ContactEmail = updateDto.ContactEmail;
        if (updateDto.ContactPhone != null) existingCompany.ContactPhone = updateDto.ContactPhone;
        if (updateDto.LogoUrl != null) existingCompany.LogoUrl = updateDto.LogoUrl;
        if (updateDto.IsActive.HasValue) existingCompany.IsActive = updateDto.IsActive.Value;
        if (updateDto.PlanType.HasValue) existingCompany.PlanType = updateDto.PlanType.Value;
        if (updateDto.MaxUsers.HasValue) existingCompany.MaxUsers = updateDto.MaxUsers.Value;
        if (updateDto.MaxWhatsappNumbers.HasValue) existingCompany.MaxWhatsappNumbers = updateDto.MaxWhatsappNumbers.Value;
        if (updateDto.MaxAiAgents.HasValue) existingCompany.MaxAiAgents = updateDto.MaxAiAgents.Value;

        await _context.SaveChangesAsync();
        _logger.LogInformation("Company updated successfully with id: {Id}", existingCompany.Id);

        return ToDto(existingCompany);
    }

    public async Task DeleteByIdAsync(Guid id)
    {
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == id);
        if (company != null)
        {
            _context.Companies.Remove(company);
            await _context.SaveChangesAsync();
        }
    }

    public async Task<bool> ExistsBySlugAsync(string slug)
    {
        return await _context.Companies.AnyAsync(c => c.Slug == slug);
    }

    public async Task<List<CompanyDto>> FindByPlanTypeAsync(CompanyPlanType planType)
    {
        var companies = await _context.Companies
            .Include(c => c.CompanyGroup)
            .Where(c => c.PlanType == planType)
            .OrderBy(c => c.Name)
            .ToListAsync();

        return companies.Select(ToDto).ToList();
    }

    private static CompanyDto ToDto(Company company)
    {
        return new CompanyDto
        {
            Id = company.Id,
            Name = company.Name,
            Slug = company.Slug,
            Description = company.Description,
            ContactEmail = company.ContactEmail,
            ContactPhone = company.ContactPhone,
            LogoUrl = company.LogoUrl,
            IsActive = company.IsActive,
            PlanType = company.PlanType,
            MaxUsers = company.MaxUsers,
            MaxWhatsappNumbers = company.MaxWhatsappNumbers,
            MaxAiAgents = company.MaxAiAgents,
            CompanyGroupId = company.CompanyGroupId,
            CreatedAt = company.CreatedAt,
            UpdatedAt = company.UpdatedAt
        };
    }
}