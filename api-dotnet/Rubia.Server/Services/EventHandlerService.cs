using Rubia.Server.Events;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class EventHandlerService : IHostedService
{
    private readonly IEventBusService _eventBus;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<EventHandlerService> _logger;

    public EventHandlerService(
        IEventBusService eventBus, 
        IServiceProvider serviceProvider,
        ILogger<EventHandlerService> logger)
    {
        _eventBus = eventBus;
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        RegisterEventHandlers();
        _logger.LogInformation("Event handlers registered successfully");
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Event handler service stopped");
        return Task.CompletedTask;
    }

    private void RegisterEventHandlers()
    {
        // Register MessageCreatedEvent handler
        _eventBus.Subscribe<MessageCreatedEvent>(async messageEvent =>
        {
            using var scope = _serviceProvider.CreateScope();
            var notificationService = scope.ServiceProvider.GetRequiredService<IWebSocketNotificationService>();
            
            await notificationService.NotifyNewMessageAsync(
                messageEvent.CompanyId, 
                messageEvent.Message.ConversationId, 
                messageEvent.Message);
        });

        // Register ConversationStatusChangedEvent handler
        _eventBus.Subscribe<ConversationStatusChangedEvent>(async statusEvent =>
        {
            using var scope = _serviceProvider.CreateScope();
            var notificationService = scope.ServiceProvider.GetRequiredService<IWebSocketNotificationService>();
            
            await notificationService.NotifyConversationStatusChangeAsync(
                statusEvent.CompanyId, 
                statusEvent.Conversation);
        });

        // Register MessageStatusUpdatedEvent handler
        _eventBus.Subscribe<MessageStatusUpdatedEvent>(async statusEvent =>
        {
            using var scope = _serviceProvider.CreateScope();
            var notificationService = scope.ServiceProvider.GetRequiredService<IWebSocketNotificationService>();
            
            await notificationService.NotifyMessageStatusUpdateAsync(
                statusEvent.ConversationId, 
                statusEvent.Message);
        });

        // Register UserAssignedEvent handler
        _eventBus.Subscribe<UserAssignedEvent>(async assignmentEvent =>
        {
            using var scope = _serviceProvider.CreateScope();
            var notificationService = scope.ServiceProvider.GetRequiredService<IWebSocketNotificationService>();
            
            await notificationService.NotifyUserAssignmentAsync(
                assignmentEvent.CompanyId,
                assignmentEvent.ConversationId,
                assignmentEvent.UserId,
                assignmentEvent.UserName);
        });
    }
}