using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Middleware;

public class JwtAuthenticationMiddleware
{
    private readonly RequestDelegate _next;
    private readonly IJwtService _jwtService;

    public JwtAuthenticationMiddleware(RequestDelegate next, IJwtService jwtService)
    {
        _next = next;
        _jwtService = jwtService;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var token = ExtractTokenFromRequest(context.Request);

        if (!string.IsNullOrEmpty(token))
        {
            var username = _jwtService.ExtractUsername(token);
            
            if (!string.IsNullOrEmpty(username) && _jwtService.IsTokenValid(token, username))
            {
                var companyId = _jwtService.ExtractCompanyId(token);
                var companyGroupId = _jwtService.ExtractCompanyGroupId(token);

                var claims = new List<Claim>
                {
                    new(ClaimTypes.Name, username),
                    new(ClaimTypes.NameIdentifier, username)
                };

                if (companyId.HasValue)
                    claims.Add(new("companyId", companyId.Value.ToString()));

                if (companyGroupId.HasValue)
                    claims.Add(new("companyGroupId", companyGroupId.Value.ToString()));

                var identity = new ClaimsIdentity(claims, "jwt");
                context.User = new ClaimsPrincipal(identity);
            }
        }

        await _next(context);
    }

    private static string? ExtractTokenFromRequest(HttpRequest request)
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        
        if (!string.IsNullOrEmpty(authHeader) && authHeader.StartsWith("Bearer "))
        {
            return authHeader["Bearer ".Length..];
        }

        return null;
    }
}