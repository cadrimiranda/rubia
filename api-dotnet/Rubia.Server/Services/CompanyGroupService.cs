using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class CompanyGroupService : ICompanyGroupService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<CompanyGroupService> _logger;

    public CompanyGroupService(RubiaDbContext context, ILogger<CompanyGroupService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<CompanyGroupDto> CreateAsync(CreateCompanyGroupDto createDto)
    {
        _logger.LogInformation("Creating company group with name: {Name}", createDto.Name);

        if (await _context.CompanyGroups.AnyAsync(cg => cg.Name == createDto.Name))
        {
            throw new ArgumentException($"Company group with name '{createDto.Name}' already exists");
        }

        var companyGroup = new CompanyGroup
        {
            Name = createDto.Name,
            Description = createDto.Description,
            IsActive = createDto.IsActive
        };

        _context.CompanyGroups.Add(companyGroup);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Company group created successfully with id: {Id}", companyGroup.Id);
        return ToDto(companyGroup);
    }

    public async Task<List<CompanyGroupDto>> FindAllAsync()
    {
        var companyGroups = await _context.CompanyGroups
            .OrderBy(cg => cg.Name)
            .ToListAsync();

        return companyGroups.Select(ToDto).ToList();
    }

    public async Task<CompanyGroupDto> FindByIdAsync(Guid id)
    {
        var companyGroup = await _context.CompanyGroups
            .FirstOrDefaultAsync(cg => cg.Id == id);

        if (companyGroup == null)
        {
            throw new ArgumentException($"Company group not found with id: {id}");
        }

        return ToDto(companyGroup);
    }

    public async Task<CompanyGroupDto> UpdateAsync(Guid id, UpdateCompanyGroupDto updateDto)
    {
        _logger.LogInformation("Updating company group with id: {Id}", id);

        var existingCompanyGroup = await _context.CompanyGroups
            .FirstOrDefaultAsync(cg => cg.Id == id);

        if (existingCompanyGroup == null)
        {
            throw new ArgumentException($"Company group not found with id: {id}");
        }

        if (updateDto.Name != null && updateDto.Name != existingCompanyGroup.Name)
        {
            if (await _context.CompanyGroups.AnyAsync(cg => cg.Name == updateDto.Name))
            {
                throw new ArgumentException($"Company group with name '{updateDto.Name}' already exists");
            }
            existingCompanyGroup.Name = updateDto.Name;
        }

        if (updateDto.Description != null)
        {
            existingCompanyGroup.Description = updateDto.Description;
        }

        if (updateDto.IsActive.HasValue)
        {
            existingCompanyGroup.IsActive = updateDto.IsActive.Value;
        }

        await _context.SaveChangesAsync();
        _logger.LogInformation("Company group updated successfully with id: {Id}", existingCompanyGroup.Id);

        return ToDto(existingCompanyGroup);
    }

    public async Task DeleteAsync(Guid id)
    {
        _logger.LogInformation("Deleting company group with id: {Id}", id);

        var companyGroup = await _context.CompanyGroups
            .FirstOrDefaultAsync(cg => cg.Id == id);

        if (companyGroup == null)
        {
            throw new ArgumentException($"Company group not found with id: {id}");
        }

        _context.CompanyGroups.Remove(companyGroup);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Company group deleted successfully");
    }

    private static CompanyGroupDto ToDto(CompanyGroup companyGroup)
    {
        return new CompanyGroupDto
        {
            Id = companyGroup.Id,
            Name = companyGroup.Name,
            Description = companyGroup.Description,
            IsActive = companyGroup.IsActive,
            CreatedAt = companyGroup.CreatedAt,
            UpdatedAt = companyGroup.UpdatedAt
        };
    }
}