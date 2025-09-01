using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Rubia.Server.Data;
using System.Data.Common;

namespace Rubia.Server.Tests.Integration;

public class BaseIntegrationTest : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Remove the existing DbContext registration
            var dbContextDescriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(DbContextOptions<RubiaDbContext>));

            if (dbContextDescriptor != null)
            {
                services.Remove(dbContextDescriptor);
            }

            // Remove any existing database connection
            var dbConnectionDescriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(DbConnection));

            if (dbConnectionDescriptor != null)
            {
                services.Remove(dbConnectionDescriptor);
            }

            // Add in-memory database for testing
            services.AddDbContext<RubiaDbContext>(options =>
            {
                options.UseInMemoryDatabase("TestDb");
                options.EnableSensitiveDataLogging();
            });

            // Ensure the database is created
            var serviceProvider = services.BuildServiceProvider();
            using var scope = serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
            context.Database.EnsureCreated();
        });

        builder.UseEnvironment("Testing");

        // Configure logging for tests
        builder.ConfigureLogging(logging =>
        {
            logging.ClearProviders();
            logging.AddConsole();
            logging.SetMinimumLevel(LogLevel.Warning);
        });
    }

    protected async Task<RubiaDbContext> GetDbContextAsync()
    {
        var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
        await context.Database.EnsureCreatedAsync();
        return context;
    }

    protected async Task CleanDatabaseAsync()
    {
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
        
        // Clean all tables in reverse dependency order
        context.CampaignContacts.RemoveRange(context.CampaignContacts);
        context.Campaigns.RemoveRange(context.Campaigns);
        context.Messages.RemoveRange(context.Messages);
        context.ConversationParticipants.RemoveRange(context.ConversationParticipants);
        context.Conversations.RemoveRange(context.Conversations);
        context.Customers.RemoveRange(context.Customers);
        context.MessageTemplates.RemoveRange(context.MessageTemplates);
        context.Users.RemoveRange(context.Users);
        context.Departments.RemoveRange(context.Departments);
        context.Companies.RemoveRange(context.Companies);
        context.CompanyGroups.RemoveRange(context.CompanyGroups);
        context.AIAgents.RemoveRange(context.AIAgents);
        context.AIModels.RemoveRange(context.AIModels);

        await context.SaveChangesAsync();
    }
}