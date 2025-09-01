using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class DonationAppointmentService : IDonationAppointmentService
{
    private readonly RubiaDbContext _context;
    private readonly IMessagingService _messagingService;
    private readonly ILogger<DonationAppointmentService> _logger;

    public DonationAppointmentService(
        RubiaDbContext context,
        IMessagingService messagingService,
        ILogger<DonationAppointmentService> logger)
    {
        _context = context;
        _messagingService = messagingService;
        _logger = logger;
    }

    public async Task<DonationAppointmentDto> CreateAppointmentAsync(CreateDonationAppointmentDto createDto, CancellationToken cancellationToken = default)
    {
        // Validate customer exists
        var customer = await _context.Customers.FindAsync(createDto.CustomerId, cancellationToken);
        if (customer == null)
            throw new ArgumentException($"Customer {createDto.CustomerId} not found");

        // Validate appointment slot availability
        var isSlotValid = await ValidateAppointmentSlotAsync(
            customer.CompanyId, 
            createDto.AppointmentDateTime, 
            createDto.EstimatedDurationMinutes ?? 60, 
            cancellationToken);

        if (!isSlotValid)
            throw new InvalidOperationException("Selected appointment slot is not available");

        var appointment = new DonationAppointment
        {
            Id = Guid.NewGuid(),
            CompanyId = customer.CompanyId,
            CustomerId = createDto.CustomerId,
            AppointmentDateTime = createDto.AppointmentDateTime,
            EstimatedDurationMinutes = createDto.EstimatedDurationMinutes ?? 60,
            AppointmentType = createDto.AppointmentType ?? DonationAppointmentType.BloodDonation.ToString(),
            Status = DonationAppointmentStatus.Scheduled.ToString(),
            Notes = createDto.Notes,
            CreatedByUserId = createDto.CreatedByUserId,
            CreatedAt = DateTime.UtcNow
        };

        _context.DonationAppointments.Add(appointment);
        await _context.SaveChangesAsync(cancellationToken);

        // Send confirmation message to customer
        await SendAppointmentConfirmationAsync(appointment, cancellationToken);

        _logger.LogInformation("Donation appointment {AppointmentId} created for customer {CustomerId}", 
            appointment.Id, createDto.CustomerId);

        return await MapToDtoAsync(appointment, cancellationToken);
    }

    public async Task<DonationAppointmentDto> UpdateAppointmentAsync(Guid appointmentId, UpdateDonationAppointmentDto updateDto, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            throw new ArgumentException($"Appointment {appointmentId} not found");

        if (appointment.Status == DonationAppointmentStatus.Completed.ToString() ||
            appointment.Status == DonationAppointmentStatus.Cancelled.ToString())
            throw new InvalidOperationException("Cannot update completed or cancelled appointments");

        var originalDateTime = appointment.AppointmentDateTime;
        var needsRescheduleNotification = false;

        if (updateDto.AppointmentDateTime.HasValue && updateDto.AppointmentDateTime != appointment.AppointmentDateTime)
        {
            var isSlotValid = await ValidateAppointmentSlotAsync(
                appointment.CompanyId,
                updateDto.AppointmentDateTime.Value,
                updateDto.EstimatedDurationMinutes ?? appointment.EstimatedDurationMinutes,
                cancellationToken);

            if (!isSlotValid)
                throw new InvalidOperationException("New appointment slot is not available");

            appointment.AppointmentDateTime = updateDto.AppointmentDateTime.Value;
            needsRescheduleNotification = true;
        }

        appointment.EstimatedDurationMinutes = updateDto.EstimatedDurationMinutes ?? appointment.EstimatedDurationMinutes;
        appointment.AppointmentType = updateDto.AppointmentType ?? appointment.AppointmentType;
        appointment.Notes = updateDto.Notes ?? appointment.Notes;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Send reschedule notification if datetime changed
        if (needsRescheduleNotification)
        {
            await SendAppointmentRescheduleNotificationAsync(appointment, originalDateTime, cancellationToken);
        }

        _logger.LogInformation("Donation appointment {AppointmentId} updated", appointmentId);

        return await MapToDtoAsync(appointment, cancellationToken);
    }

    public async Task<bool> CancelAppointmentAsync(Guid appointmentId, string reason, Guid cancelledByUserId, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            return false;

        if (appointment.Status == DonationAppointmentStatus.Completed.ToString())
            throw new InvalidOperationException("Cannot cancel completed appointments");

        appointment.Status = DonationAppointmentStatus.Cancelled.ToString();
        appointment.CancellationReason = reason;
        appointment.CancelledAt = DateTime.UtcNow;
        appointment.CancelledByUserId = cancelledByUserId;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Send cancellation notification
        await SendAppointmentCancellationNotificationAsync(appointment, reason, cancellationToken);

        _logger.LogInformation("Donation appointment {AppointmentId} cancelled by user {UserId}. Reason: {Reason}", 
            appointmentId, cancelledByUserId, reason);

        return true;
    }

    public async Task<DonationAppointmentDto?> GetAppointmentByIdAsync(Guid appointmentId, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.Company)
            .Include(a => a.CreatedByUser)
            .FirstOrDefaultAsync(a => a.Id == appointmentId, cancellationToken);

        return appointment != null ? await MapToDtoAsync(appointment, cancellationToken) : null;
    }

    public async Task<IEnumerable<DonationAppointmentDto>> GetAppointmentsByCompanyAsync(Guid companyId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default)
    {
        fromDate ??= DateTime.Today;
        toDate ??= DateTime.Today.AddMonths(1);

        var appointments = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.CreatedByUser)
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime >= fromDate 
                       && a.AppointmentDateTime <= toDate)
            .OrderBy(a => a.AppointmentDateTime)
            .ToListAsync(cancellationToken);

        var tasks = appointments.Select(a => MapToDtoAsync(a, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<IEnumerable<DonationAppointmentDto>> GetAppointmentsByCustomerAsync(Guid customerId, CancellationToken cancellationToken = default)
    {
        var appointments = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.Company)
            .Include(a => a.CreatedByUser)
            .Where(a => a.CustomerId == customerId)
            .OrderByDescending(a => a.AppointmentDateTime)
            .ToListAsync(cancellationToken);

        var tasks = appointments.Select(a => MapToDtoAsync(a, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<PagedResult<DonationAppointmentDto>> GetAppointmentsPaginatedAsync(Guid companyId, int page, int pageSize, string? status = null, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default)
    {
        var query = _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.CreatedByUser)
            .Where(a => a.CompanyId == companyId);

        if (!string.IsNullOrEmpty(status))
            query = query.Where(a => a.Status == status);

        if (fromDate.HasValue)
            query = query.Where(a => a.AppointmentDateTime >= fromDate.Value);

        if (toDate.HasValue)
            query = query.Where(a => a.AppointmentDateTime <= toDate.Value);

        var totalCount = await query.CountAsync(cancellationToken);

        var appointments = await query
            .OrderBy(a => a.AppointmentDateTime)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(cancellationToken);

        var tasks = appointments.Select(a => MapToDtoAsync(a, cancellationToken));
        var items = await Task.WhenAll(tasks);

        return new PagedResult<DonationAppointmentDto>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            PageSize = pageSize,
            TotalPages = (int)Math.Ceiling((double)totalCount / pageSize)
        };
    }

    public async Task<bool> ConfirmAppointmentAsync(Guid appointmentId, Guid confirmedByUserId, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            return false;

        if (appointment.Status != DonationAppointmentStatus.Scheduled.ToString())
            return false;

        appointment.Status = DonationAppointmentStatus.Confirmed.ToString();
        appointment.ConfirmedAt = DateTime.UtcNow;
        appointment.ConfirmedByUserId = confirmedByUserId;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Appointment {AppointmentId} confirmed by user {UserId}", appointmentId, confirmedByUserId);

        return true;
    }

    public async Task<bool> CheckInAppointmentAsync(Guid appointmentId, Guid checkedInByUserId, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            return false;

        appointment.Status = DonationAppointmentStatus.InProgress.ToString();
        appointment.CheckedInAt = DateTime.UtcNow;
        appointment.CheckedInByUserId = checkedInByUserId;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Appointment {AppointmentId} checked in by user {UserId}", appointmentId, checkedInByUserId);

        return true;
    }

    public async Task<bool> CompleteAppointmentAsync(Guid appointmentId, CompleteDonationAppointmentDto completeDto, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            return false;

        appointment.Status = DonationAppointmentStatus.Completed.ToString();
        appointment.CompletedAt = DateTime.UtcNow;
        appointment.CompletedByUserId = completeDto.CompletedByUserId;
        appointment.ActualDurationMinutes = completeDto.ActualDurationMinutes;
        appointment.CompletionNotes = completeDto.CompletionNotes;
        appointment.BloodPressure = completeDto.BloodPressure;
        appointment.HeartRate = completeDto.HeartRate;
        appointment.Weight = completeDto.Weight;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Send completion confirmation
        await SendAppointmentCompletionNotificationAsync(appointment, cancellationToken);

        _logger.LogInformation("Appointment {AppointmentId} completed by user {UserId}", 
            appointmentId, completeDto.CompletedByUserId);

        return true;
    }

    public async Task<bool> RescheduleAppointmentAsync(Guid appointmentId, RescheduleDonationAppointmentDto rescheduleDto, CancellationToken cancellationToken = default)
    {
        var appointment = await _context.DonationAppointments.FindAsync(appointmentId, cancellationToken);
        if (appointment == null)
            return false;

        if (appointment.Status == DonationAppointmentStatus.Completed.ToString() ||
            appointment.Status == DonationAppointmentStatus.Cancelled.ToString())
            throw new InvalidOperationException("Cannot reschedule completed or cancelled appointments");

        var isSlotValid = await ValidateAppointmentSlotAsync(
            appointment.CompanyId,
            rescheduleDto.NewAppointmentDateTime,
            appointment.EstimatedDurationMinutes,
            cancellationToken);

        if (!isSlotValid)
            throw new InvalidOperationException("New appointment slot is not available");

        var originalDateTime = appointment.AppointmentDateTime;
        appointment.AppointmentDateTime = rescheduleDto.NewAppointmentDateTime;
        appointment.RescheduleReason = rescheduleDto.Reason;
        appointment.RescheduledAt = DateTime.UtcNow;
        appointment.RescheduledByUserId = rescheduleDto.RescheduledByUserId;
        appointment.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        await SendAppointmentRescheduleNotificationAsync(appointment, originalDateTime, cancellationToken);

        _logger.LogInformation("Appointment {AppointmentId} rescheduled from {OriginalDate} to {NewDate}", 
            appointmentId, originalDateTime, rescheduleDto.NewAppointmentDateTime);

        return true;
    }

    public async Task<IEnumerable<AvailableSlotDto>> GetAvailableSlotsAsync(Guid companyId, DateTime date, CancellationToken cancellationToken = default)
    {
        // Business hours: 8 AM to 6 PM, 30-minute slots
        var slots = new List<AvailableSlotDto>();
        var startHour = 8;
        var endHour = 18;
        var slotDurationMinutes = 30;

        // Get existing appointments for the date
        var existingAppointments = await _context.DonationAppointments
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime.Date == date.Date
                       && a.Status != DonationAppointmentStatus.Cancelled.ToString())
            .Select(a => new { a.AppointmentDateTime, a.EstimatedDurationMinutes })
            .ToListAsync(cancellationToken);

        for (var hour = startHour; hour < endHour; hour++)
        {
            for (var minute = 0; minute < 60; minute += slotDurationMinutes)
            {
                var slotTime = date.Date.AddHours(hour).AddMinutes(minute);
                
                // Skip past slots
                if (slotTime <= DateTime.Now)
                    continue;

                // Check if slot conflicts with existing appointments
                var hasConflict = existingAppointments.Any(existing =>
                    slotTime < existing.AppointmentDateTime.AddMinutes(existing.EstimatedDurationMinutes) &&
                    slotTime.AddMinutes(slotDurationMinutes) > existing.AppointmentDateTime);

                if (!hasConflict)
                {
                    slots.Add(new AvailableSlotDto
                    {
                        DateTime = slotTime,
                        DurationMinutes = slotDurationMinutes,
                        IsAvailable = true
                    });
                }
            }
        }

        return slots;
    }

    public async Task<AppointmentCalendarDto> GetAppointmentCalendarAsync(Guid companyId, DateTime month, CancellationToken cancellationToken = default)
    {
        var startDate = new DateTime(month.Year, month.Month, 1);
        var endDate = startDate.AddMonths(1).AddDays(-1);

        var appointments = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime >= startDate 
                       && a.AppointmentDateTime <= endDate)
            .ToListAsync(cancellationToken);

        var calendarDays = new List<CalendarDayDto>();
        
        for (var date = startDate; date <= endDate; date = date.AddDays(1))
        {
            var dayAppointments = appointments
                .Where(a => a.AppointmentDateTime.Date == date.Date)
                .Select(a => new CalendarAppointmentDto
                {
                    Id = a.Id,
                    CustomerName = a.Customer?.Name ?? "Unknown",
                    AppointmentDateTime = a.AppointmentDateTime,
                    Status = Enum.Parse<DonationAppointmentStatus>(a.Status),
                    AppointmentType = Enum.Parse<DonationAppointmentType>(a.AppointmentType)
                })
                .OrderBy(a => a.AppointmentDateTime)
                .ToList();

            calendarDays.Add(new CalendarDayDto
            {
                Date = date,
                AppointmentCount = dayAppointments.Count,
                Appointments = dayAppointments,
                HasAvailableSlots = dayAppointments.Count < 16 // Max 16 appointments per day
            });
        }

        return new AppointmentCalendarDto
        {
            Month = month,
            Days = calendarDays
        };
    }

    public async Task<DonationAppointmentStatsDto> GetAppointmentStatsAsync(Guid companyId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default)
    {
        fromDate ??= DateTime.Today.AddMonths(-1);
        toDate ??= DateTime.Today;

        var appointments = await _context.DonationAppointments
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime >= fromDate 
                       && a.AppointmentDateTime <= toDate)
            .ToListAsync(cancellationToken);

        var totalAppointments = appointments.Count;
        var completedAppointments = appointments.Count(a => a.Status == DonationAppointmentStatus.Completed.ToString());
        var cancelledAppointments = appointments.Count(a => a.Status == DonationAppointmentStatus.Cancelled.ToString());
        var noShowAppointments = appointments.Count(a => a.Status == DonationAppointmentStatus.NoShow.ToString());

        return new DonationAppointmentStatsDto
        {
            CompanyId = companyId,
            PeriodStart = fromDate.Value,
            PeriodEnd = toDate.Value,
            TotalAppointments = totalAppointments,
            CompletedAppointments = completedAppointments,
            CancelledAppointments = cancelledAppointments,
            NoShowAppointments = noShowAppointments,
            CompletionRate = totalAppointments > 0 ? (double)completedAppointments / totalAppointments : 0,
            CancellationRate = totalAppointments > 0 ? (double)cancelledAppointments / totalAppointments : 0,
            NoShowRate = totalAppointments > 0 ? (double)noShowAppointments / totalAppointments : 0,
            AverageAppointmentsPerDay = totalAppointments > 0 ? totalAppointments / (toDate.Value - fromDate.Value).Days : 0
        };
    }

    public async Task<bool> SendAppointmentRemindersAsync(DateTime reminderDate, CancellationToken cancellationToken = default)
    {
        var appointmentsToRemind = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.Company)
            .Where(a => a.AppointmentDateTime.Date == reminderDate.Date
                       && (a.Status == DonationAppointmentStatus.Scheduled.ToString() ||
                           a.Status == DonationAppointmentStatus.Confirmed.ToString())
                       && a.ReminderSentAt == null)
            .ToListAsync(cancellationToken);

        var sentCount = 0;
        foreach (var appointment in appointmentsToRemind)
        {
            try
            {
                await SendAppointmentReminderAsync(appointment, cancellationToken);
                appointment.ReminderSentAt = DateTime.UtcNow;
                sentCount++;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to send reminder for appointment {AppointmentId}", appointment.Id);
            }
        }

        if (sentCount > 0)
        {
            await _context.SaveChangesAsync(cancellationToken);
            _logger.LogInformation("Sent {Count} appointment reminders for date {Date}", sentCount, reminderDate.Date);
        }

        return sentCount > 0;
    }

    public async Task<IEnumerable<DonationAppointmentDto>> GetUpcomingAppointmentsAsync(Guid companyId, int daysAhead = 7, CancellationToken cancellationToken = default)
    {
        var fromDate = DateTime.Today;
        var toDate = DateTime.Today.AddDays(daysAhead);

        var appointments = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.CreatedByUser)
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime >= fromDate 
                       && a.AppointmentDateTime <= toDate
                       && (a.Status == DonationAppointmentStatus.Scheduled.ToString() ||
                           a.Status == DonationAppointmentStatus.Confirmed.ToString()))
            .OrderBy(a => a.AppointmentDateTime)
            .ToListAsync(cancellationToken);

        var tasks = appointments.Select(a => MapToDtoAsync(a, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<IEnumerable<DonationAppointmentDto>> GetOverdueAppointmentsAsync(Guid companyId, CancellationToken cancellationToken = default)
    {
        var cutoffTime = DateTime.Now.AddHours(-2); // 2 hours past appointment time

        var appointments = await _context.DonationAppointments
            .Include(a => a.Customer)
            .Include(a => a.CreatedByUser)
            .Where(a => a.CompanyId == companyId 
                       && a.AppointmentDateTime < cutoffTime
                       && (a.Status == DonationAppointmentStatus.Scheduled.ToString() ||
                           a.Status == DonationAppointmentStatus.Confirmed.ToString()))
            .OrderBy(a => a.AppointmentDateTime)
            .ToListAsync(cancellationToken);

        var tasks = appointments.Select(a => MapToDtoAsync(a, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<bool> ValidateAppointmentSlotAsync(Guid companyId, DateTime appointmentDateTime, int durationMinutes, CancellationToken cancellationToken = default)
    {
        // Check business hours (8 AM to 6 PM)
        if (appointmentDateTime.Hour < 8 || appointmentDateTime.Hour >= 18)
            return false;

        // Check if it's not in the past
        if (appointmentDateTime <= DateTime.Now)
            return false;

        // Check for conflicts with existing appointments
        var endTime = appointmentDateTime.AddMinutes(durationMinutes);
        var hasConflict = await _context.DonationAppointments
            .AnyAsync(a => a.CompanyId == companyId
                          && a.Status != DonationAppointmentStatus.Cancelled.ToString()
                          && ((appointmentDateTime >= a.AppointmentDateTime && 
                               appointmentDateTime < a.AppointmentDateTime.AddMinutes(a.EstimatedDurationMinutes)) ||
                              (endTime > a.AppointmentDateTime && 
                               endTime <= a.AppointmentDateTime.AddMinutes(a.EstimatedDurationMinutes)) ||
                              (appointmentDateTime <= a.AppointmentDateTime && 
                               endTime >= a.AppointmentDateTime.AddMinutes(a.EstimatedDurationMinutes))), 
                      cancellationToken);

        return !hasConflict;
    }

    public async Task<int> GetAppointmentCapacityAsync(Guid companyId, DateTime date, CancellationToken cancellationToken = default)
    {
        var maxDailyAppointments = 16; // Business rule: max 16 appointments per day
        
        var existingCount = await _context.DonationAppointments
            .CountAsync(a => a.CompanyId == companyId 
                           && a.AppointmentDateTime.Date == date.Date
                           && a.Status != DonationAppointmentStatus.Cancelled.ToString(), 
                      cancellationToken);

        return Math.Max(0, maxDailyAppointments - existingCount);
    }

    // Private helper methods
    private async Task SendAppointmentConfirmationAsync(DonationAppointment appointment, CancellationToken cancellationToken)
    {
        var customer = await _context.Customers.FindAsync(appointment.CustomerId, cancellationToken);
        if (customer == null) return;

        var message = $"Olá {customer.Name}! Seu agendamento para doação foi confirmado para {appointment.AppointmentDateTime:dd/MM/yyyy HH:mm}. Local: [Endereço da unidade]. Em caso de dúvidas, entre em contato conosco.";
        
        // Implementation would depend on messaging service integration
        _logger.LogInformation("Appointment confirmation message would be sent to {Phone}: {Message}", customer.Phone, message);
    }

    private async Task SendAppointmentReminderAsync(DonationAppointment appointment, CancellationToken cancellationToken)
    {
        var customer = await _context.Customers.FindAsync(appointment.CustomerId, cancellationToken);
        if (customer == null) return;

        var message = $"Lembrete: {customer.Name}, você tem um agendamento para doação amanhã às {appointment.AppointmentDateTime:HH:mm}. Local: [Endereço da unidade]. Confirme sua presença!";
        
        _logger.LogInformation("Appointment reminder would be sent to {Phone}: {Message}", customer.Phone, message);
    }

    private async Task SendAppointmentRescheduleNotificationAsync(DonationAppointment appointment, DateTime originalDateTime, CancellationToken cancellationToken)
    {
        var customer = await _context.Customers.FindAsync(appointment.CustomerId, cancellationToken);
        if (customer == null) return;

        var message = $"Olá {customer.Name}! Seu agendamento foi reagendado de {originalDateTime:dd/MM/yyyy HH:mm} para {appointment.AppointmentDateTime:dd/MM/yyyy HH:mm}. Confirme sua presença!";
        
        _logger.LogInformation("Appointment reschedule notification would be sent to {Phone}: {Message}", customer.Phone, message);
    }

    private async Task SendAppointmentCancellationNotificationAsync(DonationAppointment appointment, string reason, CancellationToken cancellationToken)
    {
        var customer = await _context.Customers.FindAsync(appointment.CustomerId, cancellationToken);
        if (customer == null) return;

        var message = $"Olá {customer.Name}! Infelizmente seu agendamento para {appointment.AppointmentDateTime:dd/MM/yyyy HH:mm} foi cancelado. Motivo: {reason}. Entre em contato para reagendar.";
        
        _logger.LogInformation("Appointment cancellation notification would be sent to {Phone}: {Message}", customer.Phone, message);
    }

    private async Task SendAppointmentCompletionNotificationAsync(DonationAppointment appointment, CancellationToken cancellationToken)
    {
        var customer = await _context.Customers.FindAsync(appointment.CustomerId, cancellationToken);
        if (customer == null) return;

        var message = $"Parabéns {customer.Name}! Sua doação foi realizada com sucesso. Obrigado por ajudar a salvar vidas! 🩸❤️";
        
        _logger.LogInformation("Appointment completion notification would be sent to {Phone}: {Message}", customer.Phone, message);
    }

    private async Task<DonationAppointmentDto> MapToDtoAsync(DonationAppointment appointment, CancellationToken cancellationToken)
    {
        if (appointment.Customer == null)
        {
            await _context.Entry(appointment)
                .Reference(a => a.Customer)
                .LoadAsync(cancellationToken);
        }

        if (appointment.CreatedByUser == null && appointment.CreatedByUserId.HasValue)
        {
            await _context.Entry(appointment)
                .Reference(a => a.CreatedByUser)
                .LoadAsync(cancellationToken);
        }

        return new DonationAppointmentDto
        {
            Id = appointment.Id,
            CompanyId = appointment.CompanyId,
            CustomerId = appointment.CustomerId,
            AppointmentDateTime = appointment.AppointmentDateTime,
            EstimatedDurationMinutes = appointment.EstimatedDurationMinutes,
            ActualDurationMinutes = appointment.ActualDurationMinutes,
            AppointmentType = Enum.Parse<DonationAppointmentType>(appointment.AppointmentType),
            Status = Enum.Parse<DonationAppointmentStatus>(appointment.Status),
            Notes = appointment.Notes,
            CompletionNotes = appointment.CompletionNotes,
            CancellationReason = appointment.CancellationReason,
            RescheduleReason = appointment.RescheduleReason,
            BloodPressure = appointment.BloodPressure,
            HeartRate = appointment.HeartRate,
            Weight = appointment.Weight,
            CustomerName = appointment.Customer?.Name ?? "Unknown",
            CustomerPhone = appointment.Customer?.Phone ?? "Unknown",
            CustomerEmail = appointment.Customer?.Email,
            CreatedByUserName = appointment.CreatedByUser?.Name,
            ConfirmedAt = appointment.ConfirmedAt,
            CheckedInAt = appointment.CheckedInAt,
            CompletedAt = appointment.CompletedAt,
            CancelledAt = appointment.CancelledAt,
            RescheduledAt = appointment.RescheduledAt,
            ReminderSentAt = appointment.ReminderSentAt,
            CreatedAt = appointment.CreatedAt,
            UpdatedAt = appointment.UpdatedAt
        };
    }
}