namespace Rubia.Server.Services.Interfaces;

public interface IJwtService
{
    string? ExtractUsername(string token);
    Guid? ExtractCompanyId(string token);
    Guid? ExtractCompanyGroupId(string token);
    T? ExtractClaim<T>(string token, Func<System.IdentityModel.Tokens.Jwt.JwtSecurityToken, T> claimsResolver);
    string GenerateToken(string username, Guid companyGroupId, string companySlug);
    string GenerateToken(Dictionary<string, object> extraClaims, string username);
    long GetExpirationTime();
    bool IsTokenValid(string token, string username);
}