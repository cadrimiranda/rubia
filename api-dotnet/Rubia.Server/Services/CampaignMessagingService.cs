using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;
using System.Text.RegularExpressions;

namespace Rubia.Server.Services;

public class CampaignMessagingService : ICampaignMessagingService
{
    private readonly RubiaDbContext _context;
    private readonly IMessagingService _messagingService;
    private readonly ILogger<CampaignMessagingService> _logger;

    public CampaignMessagingService(
        RubiaDbContext context,
        IMessagingService messagingService,
        ILogger<CampaignMessagingService> logger)
    {
        _context = context;
        _messagingService = messagingService;
        _logger = logger;
    }

    public async Task<bool> SendCampaignMessageAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts
            .Include(cc => cc.Campaign)
                .ThenInclude(c => c!.MessageTemplate)
            .Include(cc => cc.Customer)
            .FirstOrDefaultAsync(cc => cc.Id == campaignContactId, cancellationToken);

        if (campaignContact == null)
        {
            _logger.LogWarning("Campaign contact {ContactId} not found", campaignContactId);
            return false;
        }

        if (campaignContact.Status != CampaignContactStatus.Pending.ToString())
        {
            _logger.LogWarning("Campaign contact {ContactId} is not in pending status", campaignContactId);
            return false;
        }

        try
        {
            // Process message template with customer data
            var processedMessage = await ProcessCampaignMessageTemplateAsync(
                campaignContactId, 
                campaignContact.Campaign!.MessageTemplate?.Content ?? "", 
                cancellationToken);

            // Get or create conversation for this customer
            var conversation = await GetOrCreateConversationAsync(
                campaignContact.Campaign!.CompanyId, 
                campaignContact.CustomerId, 
                campaignContact.CampaignId, 
                cancellationToken);

            // Send message through messaging service
            var messageResult = await _messagingService.SendTextMessageAsync(new SendMessageDto
            {
                ConversationId = conversation.Id,
                Content = processedMessage.Content,
                SenderType = SenderType.System,
                CampaignContactId = campaignContactId
            }, cancellationToken);

            if (messageResult.Success)
            {
                await UpdateCampaignContactStatusAsync(
                    campaignContactId, 
                    CampaignContactStatus.Sent.ToString(), 
                    cancellationToken: cancellationToken);

                campaignContact.SentAt = DateTime.UtcNow;
                await _context.SaveChangesAsync(cancellationToken);

                _logger.LogInformation("Campaign message sent to contact {ContactId}", campaignContactId);
                return true;
            }
            else
            {
                await UpdateCampaignContactStatusAsync(
                    campaignContactId, 
                    CampaignContactStatus.Failed.ToString(), 
                    messageResult.ErrorMessage, 
                    cancellationToken);

                _logger.LogWarning("Failed to send campaign message to contact {ContactId}: {Error}", 
                    campaignContactId, messageResult.ErrorMessage);
                return false;
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending campaign message to contact {ContactId}", campaignContactId);

            await UpdateCampaignContactStatusAsync(
                campaignContactId, 
                CampaignContactStatus.Failed.ToString(), 
                ex.Message, 
                cancellationToken);

            return false;
        }
    }

    public async Task<bool> SendBulkCampaignMessagesAsync(IEnumerable<Guid> campaignContactIds, CancellationToken cancellationToken = default)
    {
        var tasks = campaignContactIds.Select(id => SendCampaignMessageAsync(id, cancellationToken));
        var results = await Task.WhenAll(tasks);

        var successCount = results.Count(r => r);
        var totalCount = results.Length;

        _logger.LogInformation("Bulk campaign messages: {SuccessCount}/{TotalCount} sent successfully", 
            successCount, totalCount);

        return successCount > 0;
    }

    public async Task<MessageDto> ProcessCampaignMessageTemplateAsync(Guid campaignContactId, string templateContent, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Include(cc => cc.Campaign)
            .FirstOrDefaultAsync(cc => cc.Id == campaignContactId, cancellationToken);

        if (campaignContact == null)
            throw new ArgumentException($"Campaign contact {campaignContactId} not found");

        // Replace template variables with customer data
        var processedContent = templateContent;
        var customer = campaignContact.Customer;

        // Common template variables
        var variables = new Dictionary<string, string>
        {
            { "{{NOME}}", customer.Name },
            { "{{NAME}}", customer.Name },
            { "{{TELEFONE}}", customer.Phone },
            { "{{PHONE}}", customer.Phone },
            { "{{EMAIL}}", customer.Email ?? "" },
            { "{{CPF}}", customer.Cpf ?? "" },
            { "{{ENDERECO}}", customer.Address ?? "" },
            { "{{ADDRESS}}", customer.Address ?? "" },
            { "{{CIDADE}}", customer.AddressCity ?? "" },
            { "{{CITY}}", customer.AddressCity ?? "" },
            { "{{ESTADO}}", customer.AddressState ?? "" },
            { "{{STATE}}", customer.AddressState ?? "" },
            { "{{HOJE}}", DateTime.Now.ToString("dd/MM/yyyy") },
            { "{{TODAY}}", DateTime.Now.ToString("MM/dd/yyyy") },
            { "{{AGORA}}", DateTime.Now.ToString("HH:mm") },
            { "{{NOW}}", DateTime.Now.ToString("HH:mm") }
        };

        foreach (var variable in variables)
        {
            processedContent = processedContent.Replace(variable.Key, variable.Value);
        }

        // Process any remaining template variables with regex
        var regex = new Regex(@"\{\{([A-Z_]+)\}\}", RegexOptions.IgnoreCase);
        processedContent = regex.Replace(processedContent, match =>
        {
            var variableName = match.Groups[1].Value;
            _logger.LogWarning("Unknown template variable: {Variable} in campaign contact {ContactId}", 
                variableName, campaignContactId);
            return match.Value; // Keep the original if not found
        });

        return new MessageDto
        {
            Content = processedContent,
            SenderType = SenderType.System,
            CampaignContactId = campaignContactId
        };
    }

    public async Task<bool> ScheduleCampaignMessageAsync(Guid campaignContactId, DateTime scheduledTime, CancellationToken cancellationToken = default)
    {
        // For now, we'll implement a simple delay-based scheduling
        // In a production environment, you might want to use a more sophisticated scheduling service like Quartz.NET
        
        var delay = scheduledTime - DateTime.UtcNow;
        if (delay <= TimeSpan.Zero)
        {
            // Send immediately if scheduled time has passed
            return await SendCampaignMessageAsync(campaignContactId, cancellationToken);
        }

        // Schedule the message
        _ = Task.Delay(delay, cancellationToken)
            .ContinueWith(async _ =>
            {
                if (!cancellationToken.IsCancellationRequested)
                {
                    await SendCampaignMessageAsync(campaignContactId, cancellationToken);
                }
            }, cancellationToken);

        _logger.LogInformation("Campaign message scheduled for contact {ContactId} at {ScheduledTime}", 
            campaignContactId, scheduledTime);

        return true;
    }

    public async Task<CampaignMessageStatsDto> GetCampaignMessageStatsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaignContacts = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .ToListAsync(cancellationToken);

        var totalContacts = campaignContacts.Count;
        var sentCount = campaignContacts.Count(cc => cc.Status == CampaignContactStatus.Sent.ToString() ||
                                                   cc.Status == CampaignContactStatus.Delivered.ToString() ||
                                                   cc.Status == CampaignContactStatus.Read.ToString() ||
                                                   cc.Status == CampaignContactStatus.Responded.ToString());
        
        var deliveredCount = campaignContacts.Count(cc => cc.Status == CampaignContactStatus.Delivered.ToString() ||
                                                        cc.Status == CampaignContactStatus.Read.ToString() ||
                                                        cc.Status == CampaignContactStatus.Responded.ToString());
        
        var readCount = campaignContacts.Count(cc => cc.Status == CampaignContactStatus.Read.ToString() ||
                                                   cc.Status == CampaignContactStatus.Responded.ToString());
        
        var respondedCount = campaignContacts.Count(cc => cc.Status == CampaignContactStatus.Responded.ToString());
        var failedCount = campaignContacts.Count(cc => cc.Status == CampaignContactStatus.Failed.ToString());

        return new CampaignMessageStatsDto
        {
            CampaignId = campaignId,
            TotalContacts = totalContacts,
            SentCount = sentCount,
            DeliveredCount = deliveredCount,
            ReadCount = readCount,
            RespondedCount = respondedCount,
            FailedCount = failedCount,
            PendingCount = totalContacts - sentCount - failedCount,
            DeliveryRate = totalContacts > 0 ? (double)deliveredCount / totalContacts : 0,
            ReadRate = deliveredCount > 0 ? (double)readCount / deliveredCount : 0,
            ResponseRate = deliveredCount > 0 ? (double)respondedCount / deliveredCount : 0,
            FailureRate = totalContacts > 0 ? (double)failedCount / totalContacts : 0
        };
    }

    public async Task UpdateCampaignContactStatusAsync(Guid campaignContactId, string status, string? errorMessage = null, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts.FindAsync(campaignContactId, cancellationToken);
        if (campaignContact == null)
            return;

        campaignContact.Status = status;
        campaignContact.ErrorMessage = errorMessage;
        campaignContact.UpdatedAt = DateTime.UtcNow;

        // Update timestamp based on status
        switch (status)
        {
            case nameof(CampaignContactStatus.Sent):
                campaignContact.SentAt = DateTime.UtcNow;
                break;
            case nameof(CampaignContactStatus.Delivered):
                campaignContact.DeliveredAt = DateTime.UtcNow;
                break;
            case nameof(CampaignContactStatus.Read):
                campaignContact.ReadAt = DateTime.UtcNow;
                break;
            case nameof(CampaignContactStatus.Responded):
                campaignContact.RespondedAt = DateTime.UtcNow;
                break;
        }

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign contact {ContactId} status updated to {Status}", 
            campaignContactId, status);
    }

    public async Task<bool> RetryCampaignMessageAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts.FindAsync(campaignContactId, cancellationToken);
        if (campaignContact == null)
            return false;

        if (campaignContact.Status != CampaignContactStatus.Failed.ToString())
        {
            _logger.LogWarning("Campaign contact {ContactId} is not in failed status, cannot retry", campaignContactId);
            return false;
        }

        // Reset status to pending and clear error message
        campaignContact.Status = CampaignContactStatus.Pending.ToString();
        campaignContact.ErrorMessage = null;
        campaignContact.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Attempt to send the message again
        return await SendCampaignMessageAsync(campaignContactId, cancellationToken);
    }

    private async Task<Conversation> GetOrCreateConversationAsync(Guid companyId, Guid customerId, Guid? campaignId, CancellationToken cancellationToken)
    {
        // Try to find existing conversation for this customer
        var existingConversation = await _context.Conversations
            .FirstOrDefaultAsync(c => c.CompanyId == companyId && c.CustomerId == customerId, cancellationToken);

        if (existingConversation != null)
        {
            // Update campaign association if needed
            if (campaignId.HasValue && existingConversation.CampaignId != campaignId)
            {
                existingConversation.CampaignId = campaignId;
                existingConversation.UpdatedAt = DateTime.UtcNow;
                await _context.SaveChangesAsync(cancellationToken);
            }
            return existingConversation;
        }

        // Create new conversation
        var conversation = new Conversation
        {
            Id = Guid.NewGuid(),
            CompanyId = companyId,
            CustomerId = customerId,
            CampaignId = campaignId,
            Channel = ConversationChannel.WhatsApp.ToString(),
            ConversationType = ConversationType.OneToOne.ToString(),
            Status = ConversationStatus.Open.ToString(),
            CreatedAt = DateTime.UtcNow
        };

        _context.Conversations.Add(conversation);
        await _context.SaveChangesAsync(cancellationToken);

        return conversation;
    }
}