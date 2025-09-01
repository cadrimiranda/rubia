using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace Rubia.Server.Services;

public class JwtService
{
    private readonly IConfiguration _configuration;
    private readonly string _secretKey;
    private readonly TimeSpan _jwtExpiration;

    public JwtService(IConfiguration configuration)
    {
        _configuration = configuration;
        _secretKey = _configuration["JWT_SECRET"] ?? throw new InvalidOperationException("JWT_SECRET not configured");
        _jwtExpiration = TimeSpan.FromMilliseconds(
            long.Parse(_configuration["JWT_EXPIRATION"] ?? "86400000")); // 24 hours
    }

    public string? ExtractUsername(string token)
    {
        return ExtractClaim(token, x => x.Subject);
    }

    public Guid? ExtractCompanyId(string token)
    {
        var companyIdStr = ExtractClaim(token, x => x.Claims.FirstOrDefault(c => c.Type == "companyId")?.Value);
        return companyIdStr != null && Guid.TryParse(companyIdStr, out var companyId) ? companyId : null;
    }

    public Guid? ExtractCompanyGroupId(string token)
    {
        var companyGroupIdStr = ExtractClaim(token, x => x.Claims.FirstOrDefault(c => c.Type == "companyGroupId")?.Value);
        return companyGroupIdStr != null && Guid.TryParse(companyGroupIdStr, out var companyGroupId) ? companyGroupId : null;
    }

    public T? ExtractClaim<T>(string token, Func<JwtSecurityToken, T> claimsResolver)
    {
        var claims = ExtractAllClaims(token);
        return claims != null ? claimsResolver(claims) : default(T);
    }

    public string GenerateToken(string username, Guid companyGroupId, string companySlug)
    {
        var extraClaims = new Dictionary<string, object>
        {
            ["companyGroupId"] = companyGroupId.ToString(),
            ["companySlug"] = companySlug
        };
        return GenerateToken(extraClaims, username);
    }

    public string GenerateToken(Dictionary<string, object> extraClaims, string username)
    {
        return BuildToken(extraClaims, username, _jwtExpiration);
    }

    public long GetExpirationTime()
    {
        return (long)_jwtExpiration.TotalMilliseconds;
    }

    private string BuildToken(Dictionary<string, object> extraClaims, string username, TimeSpan expiration)
    {
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, username),
            new(JwtRegisteredClaimNames.Iat, DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString(), ClaimValueTypes.Integer64)
        };

        foreach (var extraClaim in extraClaims)
        {
            claims.Add(new Claim(extraClaim.Key, extraClaim.Value.ToString() ?? string.Empty));
        }

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_secretKey));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            claims: claims,
            expires: DateTime.UtcNow.Add(expiration),
            signingCredentials: creds
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public bool IsTokenValid(string token, string username)
    {
        var extractedUsername = ExtractUsername(token);
        return extractedUsername == username && !IsTokenExpired(token);
    }

    private bool IsTokenExpired(string token)
    {
        var expiration = ExtractExpiration(token);
        return expiration < DateTime.UtcNow;
    }

    private DateTime ExtractExpiration(string token)
    {
        return ExtractClaim(token, x => x.ValidTo) ?? DateTime.MinValue;
    }

    private JwtSecurityToken? ExtractAllClaims(string token)
    {
        try
        {
            var tokenHandler = new JwtSecurityTokenHandler();
            var key = Encoding.UTF8.GetBytes(_secretKey);

            tokenHandler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(key),
                ValidateIssuer = false,
                ValidateAudience = false,
                ClockSkew = TimeSpan.Zero
            }, out SecurityToken validatedToken);

            return (JwtSecurityToken)validatedToken;
        }
        catch
        {
            return null;
        }
    }
}