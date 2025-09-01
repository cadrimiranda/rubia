using StackExchange.Redis;
using System.Text.Json;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class RedisCacheService : IRedisCacheService
{
    private readonly IDatabase _database;
    private readonly ILogger<RedisCacheService> _logger;
    private readonly JsonSerializerOptions _jsonOptions;

    public RedisCacheService(IConnectionMultiplexer redis, ILogger<RedisCacheService> logger)
    {
        _database = redis.GetDatabase();
        _logger = logger;
        _jsonOptions = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            WriteIndented = false
        };
    }

    public async Task<T?> GetAsync<T>(string key) where T : class
    {
        try
        {
            var value = await _database.StringGetAsync(key);
            if (!value.HasValue)
                return null;

            return JsonSerializer.Deserialize<T>(value!, _jsonOptions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting value from Redis with key: {Key}", key);
            return null;
        }
    }

    public async Task<string?> GetStringAsync(string key)
    {
        try
        {
            return await _database.StringGetAsync(key);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting string value from Redis with key: {Key}", key);
            return null;
        }
    }

    public async Task SetAsync<T>(string key, T value, TimeSpan? expiry = null) where T : class
    {
        try
        {
            var json = JsonSerializer.Serialize(value, _jsonOptions);
            await _database.StringSetAsync(key, json, expiry);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error setting value in Redis with key: {Key}", key);
        }
    }

    public async Task SetStringAsync(string key, string value, TimeSpan? expiry = null)
    {
        try
        {
            await _database.StringSetAsync(key, value, expiry);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error setting string value in Redis with key: {Key}", key);
        }
    }

    public async Task<bool> ExistsAsync(string key)
    {
        try
        {
            return await _database.KeyExistsAsync(key);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking if key exists in Redis: {Key}", key);
            return false;
        }
    }

    public async Task<bool> DeleteAsync(string key)
    {
        try
        {
            return await _database.KeyDeleteAsync(key);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting key from Redis: {Key}", key);
            return false;
        }
    }

    public async Task<long> IncrementAsync(string key, long value = 1, TimeSpan? expiry = null)
    {
        try
        {
            var result = await _database.StringIncrementAsync(key, value);
            if (expiry.HasValue && result == value) // First time setting, add expiry
            {
                await _database.KeyExpireAsync(key, expiry.Value);
            }
            return result;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error incrementing value in Redis with key: {Key}", key);
            return 0;
        }
    }

    public async Task<bool> SetIfNotExistsAsync<T>(string key, T value, TimeSpan? expiry = null) where T : class
    {
        try
        {
            var json = JsonSerializer.Serialize(value, _jsonOptions);
            return await _database.StringSetAsync(key, json, expiry, When.NotExists);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error setting value if not exists in Redis with key: {Key}", key);
            return false;
        }
    }

    public async Task<TimeSpan?> GetExpiryAsync(string key)
    {
        try
        {
            return await _database.KeyTimeToLiveAsync(key);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting expiry for Redis key: {Key}", key);
            return null;
        }
    }

    public async Task<bool> ExpireAsync(string key, TimeSpan expiry)
    {
        try
        {
            return await _database.KeyExpireAsync(key, expiry);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error setting expiry for Redis key: {Key}", key);
            return false;
        }
    }
}