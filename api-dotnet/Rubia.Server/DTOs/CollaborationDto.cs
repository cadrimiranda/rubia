using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

// Conversation Participant DTOs
public class ConversationParticipantDto
{
    public Guid Id { get; set; }
    public Guid ConversationId { get; set; }
    public Guid UserId { get; set; }
    public ConversationParticipantRole Role { get; set; }
    public bool IsActive { get; set; }
    public bool CanRead { get; set; }
    public bool CanWrite { get; set; }
    public bool CanAssign { get; set; }
    public DateTime JoinedAt { get; set; }
    public DateTime? LeftAt { get; set; }
    public string UserName { get; set; } = string.Empty;
    public string UserEmail { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
}

public class CreateConversationParticipantDto
{
    [Required]
    public Guid ConversationId { get; set; }
    
    [Required]
    public Guid UserId { get; set; }
    
    public string? Role { get; set; } = "Member";
}

public class ConversationParticipantStatsDto
{
    public Guid UserId { get; set; }
    public DateTime PeriodStart { get; set; }
    public DateTime PeriodEnd { get; set; }
    public int ActiveConversations { get; set; }
    public int TotalConversations { get; set; }
    public int MessagesSent { get; set; }
    public double AverageResponseTimeMinutes { get; set; }
    public int ConversationsResolved { get; set; }
    public int ConversationsTransferred { get; set; }
}

// User AI Agent DTOs
public class UserAIAgentDto
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid AiAgentId { get; set; }
    public bool IsActive { get; set; }
    public bool IsPrimary { get; set; }
    public string? CustomPrompt { get; set; }
    public double? CustomTemperature { get; set; }
    public int? CustomMaxTokens { get; set; }
    public bool AutoResponseEnabled { get; set; }
    public int? DailyMessageLimit { get; set; }
    public int? HourlyMessageLimit { get; set; }
    public int MessagesToday { get; set; }
    public int MessagesThisHour { get; set; }
    public string UserName { get; set; } = string.Empty;
    public string UserEmail { get; set; } = string.Empty;
    public string AiAgentName { get; set; } = string.Empty;
    public string? AiAgentDescription { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
}

public class CreateUserAIAgentDto
{
    [Required]
    public Guid UserId { get; set; }
    
    [Required]
    public Guid AiAgentId { get; set; }
    
    public bool IsActive { get; set; } = true;
    public bool IsPrimary { get; set; } = false;
    public string? CustomPrompt { get; set; }
    public double? CustomTemperature { get; set; }
    public int? CustomMaxTokens { get; set; }
    public bool? AutoResponseEnabled { get; set; }
    public int? DailyMessageLimit { get; set; }
    public int? HourlyMessageLimit { get; set; }
}

public class UpdateUserAIAgentDto
{
    public bool? IsActive { get; set; }
    public bool? IsPrimary { get; set; }
    public string? CustomPrompt { get; set; }
    public double? CustomTemperature { get; set; }
    public int? CustomMaxTokens { get; set; }
    public bool? AutoResponseEnabled { get; set; }
    public int? DailyMessageLimit { get; set; }
    public int? HourlyMessageLimit { get; set; }
}

public class UserAIAgentStatsDto
{
    public Guid UserId { get; set; }
    public DateTime PeriodStart { get; set; }
    public DateTime PeriodEnd { get; set; }
    public int ActiveAIAgents { get; set; }
    public int TotalAIAgents { get; set; }
    public int AIGeneratedMessages { get; set; }
    public double AverageAIConfidence { get; set; }
    public long TotalTokensUsed { get; set; }
    public decimal TotalCost { get; set; }
}

public class AIAgentPreferencesDto
{
    public Guid UserAIAgentId { get; set; }
    public string? CustomPrompt { get; set; }
    public double? CustomTemperature { get; set; }
    public int? CustomMaxTokens { get; set; }
    public bool AutoResponseEnabled { get; set; }
    public int? DailyMessageLimit { get; set; }
    public int? HourlyMessageLimit { get; set; }
    
    // Default values from AI Agent
    public string? DefaultPrompt { get; set; }
    public double DefaultTemperature { get; set; }
    public int DefaultMaxTokens { get; set; }
}

public class UpdateAIAgentPreferencesDto
{
    public string? CustomPrompt { get; set; }
    public double? CustomTemperature { get; set; }
    public int? CustomMaxTokens { get; set; }
    public bool? AutoResponseEnabled { get; set; }
    public int? DailyMessageLimit { get; set; }
    public int? HourlyMessageLimit { get; set; }
}