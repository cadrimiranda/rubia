using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IDonationAppointmentService
{
    Task<DonationAppointmentDto> CreateAppointmentAsync(CreateDonationAppointmentDto createDto, CancellationToken cancellationToken = default);
    Task<DonationAppointmentDto> UpdateAppointmentAsync(Guid appointmentId, UpdateDonationAppointmentDto updateDto, CancellationToken cancellationToken = default);
    Task<bool> CancelAppointmentAsync(Guid appointmentId, string reason, Guid cancelledByUserId, CancellationToken cancellationToken = default);
    Task<DonationAppointmentDto?> GetAppointmentByIdAsync(Guid appointmentId, CancellationToken cancellationToken = default);
    
    Task<IEnumerable<DonationAppointmentDto>> GetAppointmentsByCompanyAsync(Guid companyId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default);
    Task<IEnumerable<DonationAppointmentDto>> GetAppointmentsByCustomerAsync(Guid customerId, CancellationToken cancellationToken = default);
    Task<PagedResult<DonationAppointmentDto>> GetAppointmentsPaginatedAsync(Guid companyId, int page, int pageSize, string? status = null, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default);
    
    Task<bool> ConfirmAppointmentAsync(Guid appointmentId, Guid confirmedByUserId, CancellationToken cancellationToken = default);
    Task<bool> CheckInAppointmentAsync(Guid appointmentId, Guid checkedInByUserId, CancellationToken cancellationToken = default);
    Task<bool> CompleteAppointmentAsync(Guid appointmentId, CompleteDonationAppointmentDto completeDto, CancellationToken cancellationToken = default);
    Task<bool> RescheduleAppointmentAsync(Guid appointmentId, RescheduleDonationAppointmentDto rescheduleDto, CancellationToken cancellationToken = default);
    
    Task<IEnumerable<AvailableSlotDto>> GetAvailableSlotsAsync(Guid companyId, DateTime date, CancellationToken cancellationToken = default);
    Task<AppointmentCalendarDto> GetAppointmentCalendarAsync(Guid companyId, DateTime month, CancellationToken cancellationToken = default);
    Task<DonationAppointmentStatsDto> GetAppointmentStatsAsync(Guid companyId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default);
    
    Task<bool> SendAppointmentRemindersAsync(DateTime reminderDate, CancellationToken cancellationToken = default);
    Task<IEnumerable<DonationAppointmentDto>> GetUpcomingAppointmentsAsync(Guid companyId, int daysAhead = 7, CancellationToken cancellationToken = default);
    Task<IEnumerable<DonationAppointmentDto>> GetOverdueAppointmentsAsync(Guid companyId, CancellationToken cancellationToken = default);
    
    Task<bool> ValidateAppointmentSlotAsync(Guid companyId, DateTime appointmentDateTime, int durationMinutes, CancellationToken cancellationToken = default);
    Task<int> GetAppointmentCapacityAsync(Guid companyId, DateTime date, CancellationToken cancellationToken = default);
}