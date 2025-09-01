using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;
using System.Security.Cryptography;
using System.Text;

namespace Rubia.Server.Services;

public class AuthService : IAuthService
{
    private readonly RubiaDbContext _context;
    private readonly IJwtService _jwtService;
    private readonly ILogger<AuthService> _logger;

    public AuthService(RubiaDbContext context, IJwtService jwtService, ILogger<AuthService> logger)
    {
        _context = context;
        _jwtService = jwtService;
        _logger = logger;
    }

    public async Task<AuthResponse> LoginAsync(LoginRequest request, HttpContext httpContext)
    {
        _logger.LogDebug("Login attempt for email: {Email}", request.Email);

        // Extract company slug from request headers or subdomain
        var companySlug = ExtractCompanySlug(httpContext);
        if (string.IsNullOrEmpty(companySlug))
        {
            throw new ArgumentException("Company not found. Please check the subdomain or X-Company-Slug header.");
        }

        _logger.LogDebug("Company slug resolved: {CompanySlug}", companySlug);

        // Find company by slug
        var company = await _context.Companies
            .Include(c => c.CompanyGroup)
            .FirstOrDefaultAsync(c => c.Slug == companySlug && c.IsActive);

        if (company == null)
        {
            throw new ArgumentException("Company not found or inactive.");
        }

        // Find user by email and company group
        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Email == request.Email 
                                    && u.CompanyGroupId == company.CompanyGroupId 
                                    && u.IsActive);

        if (user == null)
        {
            _logger.LogWarning("User not found for email: {Email} and company group: {CompanyGroupId}", 
                request.Email, company.CompanyGroupId);
            throw new UnauthorizedAccessException("Invalid credentials or user not found for this company.");
        }

        // Verify password
        if (!VerifyPassword(request.Password, user.PasswordHash))
        {
            _logger.LogWarning("Invalid password for user: {Email}", request.Email);
            throw new UnauthorizedAccessException("Invalid credentials.");
        }

        _logger.LogDebug("Authentication successful for user: {Email}", request.Email);

        // Generate JWT token
        var token = _jwtService.GenerateToken(user.Email, company.CompanyGroupId, company.Slug);

        // Check if WhatsApp setup is required
        var hasWhatsAppInstance = await _context.WhatsAppInstances
            .AnyAsync(w => w.CompanyId == company.Id && w.IsActive);

        return new AuthResponse
        {
            Token = token,
            ExpiresIn = _jwtService.GetExpirationTime(),
            RequiresWhatsAppSetup = !hasWhatsAppInstance,
            User = new UserInfo
            {
                Id = user.Id,
                Name = user.Name,
                Email = user.Email,
                Role = user.Role.ToString(),
                CompanyId = company.Id,
                CompanySlug = company.Slug,
                CompanyGroupId = company.CompanyGroupId,
                DepartmentId = user.DepartmentId
            }
        };
    }

    public async Task<AuthResponse> RefreshAsync(RefreshRequest request)
    {
        var username = _jwtService.ExtractUsername(request.Token);
        if (string.IsNullOrEmpty(username))
        {
            throw new UnauthorizedAccessException("Invalid token.");
        }

        if (!_jwtService.IsTokenValid(request.Token, username))
        {
            throw new UnauthorizedAccessException("Token expired or invalid.");
        }

        var companyGroupId = _jwtService.ExtractCompanyGroupId(request.Token);
        if (!companyGroupId.HasValue)
        {
            throw new UnauthorizedAccessException("Invalid token context.");
        }

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Email == username 
                                    && u.CompanyGroupId == companyGroupId.Value 
                                    && u.IsActive);

        if (user == null)
        {
            throw new UnauthorizedAccessException("User not found.");
        }

        var company = await _context.Companies
            .FirstOrDefaultAsync(c => c.CompanyGroupId == companyGroupId.Value && c.IsActive);

        if (company == null)
        {
            throw new UnauthorizedAccessException("Company not found.");
        }

        var newToken = _jwtService.GenerateToken(user.Email, company.CompanyGroupId, company.Slug);

        return new AuthResponse
        {
            Token = newToken,
            ExpiresIn = _jwtService.GetExpirationTime(),
            User = new UserInfo
            {
                Id = user.Id,
                Name = user.Name,
                Email = user.Email,
                Role = user.Role.ToString(),
                CompanyId = company.Id,
                CompanySlug = company.Slug,
                CompanyGroupId = company.CompanyGroupId,
                DepartmentId = user.DepartmentId
            }
        };
    }

    private static string ExtractCompanySlug(HttpContext httpContext)
    {
        // Try X-Company-Slug header first
        if (httpContext.Request.Headers.TryGetValue("X-Company-Slug", out var headerValue))
        {
            return headerValue.FirstOrDefault() ?? string.Empty;
        }

        // Try to extract from subdomain
        var host = httpContext.Request.Host.Host;
        var parts = host.Split('.');
        
        // If we have a subdomain (more than 2 parts), return the first part
        if (parts.Length > 2)
        {
            return parts[0];
        }

        return string.Empty;
    }

    private static bool VerifyPassword(string password, string hash)
    {
        // For now, using simple hash comparison
        // In production, use BCrypt or similar
        return ComputeHash(password) == hash;
    }

    private static string ComputeHash(string input)
    {
        using var sha256 = SHA256.Create();
        var hashedBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(input));
        return Convert.ToBase64String(hashedBytes);
    }
}