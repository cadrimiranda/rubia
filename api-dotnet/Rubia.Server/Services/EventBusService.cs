using Rubia.Server.Services.Interfaces;
using System.Collections.Concurrent;

namespace Rubia.Server.Services;

public class EventBusService : IEventBusService
{
    private readonly ConcurrentDictionary<Type, ConcurrentBag<Func<object, Task>>> _handlers = new();
    private readonly ILogger<EventBusService> _logger;

    public EventBusService(ILogger<EventBusService> logger)
    {
        _logger = logger;
    }

    public void Subscribe<T>(Func<T, Task> handler) where T : class
    {
        var eventType = typeof(T);
        var wrappedHandler = new Func<object, Task>(async (eventData) =>
        {
            if (eventData is T typedEvent)
            {
                await handler(typedEvent);
            }
        });

        _handlers.AddOrUpdate(eventType,
            new ConcurrentBag<Func<object, Task>> { wrappedHandler },
            (key, existingHandlers) =>
            {
                existingHandlers.Add(wrappedHandler);
                return existingHandlers;
            });

        _logger.LogDebug("Subscribed handler for event type: {EventType}", eventType.Name);
    }

    public async Task PublishAsync<T>(T eventData) where T : class
    {
        var eventType = typeof(T);
        
        if (!_handlers.TryGetValue(eventType, out var handlers))
        {
            _logger.LogDebug("No handlers registered for event type: {EventType}", eventType.Name);
            return;
        }

        var tasks = handlers.Select(handler => ExecuteHandlerSafely(handler, eventData, eventType.Name));
        await Task.WhenAll(tasks);

        _logger.LogDebug("Published event of type: {EventType}", eventType.Name);
    }

    private async Task ExecuteHandlerSafely(Func<object, Task> handler, object eventData, string eventTypeName)
    {
        try
        {
            await handler(eventData);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error executing handler for event type: {EventType}", eventTypeName);
        }
    }
}