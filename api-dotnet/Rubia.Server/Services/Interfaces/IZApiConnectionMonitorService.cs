using Rubia.Server.DTOs;
using Rubia.Server.Integrations.Adapters;

namespace Rubia.Server.Services.Interfaces;

public interface IZApiConnectionMonitorService
{
    Task<ConnectionStatus> GetConnectionStatusAsync(Guid instanceId, CancellationToken cancellationToken = default);
    Task<QrCodeResult?> GetQrCodeAsync(Guid instanceId, CancellationToken cancellationToken = default);
    Task<bool> DisconnectInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default);
    Task<bool> ReconnectInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default);
    Task StartMonitoringAsync(CancellationToken cancellationToken = default);
    Task StopMonitoringAsync();
    Task<IEnumerable<WhatsAppInstanceStatus>> GetAllInstancesStatusAsync(CancellationToken cancellationToken = default);
    Task<bool> ValidateInstanceAsync(Guid instanceId, CancellationToken cancellationToken = default);
    
    // Events
    event EventHandler<InstanceStatusChangedEventArgs>? InstanceStatusChanged;
    event EventHandler<QrCodeUpdatedEventArgs>? QrCodeUpdated;
}

public class WhatsAppInstanceStatus
{
    public Guid InstanceId { get; set; }
    public string ExternalInstanceId { get; set; } = string.Empty;
    public string InstanceName { get; set; } = string.Empty;
    public ConnectionStatus Status { get; set; }
    public DateTime LastChecked { get; set; }
    public string? PhoneNumber { get; set; }
    public QrCodeResult? QrCode { get; set; }
    public string? ErrorMessage { get; set; }
    public bool IsActive { get; set; }
    public Guid CompanyId { get; set; }
}

public class InstanceStatusChangedEventArgs : EventArgs
{
    public Guid InstanceId { get; set; }
    public ConnectionStatus OldStatus { get; set; }
    public ConnectionStatus NewStatus { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    public string? ErrorMessage { get; set; }
}

public class QrCodeUpdatedEventArgs : EventArgs
{
    public Guid InstanceId { get; set; }
    public QrCodeResult? QrCode { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}