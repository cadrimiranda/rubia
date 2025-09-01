using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Rubia.Server.Data;
using Rubia.Server.Hubs;
using Rubia.Server.Middleware;
using Rubia.Server.Services;
using Rubia.Server.Services.Interfaces;
using StackExchange.Redis;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();

// Add memory caching
builder.Services.AddMemoryCache();

// Configure Entity Framework with PostgreSQL
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection") ??
    $"Host={builder.Configuration["PGHOST"]};Database={builder.Configuration["PGDATABASE"]};Username={builder.Configuration["PGUSER"]};Password={builder.Configuration["PGPASSWORD"]};Port={builder.Configuration["PGPORT"] ?? "5432"}";

builder.Services.AddDbContext<RubiaDbContext>(options =>
    options.UseNpgsql(connectionString));

// Configure Redis
var redisConnectionString = builder.Configuration["REDIS_CLUSTER_NODES"];
if (!string.IsNullOrEmpty(redisConnectionString))
{
    var configOptions = new ConfigurationOptions();
    
    // Parse cluster nodes
    var nodes = redisConnectionString.Split(',');
    foreach (var node in nodes)
    {
        configOptions.EndPoints.Add(node.Trim());
    }
    
    // Configure cluster settings
    configOptions.Password = builder.Configuration["REDISPASSWORD"];
    configOptions.AbortOnConnectFail = false;
    configOptions.ConnectTimeout = 5000;
    configOptions.SyncTimeout = 3000;
    
    builder.Services.AddSingleton<IConnectionMultiplexer>(provider =>
        ConnectionMultiplexer.Connect(configOptions));
}
else
{
    // Fallback to single Redis instance
    var singleRedisConnection = builder.Configuration["REDISHOST"] != null
        ? $"{builder.Configuration["REDISHOST"]}:{builder.Configuration["REDISPORT"] ?? "6379"}"
        : "localhost:6379";
        
    builder.Services.AddSingleton<IConnectionMultiplexer>(provider =>
        ConnectionMultiplexer.Connect(singleRedisConnection));
}

// Register services
builder.Services.AddScoped<ICompanyGroupService, CompanyGroupService>();
builder.Services.AddScoped<ICompanyService, CompanyService>();
builder.Services.AddScoped<IDepartmentService, DepartmentService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<ICustomerService, CustomerService>();
builder.Services.AddScoped<IPhoneService, PhoneService>();
builder.Services.AddScoped<IAIModelService, AIModelService>();
builder.Services.AddScoped<IAIAgentService, AIAgentService>();
builder.Services.AddScoped<IMessageTemplateService, MessageTemplateService>();
builder.Services.AddScoped<IJwtService, JwtService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IConversationService, ConversationService>();
builder.Services.AddScoped<IMessageService, MessageService>();
builder.Services.AddScoped<IOpenAIService, OpenAIService>();
builder.Services.AddScoped<IMessagingService, MessagingService>();
builder.Services.AddScoped<IZApiConnectionMonitorService, ZApiConnectionMonitorService>();
builder.Services.AddScoped<IMessagingAdapter, TwilioAdapter>();
builder.Services.AddScoped<TwilioAdapter>();
builder.Services.AddScoped<ZApiAdapter>();
builder.Services.AddScoped<IWebSocketNotificationService, WebSocketNotificationService>();
builder.Services.AddScoped<IRedisCacheService, RedisCacheService>();
builder.Services.AddScoped<IWhatsAppService, WhatsAppService>();
builder.Services.AddScoped<ICampaignService, CampaignService>();
builder.Services.AddScoped<IFAQService, FAQService>();
builder.Services.AddScoped<IMessageDraftService, MessageDraftService>();
builder.Services.AddScoped<IUnreadMessageCountService, UnreadMessageCountService>();
builder.Services.AddScoped<IConversationMediaService, ConversationMediaService>();
builder.Services.AddScoped<IAudioMessageRepository, AudioMessageRepository>();
builder.Services.Configure<AudioStorageOptions>(builder.Configuration.GetSection("AudioStorage"));
builder.Services.AddScoped<IAudioStorageService, AudioStorageService>();
builder.Services.AddScoped<IAudioProcessingService, AudioProcessingService>();
builder.Services.AddScoped<IAILogService, AILogService>();
builder.Services.AddScoped<ITemplateEnhancementService, TemplateEnhancementService>();
builder.Services.AddScoped<IMessageTemplateRevisionService, MessageTemplateRevisionService>();

// Campaign System Services (Phase 2.1)
builder.Services.AddScoped<ICampaignProcessingService, CampaignProcessingService>();
builder.Services.AddScoped<ICampaignMessagingService, CampaignMessagingService>();
builder.Services.AddScoped<ICampaignContactService, CampaignContactService>();

// Collaboration System Services (Phase 2.2)
builder.Services.AddScoped<IConversationParticipantService, ConversationParticipantService>();
builder.Services.AddScoped<IUserAIAgentService, UserAIAgentService>();

// Event Bus Services (Phase 2.3)
builder.Services.AddScoped<IRabbitMQEventBusService, RabbitMQEventBusService>();

// Donation Appointment Services (Phase 3.1)
builder.Services.AddScoped<IDonationAppointmentService, DonationAppointmentService>();

// Monitoring Services (Phase 3.2)
builder.Services.AddHostedService<MetricsCollector>();
builder.Services.AddSingleton<MetricsCollector>();

// Configure HttpClient
builder.Services.AddHttpClient();

// Configure Event Bus
builder.Services.AddSingleton<IEventBusService, EventBusService>();
builder.Services.AddHostedService<EventHandlerService>();

// Configure SignalR
builder.Services.AddSignalR();

// Configure JWT Authentication
var jwtSecret = builder.Configuration["JWT_SECRET"] ?? throw new InvalidOperationException("JWT_SECRET not configured");
var key = Encoding.UTF8.GetBytes(jwtSecret);

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(key),
            ValidateIssuer = false,
            ValidateAudience = false,
            ClockSkew = TimeSpan.Zero
        };
    });

builder.Services.AddAuthorization();

// Configure CORS
var allowedOrigins = builder.Configuration["CORS_ALLOWED_ORIGINS"]?.Split(',') ?? new[] { "http://localhost:5173", "http://localhost:3000" };

builder.Services.AddCors(options =>
{
    options.AddPolicy("RubiaPolicy", policy =>
    {
        policy.WithOrigins(allowedOrigins)
              .AllowAnyMethod()
              .AllowAnyHeader()
              .AllowCredentials();
    });
    
    options.AddPolicy("WebhookPolicy", policy =>
    {
        policy.AllowAnyOrigin()
              .WithMethods("POST", "GET", "OPTIONS")
              .AllowAnyHeader();
    });
});

// Health Checks (Phase 3.2)
builder.Services.AddHealthChecks()
    .AddCheck<DatabaseHealthCheck>("database")
    .AddCheck<RedisHealthCheck>("redis")
    .AddCheck<WhatsAppHealthCheck>("whatsapp")
    .AddCheck<OpenAIHealthCheck>("openai")
    .AddCheck<SystemResourcesHealthCheck>("system");

// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseCors("RubiaPolicy");

app.UseAuthentication();
app.UseAuthorization();

// Add JWT middleware
app.UseMiddleware<JwtAuthenticationMiddleware>();

app.MapControllers();

// Configure SignalR Hub
app.MapHub<ChatHub>("/ws/chat");

// Health Checks Endpoints
app.MapHealthChecks("/health");
app.MapHealthChecks("/health/ready", new HealthCheckOptions
{
    Predicate = check => check.Tags.Contains("ready")
});
app.MapHealthChecks("/health/live", new HealthCheckOptions
{
    Predicate = check => check.Tags.Contains("live")
});

// Ensure database is created
using (var scope = app.Services.CreateScope())
{
    var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
    if (app.Environment.IsDevelopment())
    {
        context.Database.EnsureCreated();
    }
}

app.Run();