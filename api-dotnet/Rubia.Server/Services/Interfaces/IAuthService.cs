using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IAuthService
{
    Task<AuthResponse> LoginAsync(LoginRequest request, HttpContext httpContext);
    Task<AuthResponse> RefreshAsync(RefreshRequest request);
}