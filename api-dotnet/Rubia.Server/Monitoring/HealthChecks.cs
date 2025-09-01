using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using StackExchange.Redis;

namespace Rubia.Server.Monitoring;

public class DatabaseHealthCheck : IHealthCheck
{
    private readonly RubiaDbContext _context;

    public DatabaseHealthCheck(RubiaDbContext context)
    {
        _context = context;
    }

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            await _context.Database.ExecuteSqlRawAsync("SELECT 1", cancellationToken);
            return HealthCheckResult.Healthy("Database connection is healthy");
        }
        catch (Exception ex)
        {
            return HealthCheckResult.Unhealthy("Database connection failed", ex);
        }
    }
}

public class RedisHealthCheck : IHealthCheck
{
    private readonly IConnectionMultiplexer _connectionMultiplexer;

    public RedisHealthCheck(IConnectionMultiplexer connectionMultiplexer)
    {
        _connectionMultiplexer = connectionMultiplexer;
    }

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            var database = _connectionMultiplexer.GetDatabase();
            await database.PingAsync();
            return HealthCheckResult.Healthy("Redis connection is healthy");
        }
        catch (Exception ex)
        {
            return HealthCheckResult.Unhealthy("Redis connection failed", ex);
        }
    }
}

public class WhatsAppHealthCheck : IHealthCheck
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<WhatsAppHealthCheck> _logger;

    public WhatsAppHealthCheck(RubiaDbContext context, ILogger<WhatsAppHealthCheck> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            var activeInstances = await _context.WhatsAppInstances
                .Where(w => w.IsActive && w.Status == "CONNECTED")
                .CountAsync(cancellationToken);

            var totalInstances = await _context.WhatsAppInstances
                .Where(w => w.IsActive)
                .CountAsync(cancellationToken);

            if (totalInstances == 0)
            {
                return HealthCheckResult.Degraded("No WhatsApp instances configured");
            }

            if (activeInstances == 0)
            {
                return HealthCheckResult.Unhealthy("No WhatsApp instances are connected");
            }

            var healthyPercentage = (double)activeInstances / totalInstances;
            
            if (healthyPercentage >= 0.8) // 80% or more instances are healthy
            {
                return HealthCheckResult.Healthy($"{activeInstances}/{totalInstances} WhatsApp instances are connected");
            }
            else if (healthyPercentage >= 0.5) // 50% or more instances are healthy
            {
                return HealthCheckResult.Degraded($"Only {activeInstances}/{totalInstances} WhatsApp instances are connected");
            }
            else
            {
                return HealthCheckResult.Unhealthy($"Only {activeInstances}/{totalInstances} WhatsApp instances are connected");
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking WhatsApp health");
            return HealthCheckResult.Unhealthy("WhatsApp health check failed", ex);
        }
    }
}

public class OpenAIHealthCheck : IHealthCheck
{
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<OpenAIHealthCheck> _logger;

    public OpenAIHealthCheck(HttpClient httpClient, IConfiguration configuration, ILogger<OpenAIHealthCheck> logger)
    {
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
    }

    public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            var apiKey = _configuration["OPENAI_API_KEY"];
            if (string.IsNullOrEmpty(apiKey))
            {
                return HealthCheckResult.Degraded("OpenAI API key not configured");
            }

            _httpClient.DefaultRequestHeaders.Clear();
            _httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");

            // Simple request to check if the API is accessible
            var response = await _httpClient.GetAsync("https://api.openai.com/v1/models", cancellationToken);
            
            if (response.IsSuccessStatusCode)
            {
                return HealthCheckResult.Healthy("OpenAI API is accessible");
            }
            else
            {
                return HealthCheckResult.Unhealthy($"OpenAI API returned status code: {response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking OpenAI health");
            return HealthCheckResult.Unhealthy("OpenAI API health check failed", ex);
        }
    }
}

public class SystemResourcesHealthCheck : IHealthCheck
{
    private readonly ILogger<SystemResourcesHealthCheck> _logger;

    public SystemResourcesHealthCheck(ILogger<SystemResourcesHealthCheck> logger)
    {
        _logger = logger;
    }

    public Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default)
    {
        try
        {
            var availableMemory = GC.GetTotalMemory(false);
            var workingSet = Environment.WorkingSet;
            var memoryUsagePercentage = (double)workingSet / (1024 * 1024 * 1024); // Convert to GB

            var data = new Dictionary<string, object>
            {
                { "WorkingSetMB", workingSet / (1024 * 1024) },
                { "AvailableMemoryMB", availableMemory / (1024 * 1024) },
                { "ProcessorCount", Environment.ProcessorCount },
                { "ThreadCount", System.Diagnostics.Process.GetCurrentProcess().Threads.Count }
            };

            if (memoryUsagePercentage > 2) // More than 2GB
            {
                return Task.FromResult(HealthCheckResult.Degraded("High memory usage detected", null, data));
            }

            return Task.FromResult(HealthCheckResult.Healthy("System resources are healthy", data));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking system resources health");
            return Task.FromResult(HealthCheckResult.Unhealthy("System resources health check failed", ex));
        }
    }
}