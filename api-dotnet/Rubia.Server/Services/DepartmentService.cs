using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class DepartmentService : IDepartmentService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<DepartmentService> _logger;

    public DepartmentService(RubiaDbContext context, ILogger<DepartmentService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<DepartmentDto> CreateAsync(CreateDepartmentDto createDto, Guid companyId)
    {
        _logger.LogInformation("Creating department with name: {Name} for company: {CompanyId}", 
            createDto.Name, companyId);

        if (await _context.Departments.AnyAsync(d => d.Name == createDto.Name && d.CompanyId == companyId))
        {
            throw new ArgumentException($"Departamento com nome '{createDto.Name}' já existe");
        }

        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == companyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        var department = new Department
        {
            Name = createDto.Name,
            Description = createDto.Description,
            CompanyId = companyId,
            AutoAssign = createDto.AutoAssign
        };

        _context.Departments.Add(department);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Department created successfully with id: {Id}", department.Id);
        return ToDto(department);
    }

    public async Task<DepartmentDto> FindByIdAsync(Guid id, Guid companyId)
    {
        _logger.LogDebug("Finding department by id: {Id} for company: {CompanyId}", id, companyId);

        var department = await _context.Departments
            .FirstOrDefaultAsync(d => d.Id == id && d.CompanyId == companyId);

        if (department == null)
        {
            throw new ArgumentException("Departamento não encontrado");
        }

        return ToDto(department);
    }

    public async Task<List<DepartmentDto>> FindAllAsync(Guid companyId)
    {
        _logger.LogDebug("Finding all departments for company: {CompanyId}", companyId);

        var departments = await _context.Departments
            .Where(d => d.CompanyId == companyId)
            .OrderBy(d => d.Name)
            .ToListAsync();

        return departments.Select(ToDto).ToList();
    }

    public async Task<List<DepartmentDto>> FindByAutoAssignAsync(Guid companyId)
    {
        _logger.LogDebug("Finding departments with auto assign enabled for company: {CompanyId}", companyId);

        var departments = await _context.Departments
            .Where(d => d.AutoAssign && d.CompanyId == companyId)
            .OrderBy(d => d.Name)
            .ToListAsync();

        return departments.Select(ToDto).ToList();
    }

    public async Task<DepartmentDto> UpdateAsync(Guid id, UpdateDepartmentDto updateDto, Guid companyId)
    {
        _logger.LogInformation("Updating department with id: {Id} for company: {CompanyId}", id, companyId);

        var department = await _context.Departments
            .FirstOrDefaultAsync(d => d.Id == id && d.CompanyId == companyId);

        if (department == null)
        {
            throw new ArgumentException("Departamento não encontrado");
        }

        if (updateDto.Name != null)
        {
            if (updateDto.Name != department.Name && 
                await _context.Departments.AnyAsync(d => d.Name == updateDto.Name && d.CompanyId == companyId))
            {
                throw new ArgumentException($"Departamento com nome '{updateDto.Name}' já existe");
            }
            department.Name = updateDto.Name;
        }

        if (updateDto.Description != null)
        {
            department.Description = updateDto.Description;
        }

        if (updateDto.AutoAssign.HasValue)
        {
            department.AutoAssign = updateDto.AutoAssign.Value;
        }

        await _context.SaveChangesAsync();
        _logger.LogInformation("Department updated successfully");

        return ToDto(department);
    }

    public async Task DeleteAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Deleting department with id: {Id} for company: {CompanyId}", id, companyId);

        var department = await _context.Departments
            .FirstOrDefaultAsync(d => d.Id == id && d.CompanyId == companyId);

        if (department == null)
        {
            throw new ArgumentException("Departamento não encontrado");
        }

        _context.Departments.Remove(department);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Department deleted successfully");
    }

    private static DepartmentDto ToDto(Department department)
    {
        return new DepartmentDto
        {
            Id = department.Id,
            CompanyId = department.CompanyId,
            Name = department.Name,
            Description = department.Description,
            AutoAssign = department.AutoAssign,
            CreatedAt = department.CreatedAt,
            UpdatedAt = department.UpdatedAt
        };
    }
}