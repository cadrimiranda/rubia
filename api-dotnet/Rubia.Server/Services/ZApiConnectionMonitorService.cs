using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Integrations.Adapters;
using Rubia.Server.Services.Interfaces;
using System.Collections.Concurrent;

namespace Rubia.Server.Services;

public class ZApiConnectionMonitorService : IZApiConnectionMonitorService, IDisposable
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<ZApiConnectionMonitorService> _logger;
    private readonly IWebSocketNotificationService _notificationService;
    private readonly ConcurrentDictionary<Guid, WhatsAppInstanceStatus> _instanceStatuses;
    private readonly Timer _monitoringTimer;
    private readonly SemaphoreSlim _monitoringSemaphore;
    private bool _isMonitoring;
    private bool _disposed;

    public event EventHandler<InstanceStatusChangedEventArgs>? InstanceStatusChanged;
    public event EventHandler<QrCodeUpdatedEventArgs>? QrCodeUpdated;

    public ZApiConnectionMonitorService(
        IServiceProvider serviceProvider,
        ILogger<ZApiConnectionMonitorService> logger,
        IWebSocketNotificationService notificationService)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
        _notificationService = notificationService;
        _instanceStatuses = new ConcurrentDictionary<Guid, WhatsAppInstanceStatus>();
        _monitoringSemaphore = new SemaphoreSlim(1, 1);
        
        // Monitor every 30 seconds
        _monitoringTimer = new Timer(MonitorInstancesCallback, null, Timeout.Infinite, TimeSpan.FromSeconds(30));
    }

    public async Task StartMonitoringAsync(CancellationToken cancellationToken = default)
    {
        await _monitoringSemaphore.WaitAsync(cancellationToken);
        try
        {
            if (_isMonitoring)
            {
                _logger.LogWarning("Connection monitoring is already running");
                return;
            }

            _logger.LogInformation("Starting WhatsApp connection monitoring...");
            
            // Load initial instances
            await RefreshInstancesAsync(cancellationToken);
            
            // Start timer
            _monitoringTimer.Change(TimeSpan.Zero, TimeSpan.FromSeconds(30));
            _isMonitoring = true;
            
            _logger.LogInformation("WhatsApp connection monitoring started successfully");
        }
        finally
        {
            _monitoringSemaphore.Release();
        }
    }

    public async Task StopMonitoringAsync()
    {
        await _monitoringSemaphore.WaitAsync();
        try
        {
            if (!_isMonitoring)
            {
                return;
            }

            _logger.LogInformation("Stopping WhatsApp connection monitoring...");
            
            _monitoringTimer.Change(Timeout.Infinite, Timeout.Infinite);
            _isMonitoring = false;
            
            _logger.LogInformation("WhatsApp connection monitoring stopped");
        }
        finally
        {
            _monitoringSemaphore.Release();
        }
    }

    public async Task<ConnectionStatus> GetConnectionStatusAsync(Guid instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            var instance = await context.WhatsAppInstances
                .FirstOrDefaultAsync(i => i.Id == instanceId && i.IsActive, cancellationToken);

            if (instance == null)
            {
                _logger.LogWarning("WhatsApp instance not found: {InstanceId}", instanceId);
                return ConnectionStatus.Disconnected;
            }

            var adapter = scope.ServiceProvider.GetRequiredService<ZApiAdapter>();
            var status = await adapter.GetConnectionStatusAsync(instance.ExternalInstanceId, cancellationToken);
            
            // Update cached status
            _instanceStatuses.AddOrUpdate(instanceId, 
                new WhatsAppInstanceStatus 
                { 
                    InstanceId = instanceId,
                    ExternalInstanceId = instance.ExternalInstanceId,
                    InstanceName = instance.InstanceName,
                    Status = status,
                    LastChecked = DateTime.UtcNow,
                    IsActive = instance.IsActive,
                    CompanyId = instance.CompanyId
                },
                (key, existing) =>
                {
                    var oldStatus = existing.Status;
                    existing.Status = status;
                    existing.LastChecked = DateTime.UtcNow;
                    
                    // Raise event if status changed
                    if (oldStatus != status)
                    {
                        OnInstanceStatusChanged(new InstanceStatusChangedEventArgs
                        {
                            InstanceId = instanceId,
                            OldStatus = oldStatus,
                            NewStatus = status
                        });
                    }
                    
                    return existing;
                });

            return status;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting connection status for instance {InstanceId}", instanceId);
            return ConnectionStatus.Error;
        }
    }

    public async Task<QrCodeResult?> GetQrCodeAsync(Guid instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            var instance = await context.WhatsAppInstances
                .FirstOrDefaultAsync(i => i.Id == instanceId && i.IsActive, cancellationToken);

            if (instance == null)
            {
                return null;
            }

            var adapter = scope.ServiceProvider.GetRequiredService<ZApiAdapter>();
            var qrCode = await adapter.GetQrCodeAsync(instance.ExternalInstanceId, cancellationToken);
            
            if (qrCode != null)
            {
                // Update cached QR code
                if (_instanceStatuses.TryGetValue(instanceId, out var status))
                {
                    status.QrCode = qrCode;
                    OnQrCodeUpdated(new QrCodeUpdatedEventArgs { InstanceId = instanceId, QrCode = qrCode });
                }
            }
            
            return qrCode;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting QR code for instance {InstanceId}", instanceId);
            return null;
        }
    }

    public async Task<bool> DisconnectInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            var instance = await context.WhatsAppInstances
                .FirstOrDefaultAsync(i => i.Id == instanceId, cancellationToken);

            if (instance == null)
            {
                return false;
            }

            var adapter = scope.ServiceProvider.GetRequiredService<ZApiAdapter>();
            var success = await adapter.DisconnectInstanceAsync(instance.ExternalInstanceId, cancellationToken);
            
            if (success)
            {
                // Update status
                await GetConnectionStatusAsync(instanceId, cancellationToken);
                _logger.LogInformation("Instance {InstanceId} disconnected successfully", instanceId);
            }
            
            return success;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error disconnecting instance {InstanceId}", instanceId);
            return false;
        }
    }

    public async Task<bool> ReconnectInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            // First disconnect
            await DisconnectInstanceAsync(instanceId, cancellationToken);
            
            // Wait a moment
            await Task.Delay(2000, cancellationToken);
            
            // Check status (this will trigger reconnection process)
            var status = await GetConnectionStatusAsync(instanceId, cancellationToken);
            
            _logger.LogInformation("Reconnection initiated for instance {InstanceId}, status: {Status}", 
                instanceId, status);
            
            return status != ConnectionStatus.Error;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error reconnecting instance {InstanceId}", instanceId);
            return false;
        }
    }

    public async Task<IEnumerable<WhatsAppInstanceStatus>> GetAllInstancesStatusAsync(CancellationToken cancellationToken = default)
    {
        await RefreshInstancesAsync(cancellationToken);
        return _instanceStatuses.Values.ToList();
    }

    public async Task<bool> ValidateInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            var instance = await context.WhatsAppInstances
                .FirstOrDefaultAsync(i => i.Id == instanceId, cancellationToken);

            if (instance == null || !instance.IsActive)
            {
                return false;
            }

            var adapter = scope.ServiceProvider.GetRequiredService<ZApiAdapter>();
            return await adapter.IsAvailableAsync(cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error validating instance {InstanceId}", instanceId);
            return false;
        }
    }

    // Private methods
    private async void MonitorInstancesCallback(object? state)
    {
        if (!_isMonitoring || _disposed)
            return;

        try
        {
            await RefreshInstancesAsync(CancellationToken.None);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error during monitoring cycle");
        }
    }

    private async Task RefreshInstancesAsync(CancellationToken cancellationToken)
    {
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            
            var instances = await context.WhatsAppInstances
                .Where(i => i.IsActive)
                .Select(i => new { i.Id, i.ExternalInstanceId, i.InstanceName, i.CompanyId, i.IsActive })
                .ToListAsync(cancellationToken);

            var tasks = instances.Select(async instance =>
            {
                try
                {
                    var adapter = scope.ServiceProvider.GetRequiredService<ZApiAdapter>();
                    var status = await adapter.GetConnectionStatusAsync(instance.ExternalInstanceId, cancellationToken);
                    
                    var currentStatus = _instanceStatuses.GetValueOrDefault(instance.Id);
                    var oldStatus = currentStatus?.Status ?? ConnectionStatus.Disconnected;

                    var instanceStatus = new WhatsAppInstanceStatus
                    {
                        InstanceId = instance.Id,
                        ExternalInstanceId = instance.ExternalInstanceId,
                        InstanceName = instance.InstanceName,
                        Status = status,
                        LastChecked = DateTime.UtcNow,
                        IsActive = instance.IsActive,
                        CompanyId = instance.CompanyId,
                        QrCode = currentStatus?.QrCode
                    };

                    _instanceStatuses.AddOrUpdate(instance.Id, instanceStatus, (key, existing) =>
                    {
                        existing.Status = status;
                        existing.LastChecked = DateTime.UtcNow;
                        return existing;
                    });

                    // Raise event if status changed
                    if (oldStatus != status)
                    {
                        OnInstanceStatusChanged(new InstanceStatusChangedEventArgs
                        {
                            InstanceId = instance.Id,
                            OldStatus = oldStatus,
                            NewStatus = status
                        });

                        // Notify via WebSocket
                        await _notificationService.NotifyInstanceStatusChangedAsync(instance.Id, status);
                    }

                    // If status is QrCodeRequired, try to get QR code
                    if (status == ConnectionStatus.QrCodeRequired)
                    {
                        var qrCode = await adapter.GetQrCodeAsync(instance.ExternalInstanceId, cancellationToken);
                        if (qrCode != null)
                        {
                            instanceStatus.QrCode = qrCode;
                            OnQrCodeUpdated(new QrCodeUpdatedEventArgs { InstanceId = instance.Id, QrCode = qrCode });
                            await _notificationService.NotifyQrCodeUpdatedAsync(instance.Id, qrCode);
                        }
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error monitoring instance {InstanceId}", instance.Id);
                    
                    _instanceStatuses.AddOrUpdate(instance.Id, 
                        new WhatsAppInstanceStatus 
                        { 
                            InstanceId = instance.Id,
                            Status = ConnectionStatus.Error, 
                            ErrorMessage = ex.Message,
                            LastChecked = DateTime.UtcNow
                        },
                        (key, existing) =>
                        {
                            existing.Status = ConnectionStatus.Error;
                            existing.ErrorMessage = ex.Message;
                            existing.LastChecked = DateTime.UtcNow;
                            return existing;
                        });
                }
            });

            await Task.WhenAll(tasks);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error refreshing instance statuses");
        }
    }

    private void OnInstanceStatusChanged(InstanceStatusChangedEventArgs args)
    {
        try
        {
            InstanceStatusChanged?.Invoke(this, args);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error raising InstanceStatusChanged event");
        }
    }

    private void OnQrCodeUpdated(QrCodeUpdatedEventArgs args)
    {
        try
        {
            QrCodeUpdated?.Invoke(this, args);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error raising QrCodeUpdated event");
        }
    }

    public void Dispose()
    {
        if (_disposed)
            return;

        _disposed = true;
        
        _monitoringTimer?.Dispose();
        _monitoringSemaphore?.Dispose();
        
        GC.SuppressFinalize(this);
    }
}