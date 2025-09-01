using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class DonationAppointmentDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public Guid CustomerId { get; set; }
    public DateTime AppointmentDateTime { get; set; }
    public int EstimatedDurationMinutes { get; set; }
    public int? ActualDurationMinutes { get; set; }
    public DonationAppointmentType AppointmentType { get; set; }
    public DonationAppointmentStatus Status { get; set; }
    public string? Notes { get; set; }
    public string? CompletionNotes { get; set; }
    public string? CancellationReason { get; set; }
    public string? RescheduleReason { get; set; }
    public string? BloodPressure { get; set; }
    public int? HeartRate { get; set; }
    public decimal? Weight { get; set; }
    
    // Navigation properties
    public string CustomerName { get; set; } = string.Empty;
    public string CustomerPhone { get; set; } = string.Empty;
    public string? CustomerEmail { get; set; }
    public string? CreatedByUserName { get; set; }
    
    // Timestamps
    public DateTime? ConfirmedAt { get; set; }
    public DateTime? CheckedInAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public DateTime? CancelledAt { get; set; }
    public DateTime? RescheduledAt { get; set; }
    public DateTime? ReminderSentAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
}

public class CreateDonationAppointmentDto
{
    [Required]
    public Guid CustomerId { get; set; }
    
    [Required]
    public DateTime AppointmentDateTime { get; set; }
    
    public int? EstimatedDurationMinutes { get; set; } = 60;
    public string? AppointmentType { get; set; }
    public string? Notes { get; set; }
    
    [Required]
    public Guid CreatedByUserId { get; set; }
}

public class UpdateDonationAppointmentDto
{
    public DateTime? AppointmentDateTime { get; set; }
    public int? EstimatedDurationMinutes { get; set; }
    public string? AppointmentType { get; set; }
    public string? Notes { get; set; }
}

public class CompleteDonationAppointmentDto
{
    [Required]
    public Guid CompletedByUserId { get; set; }
    
    public int? ActualDurationMinutes { get; set; }
    public string? CompletionNotes { get; set; }
    public string? BloodPressure { get; set; }
    public int? HeartRate { get; set; }
    public decimal? Weight { get; set; }
}

public class RescheduleDonationAppointmentDto
{
    [Required]
    public DateTime NewAppointmentDateTime { get; set; }
    
    [Required]
    public Guid RescheduledByUserId { get; set; }
    
    public string? Reason { get; set; }
}

public class AvailableSlotDto
{
    public DateTime DateTime { get; set; }
    public int DurationMinutes { get; set; }
    public bool IsAvailable { get; set; }
}

public class AppointmentCalendarDto
{
    public DateTime Month { get; set; }
    public List<CalendarDayDto> Days { get; set; } = new();
}

public class CalendarDayDto
{
    public DateTime Date { get; set; }
    public int AppointmentCount { get; set; }
    public List<CalendarAppointmentDto> Appointments { get; set; } = new();
    public bool HasAvailableSlots { get; set; }
}

public class CalendarAppointmentDto
{
    public Guid Id { get; set; }
    public string CustomerName { get; set; } = string.Empty;
    public DateTime AppointmentDateTime { get; set; }
    public DonationAppointmentStatus Status { get; set; }
    public DonationAppointmentType AppointmentType { get; set; }
}

public class DonationAppointmentStatsDto
{
    public Guid CompanyId { get; set; }
    public DateTime PeriodStart { get; set; }
    public DateTime PeriodEnd { get; set; }
    public int TotalAppointments { get; set; }
    public int CompletedAppointments { get; set; }
    public int CancelledAppointments { get; set; }
    public int NoShowAppointments { get; set; }
    public double CompletionRate { get; set; }
    public double CancellationRate { get; set; }
    public double NoShowRate { get; set; }
    public double AverageAppointmentsPerDay { get; set; }
}