using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using Rubia.Server.Events;
using Rubia.Server.Services.Interfaces;
using System.Collections.Concurrent;
using System.Text;
using System.Text.Json;

namespace Rubia.Server.Services;

public class RabbitMQEventBusService : IRabbitMQEventBusService, IDisposable
{
    private readonly ILogger<RabbitMQEventBusService> _logger;
    private readonly IServiceProvider _serviceProvider;
    private readonly IConfiguration _configuration;
    private readonly ConcurrentDictionary<Type, List<Type>> _subscriptions = new();
    private readonly ConcurrentDictionary<Type, List<Func<object, CancellationToken, Task>>> _handlers = new();
    
    private IConnection? _connection;
    private IModel? _channel;
    private readonly string _exchangeName;
    private readonly string _queueName;
    private bool _disposed;

    public RabbitMQEventBusService(
        ILogger<RabbitMQEventBusService> logger,
        IServiceProvider serviceProvider,
        IConfiguration configuration)
    {
        _logger = logger;
        _serviceProvider = serviceProvider;
        _configuration = configuration;
        _exchangeName = configuration.GetValue<string>("RabbitMQ:ExchangeName") ?? "rubia.events";
        _queueName = configuration.GetValue<string>("RabbitMQ:QueueName") ?? "rubia.queue";
    }

    public async Task PublishAsync<TEvent>(TEvent @event, CancellationToken cancellationToken = default) 
        where TEvent : class, IEvent
    {
        var routingKey = typeof(TEvent).Name.ToLowerInvariant();
        await PublishAsync(@event, routingKey, cancellationToken);
    }

    public async Task PublishAsync<TEvent>(TEvent @event, string routingKey, CancellationToken cancellationToken = default) 
        where TEvent : class, IEvent
    {
        if (_disposed) throw new ObjectDisposedException(nameof(RabbitMQEventBusService));

        try
        {
            await EnsureConnectionAsync();
            
            var message = JsonSerializer.Serialize(@event, new JsonSerializerOptions 
            { 
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase 
            });
            
            var body = Encoding.UTF8.GetBytes(message);

            var properties = _channel!.CreateBasicProperties();
            properties.Persistent = true;
            properties.MessageId = Guid.NewGuid().ToString();
            properties.Timestamp = new AmqpTimestamp(DateTimeOffset.UtcNow.ToUnixTimeSeconds());
            properties.Type = typeof(TEvent).Name;

            _channel.BasicPublish(
                exchange: _exchangeName,
                routingKey: routingKey,
                basicProperties: properties,
                body: body);

            _logger.LogDebug("Published event {EventType} with routing key {RoutingKey}", 
                typeof(TEvent).Name, routingKey);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish event {EventType}", typeof(TEvent).Name);
            throw;
        }
    }

    public void Subscribe<TEvent, THandler>() 
        where TEvent : class, IEvent 
        where THandler : class, IEventHandler<TEvent>
    {
        var eventType = typeof(TEvent);
        var handlerType = typeof(THandler);

        if (!_subscriptions.ContainsKey(eventType))
        {
            _subscriptions[eventType] = new List<Type>();
        }

        _subscriptions[eventType].Add(handlerType);
        
        _logger.LogInformation("Subscribed {HandlerType} to {EventType}", 
            handlerType.Name, eventType.Name);
    }

    public void Subscribe<TEvent>(Func<TEvent, CancellationToken, Task> handler) where TEvent : class, IEvent
    {
        var eventType = typeof(TEvent);

        if (!_handlers.ContainsKey(eventType))
        {
            _handlers[eventType] = new List<Func<object, CancellationToken, Task>>();
        }

        _handlers[eventType].Add(async (evt, ct) => await handler((TEvent)evt, ct));
        
        _logger.LogInformation("Subscribed lambda handler to {EventType}", eventType.Name);
    }

    public async Task StartAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            await EnsureConnectionAsync();
            SetupConsumer();
            _logger.LogInformation("RabbitMQ Event Bus started successfully");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to start RabbitMQ Event Bus");
            throw;
        }
    }

    public async Task StopAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            _channel?.Close();
            _connection?.Close();
            _logger.LogInformation("RabbitMQ Event Bus stopped successfully");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error stopping RabbitMQ Event Bus");
        }
        
        await Task.CompletedTask;
    }

    private async Task EnsureConnectionAsync()
    {
        if (_connection?.IsOpen == true && _channel?.IsOpen == true)
            return;

        var hostName = _configuration.GetValue<string>("RabbitMQ:HostName") ?? "localhost";
        var port = _configuration.GetValue<int>("RabbitMQ:Port", 5672);
        var userName = _configuration.GetValue<string>("RabbitMQ:UserName") ?? "guest";
        var password = _configuration.GetValue<string>("RabbitMQ:Password") ?? "guest";
        var virtualHost = _configuration.GetValue<string>("RabbitMQ:VirtualHost") ?? "/";

        var factory = new ConnectionFactory
        {
            HostName = hostName,
            Port = port,
            UserName = userName,
            Password = password,
            VirtualHost = virtualHost,
            AutomaticRecoveryEnabled = true,
            NetworkRecoveryInterval = TimeSpan.FromSeconds(10),
            RequestedHeartbeat = TimeSpan.FromSeconds(60)
        };

        _connection = factory.CreateConnection("Rubia.EventBus");
        _channel = _connection.CreateModel();

        // Declare exchange
        _channel.ExchangeDeclare(_exchangeName, ExchangeType.Topic, durable: true);

        // Declare queue
        _channel.QueueDeclare(_queueName, durable: true, exclusive: false, autoDelete: false);

        // Bind queue to exchange with wildcard routing key
        _channel.QueueBind(_queueName, _exchangeName, "*");

        await Task.CompletedTask;
    }

    private void SetupConsumer()
    {
        if (_channel == null) return;

        var consumer = new EventingBasicConsumer(_channel);
        
        consumer.Received += async (model, ea) =>
        {
            try
            {
                var body = ea.Body.ToArray();
                var message = Encoding.UTF8.GetString(body);
                var eventTypeName = ea.BasicProperties?.Type;

                if (string.IsNullOrEmpty(eventTypeName))
                {
                    _logger.LogWarning("Received message without event type");
                    _channel.BasicAck(ea.DeliveryTag, false);
                    return;
                }

                // Find event type
                var eventType = AppDomain.CurrentDomain.GetAssemblies()
                    .SelectMany(a => a.GetTypes())
                    .FirstOrDefault(t => t.Name == eventTypeName && typeof(IEvent).IsAssignableFrom(t));

                if (eventType == null)
                {
                    _logger.LogWarning("Unknown event type: {EventType}", eventTypeName);
                    _channel.BasicAck(ea.DeliveryTag, false);
                    return;
                }

                // Deserialize event
                var eventData = JsonSerializer.Deserialize(message, eventType, new JsonSerializerOptions 
                { 
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase 
                });

                if (eventData == null)
                {
                    _logger.LogWarning("Failed to deserialize event {EventType}", eventTypeName);
                    _channel.BasicAck(ea.DeliveryTag, false);
                    return;
                }

                // Handle subscriptions
                await HandleSubscriptionsAsync(eventType, eventData);

                // Handle lambda handlers
                await HandleLambdaHandlersAsync(eventType, eventData);

                _channel.BasicAck(ea.DeliveryTag, false);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error processing received message");
                _channel.BasicNack(ea.DeliveryTag, false, false);
            }
        };

        _channel.BasicConsume(_queueName, autoAck: false, consumer);
    }

    private async Task HandleSubscriptionsAsync(Type eventType, object eventData)
    {
        if (!_subscriptions.TryGetValue(eventType, out var handlerTypes))
            return;

        using var scope = _serviceProvider.CreateScope();

        foreach (var handlerType in handlerTypes)
        {
            try
            {
                var handler = scope.ServiceProvider.GetService(handlerType);
                if (handler == null)
                {
                    _logger.LogWarning("Handler {HandlerType} not registered in DI container", handlerType.Name);
                    continue;
                }

                var handleMethod = handlerType.GetMethod("HandleAsync");
                if (handleMethod != null)
                {
                    var result = handleMethod.Invoke(handler, new[] { eventData, CancellationToken.None });
                    if (result is Task task)
                    {
                        await task;
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error executing handler {HandlerType} for event {EventType}", 
                    handlerType.Name, eventType.Name);
            }
        }
    }

    private async Task HandleLambdaHandlersAsync(Type eventType, object eventData)
    {
        if (!_handlers.TryGetValue(eventType, out var handlerFunctions))
            return;

        foreach (var handlerFunction in handlerFunctions)
        {
            try
            {
                await handlerFunction(eventData, CancellationToken.None);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error executing lambda handler for event {EventType}", eventType.Name);
            }
        }
    }

    public void Dispose()
    {
        if (_disposed) return;

        try
        {
            _channel?.Close();
            _channel?.Dispose();
            _connection?.Close();
            _connection?.Dispose();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error disposing RabbitMQ Event Bus");
        }
        finally
        {
            _disposed = true;
        }
    }
}