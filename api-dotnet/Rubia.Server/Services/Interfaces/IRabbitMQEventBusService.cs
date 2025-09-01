using Rubia.Server.Events;

namespace Rubia.Server.Services.Interfaces;

public interface IRabbitMQEventBusService
{
    Task PublishAsync<TEvent>(TEvent @event, CancellationToken cancellationToken = default) where TEvent : class, IEvent;
    Task PublishAsync<TEvent>(TEvent @event, string routingKey, CancellationToken cancellationToken = default) where TEvent : class, IEvent;
    
    void Subscribe<TEvent, THandler>() 
        where TEvent : class, IEvent 
        where THandler : class, IEventHandler<TEvent>;
    
    void Subscribe<TEvent>(Func<TEvent, CancellationToken, Task> handler) where TEvent : class, IEvent;
    
    Task StartAsync(CancellationToken cancellationToken = default);
    Task StopAsync(CancellationToken cancellationToken = default);
    
    void Dispose();
}