namespace Rubia.Server.Services.Interfaces;

public interface IEventBusService
{
    void Subscribe<T>(Func<T, Task> handler) where T : class;
    Task PublishAsync<T>(T eventData) where T : class;
}