using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Newtonsoft.Json;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using System.Net.Http.Headers;
using System.Text;
using Xunit;

namespace Rubia.Server.Tests.Integration;

public class CampaignControllerIntegrationTest : BaseIntegrationTest, IAsyncLifetime
{
    private HttpClient _client;
    private CompanyGroup _companyGroup;
    private Company _company;
    private User _user;
    private MessageTemplate _messageTemplate;
    private Department _department;

    public async Task InitializeAsync()
    {
        _client = CreateClient();
        await SetupTestDataAsync();
    }

    public async Task DisposeAsync()
    {
        await CleanDatabaseAsync();
        _client?.Dispose();
    }

    private async Task SetupTestDataAsync()
    {
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();

        // Setup company group
        _companyGroup = new CompanyGroup
        {
            Id = Guid.NewGuid(),
            Name = "Test Group"
        };
        context.CompanyGroups.Add(_companyGroup);

        // Setup company
        _company = new Company
        {
            Id = Guid.NewGuid(),
            Name = "Test Company",
            Slug = "test-company",
            ContactEmail = "test@company.com",
            ContactPhone = "1199999999",
            CompanyGroupId = _companyGroup.Id,
            CompanyGroup = _companyGroup
        };
        context.Companies.Add(_company);

        // Setup department
        _department = new Department
        {
            Id = Guid.NewGuid(),
            Name = "Test Department",
            CompanyId = _company.Id,
            Company = _company
        };
        context.Departments.Add(_department);

        // Setup user
        _user = new User
        {
            Id = Guid.NewGuid(),
            Name = "Test User",
            Email = "test@user.com",
            PasswordHash = "password",
            Role = UserRole.ADMIN,
            CompanyId = _company.Id,
            Company = _company,
            DepartmentId = _department.Id,
            Department = _department
        };
        context.Users.Add(_user);

        // Setup message template
        _messageTemplate = new MessageTemplate
        {
            Id = Guid.NewGuid(),
            Name = "Test Template",
            Content = "Test message content",
            CompanyId = _company.Id,
            Company = _company,
            CreatedById = _user.Id,
            CreatedBy = _user
        };
        context.MessageTemplates.Add(_messageTemplate);

        await context.SaveChangesAsync();
    }

    [Fact]
    public async Task ShouldProcessValidExcelFileAndCreateTwoContacts()
    {
        // Arrange
        var filePath = Path.Combine("Resources", "planilha_correta.xlsx");
        var fileBytes = await File.ReadAllBytesAsync(filePath);

        var processCampaignDto = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "Test Campaign",
            Description = "Test Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        using var formData = new MultipartFormDataContent();
        
        // Add Excel file
        var fileContent = new ByteArrayContent(fileBytes);
        fileContent.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        formData.Add(fileContent, "file", "planilha_correta.xlsx");

        // Add JSON data
        var jsonContent = new StringContent(JsonConvert.SerializeObject(processCampaignDto), Encoding.UTF8, "application/json");
        formData.Add(jsonContent, "data");

        // Act
        var response = await _client.PostAsync("/api/campaigns/process", formData);

        // Assert
        response.Should().BeSuccessful();

        var responseContent = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<dynamic>(responseContent);

        // Verify response structure
        ((bool)result.success).Should().BeTrue();
        ((string)result.campaign.name).Should().Be("Test Campaign");
        ((int)result.statistics.created).Should().Be(2);
        ((int)result.statistics.processed).Should().Be(2);
        ((int)result.statistics.duplicates).Should().Be(0);
        ((int)result.statistics.errors).Should().Be(0);

        // Verify customers were created
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
        
        var customers = await context.Customers.ToListAsync();
        customers.Should().HaveCount(2);

        var maura = customers.FirstOrDefault(c => c.Name == "MAURA RODRIGUES");
        maura.Should().NotBeNull();
        maura!.Phone.Should().Be("+5511999999999");
        maura.BirthDate.Should().HaveValue();
        maura.BirthDate!.Value.ToString("yyyy-MM-dd").Should().StartWith("2003-09-18");

        var tarze = customers.FirstOrDefault(c => c.Name == "TARZE CARVALHO");
        tarze.Should().NotBeNull();
        tarze!.Phone.Should().Be("+5511999999992");
        tarze.BirthDate.Should().HaveValue();
        tarze.BirthDate!.Value.ToString("yyyy-MM-dd").Should().StartWith("2005-08-16");

        // Verify campaign was created
        var campaigns = await context.Campaigns.ToListAsync();
        campaigns.Should().HaveCount(1);
        campaigns.First().TotalContacts.Should().Be(2);

        // Verify campaign contacts were created
        var campaignContacts = await context.CampaignContacts.ToListAsync();
        campaignContacts.Should().HaveCount(2);

        // Verify conversations were created
        var conversations = await context.Conversations.ToListAsync();
        conversations.Should().HaveCount(2);
    }

    [Fact]
    public async Task ShouldRejectExcelFileWithDuplicatePhones()
    {
        // Arrange
        var filePath = Path.Combine("Resources", "planilha_incorreta.xlsx");
        var fileBytes = await File.ReadAllBytesAsync(filePath);

        var processCampaignDto = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "Test Campaign Incorrect",
            Description = "Test Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        using var formData = new MultipartFormDataContent();
        
        // Add Excel file
        var fileContent = new ByteArrayContent(fileBytes);
        fileContent.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        formData.Add(fileContent, "file", "planilha_incorreta.xlsx");

        // Add JSON data
        var jsonContent = new StringContent(JsonConvert.SerializeObject(processCampaignDto), Encoding.UTF8, "application/json");
        formData.Add(jsonContent, "data");

        // Act
        var response = await _client.PostAsync("/api/campaigns/process", formData);

        // Assert
        response.Should().BeSuccessful();

        var responseContent = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<dynamic>(responseContent);

        // Verify response structure
        ((bool)result.success).Should().BeTrue();
        ((int)result.statistics.duplicates).Should().BeGreaterThan(0);

        // Verify only unique contacts were created
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();
        
        var customers = await context.Customers.ToListAsync();
        customers.Should().HaveCountLessThanOrEqualTo(1);
    }

    [Fact]
    public async Task ShouldCreateTwoCampaignsWithSameContactsWithoutError()
    {
        // Arrange - First campaign
        var filePath = Path.Combine("Resources", "planilha_correta.xlsx");
        var fileBytes = await File.ReadAllBytesAsync(filePath);

        var processCampaignDto1 = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "First Campaign",
            Description = "First Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        // Act - First campaign
        using (var formData1 = new MultipartFormDataContent())
        {
            var fileContent1 = new ByteArrayContent(fileBytes);
            fileContent1.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            formData1.Add(fileContent1, "file", "planilha_correta.xlsx");

            var jsonContent1 = new StringContent(JsonConvert.SerializeObject(processCampaignDto1), Encoding.UTF8, "application/json");
            formData1.Add(jsonContent1, "data");

            var response1 = await _client.PostAsync("/api/campaigns/process", formData1);
            response1.Should().BeSuccessful();

            var responseContent1 = await response1.Content.ReadAsStringAsync();
            var result1 = JsonConvert.DeserializeObject<dynamic>(responseContent1);
            ((bool)result1.success).Should().BeTrue();
        }

        // Arrange - Second campaign
        var processCampaignDto2 = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "Second Campaign",
            Description = "Second Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        // Act - Second campaign
        using (var formData2 = new MultipartFormDataContent())
        {
            var fileContent2 = new ByteArrayContent(fileBytes);
            fileContent2.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            formData2.Add(fileContent2, "file", "planilha_correta.xlsx");

            var jsonContent2 = new StringContent(JsonConvert.SerializeObject(processCampaignDto2), Encoding.UTF8, "application/json");
            formData2.Add(jsonContent2, "data");

            var response2 = await _client.PostAsync("/api/campaigns/process", formData2);
            response2.Should().BeSuccessful();

            var responseContent2 = await response2.Content.ReadAsStringAsync();
            var result2 = JsonConvert.DeserializeObject<dynamic>(responseContent2);
            ((bool)result2.success).Should().BeTrue();
        }

        // Assert - Verify both campaigns were created
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();

        var campaigns = await context.Campaigns.ToListAsync();
        campaigns.Should().HaveCount(2);
        campaigns.Select(c => c.Name).Should().Contain(new[] { "First Campaign", "Second Campaign" });

        // Verify customers exist (should be same 2 customers)
        var customers = await context.Customers.ToListAsync();
        customers.Should().HaveCount(2);

        // Verify campaign contacts exist for both campaigns
        var campaignContacts = await context.CampaignContacts.ToListAsync();
        campaignContacts.Should().HaveCount(4); // 2 contacts × 2 campaigns

        // Verify conversations exist for both campaigns
        var conversations = await context.Conversations.ToListAsync();
        conversations.Should().HaveCount(4); // 2 contacts × 2 campaigns
    }

    [Fact]
    public async Task ShouldUpdateContactAndCreateTwoCampaigns()
    {
        // Arrange - First campaign with original data
        var originalFilePath = Path.Combine("Resources", "planilha_correta.xlsx");
        var originalFileBytes = await File.ReadAllBytesAsync(originalFilePath);

        var processCampaignDto1 = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "Original Campaign",
            Description = "Original Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        // Act - First campaign
        using (var formData1 = new MultipartFormDataContent())
        {
            var fileContent1 = new ByteArrayContent(originalFileBytes);
            fileContent1.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            formData1.Add(fileContent1, "file", "planilha_correta.xlsx");

            var jsonContent1 = new StringContent(JsonConvert.SerializeObject(processCampaignDto1), Encoding.UTF8, "application/json");
            formData1.Add(jsonContent1, "data");

            var response1 = await _client.PostAsync("/api/campaigns/process", formData1);
            response1.Should().BeSuccessful();

            var responseContent1 = await response1.Content.ReadAsStringAsync();
            var result1 = JsonConvert.DeserializeObject<dynamic>(responseContent1);
            ((bool)result1.success).Should().BeTrue();
        }

        // Arrange - Second campaign with updated data
        var updatedFilePath = Path.Combine("Resources", "planilha_correta_update.xlsx");
        var updatedFileBytes = await File.ReadAllBytesAsync(updatedFilePath);

        var processCampaignDto2 = new ProcessCampaignDto
        {
            CompanyId = _company.Id,
            UserId = _user.Id,
            Name = "Updated Campaign",
            Description = "Updated Description",
            StartDate = DateOnly.FromDateTime(DateTime.Now),
            EndDate = DateOnly.FromDateTime(DateTime.Now.AddDays(30)),
            SourceSystem = "TEST",
            TemplateIds = new List<Guid> { _messageTemplate.Id }
        };

        // Act - Second campaign with updates
        using (var formData2 = new MultipartFormDataContent())
        {
            var fileContent2 = new ByteArrayContent(updatedFileBytes);
            fileContent2.Headers.ContentType = MediaTypeHeaderValue.Parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            formData2.Add(fileContent2, "file", "planilha_correta_update.xlsx");

            var jsonContent2 = new StringContent(JsonConvert.SerializeObject(processCampaignDto2), Encoding.UTF8, "application/json");
            formData2.Add(jsonContent2, "data");

            var response2 = await _client.PostAsync("/api/campaigns/process", formData2);
            response2.Should().BeSuccessful();

            var responseContent2 = await response2.Content.ReadAsStringAsync();
            var result2 = JsonConvert.DeserializeObject<dynamic>(responseContent2);
            ((bool)result2.success).Should().BeTrue();
        }

        // Assert - Verify MAURA RODRIGUES was updated
        using var scope = Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<RubiaDbContext>();

        var customers = await context.Customers.ToListAsync();
        var maura = customers.FirstOrDefault(c => c.Name == "MAURA RODRIGUES");
        maura.Should().NotBeNull();
        maura!.Phone.Should().Be("+5511999999999");
        maura.BirthDate.Should().HaveValue();
        maura.BirthDate!.Value.ToString("yyyy-MM-dd").Should().StartWith("2003-09-18");

        // Verify both campaigns were created
        var campaigns = await context.Campaigns.ToListAsync();
        campaigns.Should().HaveCount(2);
        campaigns.Select(c => c.Name).Should().Contain(new[] { "Original Campaign", "Updated Campaign" });

        // Verify campaign contacts and conversations exist for both campaigns
        var campaignContacts = await context.CampaignContacts.ToListAsync();
        campaignContacts.Should().HaveCount(4); // 2 contacts × 2 campaigns

        var conversations = await context.Conversations.ToListAsync();
        conversations.Should().HaveCount(4); // 2 contacts × 2 campaigns
    }
}