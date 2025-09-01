using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.Entities;
using Rubia.Server.Services;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/message-enhancement-audit")]
[Authorize]
public class MessageEnhancementAuditController : ControllerBase
{
    private readonly MessageEnhancementAuditService _auditService;
    private readonly CompanyContextService _companyContextService;
    private readonly ILogger<MessageEnhancementAuditController> _logger;

    public MessageEnhancementAuditController(
        MessageEnhancementAuditService auditService,
        CompanyContextService companyContextService,
        ILogger<MessageEnhancementAuditController> logger)
    {
        _auditService = auditService;
        _companyContextService = companyContextService;
        _logger = logger;
    }

    [HttpGet("company/{companyId}")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByCompany(
        Guid companyId,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20,
        [FromQuery] string sortBy = "createdAt",
        [FromQuery] string sortDir = "desc")
    {
        _logger.LogDebug("Fetching message enhancement audits for company: {CompanyId}", companyId);

        await _companyContextService.EnsureCompanyAccessAsync(companyId);

        var (items, totalCount) = await _auditService.GetAuditsByCompanyAsync(companyId, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("company/{companyId}/stats")]
    public async Task<ActionResult<EnhancementStats>> GetCompanyStats(Guid companyId)
    {
        _logger.LogDebug("Fetching enhancement stats for company: {CompanyId}", companyId);

        await _companyContextService.EnsureCompanyAccessAsync(companyId);

        var stats = await _auditService.GetCompanyStatsAsync(companyId);
        return Ok(stats);
    }

    [HttpGet("company/{companyId}/by-date-range")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByDateRange(
        Guid companyId,
        [FromQuery, Required] DateTime startDate,
        [FromQuery, Required] DateTime endDate,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        _logger.LogDebug("Fetching audits for company: {CompanyId} between {StartDate} and {EndDate}",
            companyId, startDate, endDate);

        await _companyContextService.EnsureCompanyAccessAsync(companyId);

        var (items, totalCount) = await _auditService.GetAuditsByDateRangeAsync(
            companyId, startDate, endDate, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("company/{companyId}/by-temperament/{temperament}")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByTemperament(
        Guid companyId,
        string temperament,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        _logger.LogDebug("Fetching audits for company: {CompanyId} with temperament: {Temperament}",
            companyId, temperament);

        await _companyContextService.EnsureCompanyAccessAsync(companyId);

        var (items, totalCount) = await _auditService.GetAuditsByTemperamentAsync(
            companyId, temperament, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("company/{companyId}/by-ai-model/{model}")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByAiModel(
        Guid companyId,
        string model,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        _logger.LogDebug("Fetching audits for company: {CompanyId} with AI model: {Model}",
            companyId, model);

        await _companyContextService.EnsureCompanyAccessAsync(companyId);

        var (items, totalCount) = await _auditService.GetAuditsByAiModelAsync(
            companyId, model, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("user/{userId}")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByUser(
        Guid userId,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        _logger.LogDebug("Fetching audits for user: {UserId}", userId);

        var (items, totalCount) = await _auditService.GetAuditsByUserAsync(userId, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("ai-agent/{aiAgentId}")]
    public async Task<ActionResult<PagedResult<MessageEnhancementAudit>>> GetAuditsByAiAgent(
        Guid aiAgentId,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        _logger.LogDebug("Fetching audits for AI agent: {AiAgentId}", aiAgentId);

        var (items, totalCount) = await _auditService.GetAuditsByAiAgentAsync(aiAgentId, page, size);

        return Ok(new PagedResult<MessageEnhancementAudit>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            Size = size
        });
    }

    [HttpGet("conversation/{conversationId}")]
    public async Task<ActionResult<List<MessageEnhancementAudit>>> GetAuditsByConversation(
        Guid conversationId)
    {
        _logger.LogDebug("Fetching audits for conversation: {ConversationId}", conversationId);

        var audits = await _auditService.GetAuditsByConversationAsync(conversationId);
        return Ok(audits);
    }
}

public class PagedResult<T>
{
    public List<T> Items { get; set; } = [];
    public int TotalCount { get; set; }
    public int Page { get; set; }
    public int Size { get; set; }
    public int TotalPages => (int)Math.Ceiling((double)TotalCount / Size);
}