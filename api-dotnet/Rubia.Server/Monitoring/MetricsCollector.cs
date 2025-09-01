using System.Diagnostics.Metrics;
using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Monitoring;

public class MetricsCollector : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<MetricsCollector> _logger;
    private readonly Meter _meter;
    
    // Counters
    private readonly Counter<long> _messagesReceivedCounter;
    private readonly Counter<long> _messagesSentCounter;
    private readonly Counter<long> _campaignMessagesCounter;
    private readonly Counter<long> _aiGeneratedMessagesCounter;
    private readonly Counter<long> _whatsappConnectionsCounter;
    
    // Gauges
    private readonly ObservableGauge<int> _activeConversationsGauge;
    private readonly ObservableGauge<int> _activeUsersGauge;
    private readonly ObservableGauge<int> _connectedWhatsAppInstancesGauge;
    private readonly ObservableGauge<long> _databaseConnectionsGauge;
    private readonly ObservableGauge<double> _memoryUsageGauge;

    public MetricsCollector(IServiceProvider serviceProvider, ILogger<MetricsCollector> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
        _meter = new Meter("Rubia.Server");

        // Initialize counters
        _messagesReceivedCounter = _meter.CreateCounter<long>("rubia_messages_received_total", "messages", "Total number of messages received");
        _messagesSentCounter = _meter.CreateCounter<long>("rubia_messages_sent_total", "messages", "Total number of messages sent");
        _campaignMessagesCounter = _meter.CreateCounter<long>("rubia_campaign_messages_total", "messages", "Total number of campaign messages sent");
        _aiGeneratedMessagesCounter = _meter.CreateCounter<long>("rubia_ai_messages_total", "messages", "Total number of AI-generated messages");
        _whatsappConnectionsCounter = _meter.CreateCounter<long>("rubia_whatsapp_connections_total", "connections", "Total number of WhatsApp connection events");

        // Initialize gauges
        _activeConversationsGauge = _meter.CreateObservableGauge("rubia_active_conversations", "conversations", "Number of active conversations");
        _activeUsersGauge = _meter.CreateObservableGauge("rubia_active_users", "users", "Number of active users");
        _connectedWhatsAppInstancesGauge = _meter.CreateObservableGauge("rubia_whatsapp_connected_instances", "instances", "Number of connected WhatsApp instances");
        _databaseConnectionsGauge = _meter.CreateObservableGauge("rubia_database_connections", "connections", "Number of active database connections");
        _memoryUsageGauge = _meter.CreateObservableGauge("rubia_memory_usage_mb", "megabytes", "Memory usage in megabytes");
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("Metrics collector started");

        // Subscribe to observable gauges
        _activeConversationsGauge.Callback = () => new[]
        {
            new Measurement<int>(GetActiveConversationsCount(), Array.Empty<KeyValuePair<string, object?>>())
        };

        _activeUsersGauge.Callback = () => new[]
        {
            new Measurement<int>(GetActiveUsersCount(), Array.Empty<KeyValuePair<string, object?>>())
        };

        _connectedWhatsAppInstancesGauge.Callback = () => new[]
        {
            new Measurement<int>(GetConnectedWhatsAppInstancesCount(), Array.Empty<KeyValuePair<string, object?>>())
        };

        _databaseConnectionsGauge.Callback = () => new[]
        {
            new Measurement<long>(GetDatabaseConnectionsCount(), Array.Empty<KeyValuePair<string, object?>>())
        };

        _memoryUsageGauge.Callback = () => new[]
        {
            new Measurement<double>(GetMemoryUsageMB(), Array.Empty<KeyValuePair<string, object?>>())
        };

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await CollectApplicationMetricsAsync();
                await Task.Delay(TimeSpan.FromMinutes(1), stoppingToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error collecting metrics");
                await Task.Delay(TimeSpan.FromMinutes(1), stoppingToken);
            }
        }
    }

    private async Task CollectApplicationMetricsAsync()
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();

        try
        {
            // Collect message metrics from the last hour
            var oneHourAgo = DateTime.UtcNow.AddHours(-1);
            
            var messagesReceived = await context.Messages
                .Where(m => m.CreatedAt >= oneHourAgo && m.SenderType != "USER")
                .CountAsync();

            var messagesSent = await context.Messages
                .Where(m => m.CreatedAt >= oneHourAgo && m.SenderType == "USER")
                .CountAsync();

            var campaignMessages = await context.Messages
                .Where(m => m.CreatedAt >= oneHourAgo && m.CampaignContactId != null)
                .CountAsync();

            var aiMessages = await context.Messages
                .Where(m => m.CreatedAt >= oneHourAgo && m.IsAiGenerated == true)
                .CountAsync();

            // Update counters
            _messagesReceivedCounter.Add(messagesReceived);
            _messagesSentCounter.Add(messagesSent);
            _campaignMessagesCounter.Add(campaignMessages);
            _aiGeneratedMessagesCounter.Add(aiMessages);

            _logger.LogDebug("Collected metrics: Messages Received: {MessagesReceived}, Messages Sent: {MessagesSent}, Campaign Messages: {CampaignMessages}, AI Messages: {AiMessages}",
                messagesReceived, messagesSent, campaignMessages, aiMessages);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error collecting application metrics");
        }
    }

    private int GetActiveConversationsCount()
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            return context.Conversations
                .Where(c => c.Status == "OPEN" || c.Status == "WAITING")
                .Count();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting active conversations count");
            return 0;
        }
    }

    private int GetActiveUsersCount()
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            return context.Users
                .Where(u => u.IsActive)
                .Count();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting active users count");
            return 0;
        }
    }

    private int GetConnectedWhatsAppInstancesCount()
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            return context.WhatsAppInstances
                .Where(w => w.IsActive && w.Status == "CONNECTED")
                .Count();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting connected WhatsApp instances count");
            return 0;
        }
    }

    private long GetDatabaseConnectionsCount()
    {
        try
        {
            // This would need to be implemented based on your specific database monitoring needs
            // For PostgreSQL, you might query pg_stat_activity
            return 0; // Placeholder
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting database connections count");
            return 0;
        }
    }

    private double GetMemoryUsageMB()
    {
        try
        {
            var workingSet = Environment.WorkingSet;
            return workingSet / (1024.0 * 1024.0); // Convert bytes to MB
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting memory usage");
            return 0;
        }
    }

    public void RecordMessageReceived(string channel = "whatsapp", string messageType = "text")
    {
        _messagesReceivedCounter.Add(1, new KeyValuePair<string, object?>("channel", channel), new KeyValuePair<string, object?>("type", messageType));
    }

    public void RecordMessageSent(string channel = "whatsapp", string messageType = "text", bool isAi = false)
    {
        _messagesSentCounter.Add(1, new KeyValuePair<string, object?>("channel", channel), new KeyValuePair<string, object?>("type", messageType));
        
        if (isAi)
        {
            _aiGeneratedMessagesCounter.Add(1);
        }
    }

    public void RecordCampaignMessage(string status = "sent")
    {
        _campaignMessagesCounter.Add(1, new KeyValuePair<string, object?>("status", status));
    }

    public void RecordWhatsAppConnection(string status = "connected")
    {
        _whatsappConnectionsCounter.Add(1, new KeyValuePair<string, object?>("status", status));
    }

    public override void Dispose()
    {
        _meter?.Dispose();
        base.Dispose();
    }
}