using Serilog;
using Serilog.Events;

namespace Rubia.Server.Logging;

public static class LoggingConfiguration
{
    public static void ConfigureSerilog(WebApplicationBuilder builder)
    {
        var configuration = builder.Configuration;
        var environment = builder.Environment;

        Log.Logger = new LoggerConfiguration()
            .ReadFrom.Configuration(configuration)
            .Enrich.FromLogContext()
            .Enrich.WithProperty("Application", "Rubia.Server")
            .Enrich.WithProperty("Environment", environment.EnvironmentName)
            .Enrich.WithMachineName()
            .Enrich.WithThreadId()
            .CreateLogger();

        builder.Host.UseSerilog();
    }

    public static LoggerConfiguration GetBaseConfiguration(IConfiguration configuration, string environmentName)
    {
        var loggerConfig = new LoggerConfiguration()
            .ReadFrom.Configuration(configuration)
            .Enrich.FromLogContext()
            .Enrich.WithProperty("Application", "Rubia.Server")
            .Enrich.WithProperty("Environment", environmentName)
            .Enrich.WithMachineName()
            .Enrich.WithThreadId()
            .Enrich.WithCorrelationId();

        // Console logging
        loggerConfig.WriteTo.Console(
            outputTemplate: "[{Timestamp:HH:mm:ss} {Level:u3}] {SourceContext}: {Message:lj}{NewLine}{Exception}");

        // File logging
        var logsPath = configuration.GetValue<string>("Logging:FilePath") ?? "logs/rubia-.txt";
        loggerConfig.WriteTo.File(
            path: logsPath,
            rollingInterval: RollingInterval.Day,
            retainedFileCountLimit: 30,
            shared: true,
            outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss.fff zzz} [{Level:u3}] {SourceContext}: {Message:lj}{NewLine}{Exception}");

        // Different log levels for different environments
        if (environmentName.Equals("Development", StringComparison.OrdinalIgnoreCase))
        {
            loggerConfig.MinimumLevel.Debug()
                .MinimumLevel.Override("Microsoft", LogEventLevel.Information)
                .MinimumLevel.Override("Microsoft.AspNetCore", LogEventLevel.Warning)
                .MinimumLevel.Override("System", LogEventLevel.Warning);
        }
        else if (environmentName.Equals("Production", StringComparison.OrdinalIgnoreCase))
        {
            loggerConfig.MinimumLevel.Information()
                .MinimumLevel.Override("Microsoft", LogEventLevel.Warning)
                .MinimumLevel.Override("Microsoft.AspNetCore", LogEventLevel.Error)
                .MinimumLevel.Override("System", LogEventLevel.Error);
        }
        else
        {
            loggerConfig.MinimumLevel.Information()
                .MinimumLevel.Override("Microsoft", LogEventLevel.Information)
                .MinimumLevel.Override("Microsoft.AspNetCore", LogEventLevel.Warning)
                .MinimumLevel.Override("System", LogEventLevel.Warning);
        }

        // Structured logging filters for sensitive data
        loggerConfig.Filter.ByExcluding(logEvent =>
        {
            var message = logEvent.MessageTemplate.Text;
            return message.Contains("password", StringComparison.OrdinalIgnoreCase) ||
                   message.Contains("token", StringComparison.OrdinalIgnoreCase) ||
                   message.Contains("secret", StringComparison.OrdinalIgnoreCase);
        });

        return loggerConfig;
    }

    public static void AddCustomEnrichers(LoggerConfiguration loggerConfig)
    {
        loggerConfig.Enrich.With<RequestIdEnricher>();
        loggerConfig.Enrich.With<UserIdEnricher>();
        loggerConfig.Enrich.With<CompanyIdEnricher>();
    }
}

public class RequestIdEnricher : ILogEventEnricher
{
    public void Enrich(LogEvent logEvent, ILogEventPropertyFactory propertyFactory)
    {
        var httpContext = GetHttpContext();
        if (httpContext != null)
        {
            var requestId = httpContext.TraceIdentifier;
            logEvent.AddPropertyIfAbsent(propertyFactory.CreateProperty("RequestId", requestId));
        }
    }

    private static HttpContext? GetHttpContext()
    {
        // This would need to be properly injected in a real implementation
        return null; // Placeholder
    }
}

public class UserIdEnricher : ILogEventEnricher
{
    public void Enrich(LogEvent logEvent, ILogEventPropertyFactory propertyFactory)
    {
        var httpContext = GetHttpContext();
        if (httpContext?.User?.Identity?.IsAuthenticated == true)
        {
            var userId = httpContext.User.FindFirst("sub")?.Value ?? httpContext.User.FindFirst("id")?.Value;
            if (!string.IsNullOrEmpty(userId))
            {
                logEvent.AddPropertyIfAbsent(propertyFactory.CreateProperty("UserId", userId));
            }
        }
    }

    private static HttpContext? GetHttpContext()
    {
        // This would need to be properly injected in a real implementation
        return null; // Placeholder
    }
}

public class CompanyIdEnricher : ILogEventEnricher
{
    public void Enrich(LogEvent logEvent, ILogEventPropertyFactory propertyFactory)
    {
        var httpContext = GetHttpContext();
        if (httpContext?.User?.Identity?.IsAuthenticated == true)
        {
            var companyId = httpContext.User.FindFirst("company_id")?.Value;
            if (!string.IsNullOrEmpty(companyId))
            {
                logEvent.AddPropertyIfAbsent(propertyFactory.CreateProperty("CompanyId", companyId));
            }
        }
    }

    private static HttpContext? GetHttpContext()
    {
        // This would need to be properly injected in a real implementation
        return null; // Placeholder
    }
}