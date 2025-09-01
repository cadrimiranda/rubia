namespace Rubia.Server.Services.Interfaces;

public interface IRedisCacheService
{
    Task<T?> GetAsync<T>(string key) where T : class;
    Task<string?> GetStringAsync(string key);
    Task SetAsync<T>(string key, T value, TimeSpan? expiry = null) where T : class;
    Task SetStringAsync(string key, string value, TimeSpan? expiry = null);
    Task<bool> ExistsAsync(string key);
    Task<bool> DeleteAsync(string key);
    Task<long> IncrementAsync(string key, long value = 1, TimeSpan? expiry = null);
    Task<bool> SetIfNotExistsAsync<T>(string key, T value, TimeSpan? expiry = null) where T : class;
    Task<TimeSpan?> GetExpiryAsync(string key);
    Task<bool> ExpireAsync(string key, TimeSpan expiry);
}