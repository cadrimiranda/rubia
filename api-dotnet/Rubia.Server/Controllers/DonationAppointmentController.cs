using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class DonationAppointmentController : ControllerBase
{
    private readonly IDonationAppointmentService _donationAppointmentService;
    private readonly ILogger<DonationAppointmentController> _logger;

    public DonationAppointmentController(
        IDonationAppointmentService donationAppointmentService,
        ILogger<DonationAppointmentController> logger)
    {
        _donationAppointmentService = donationAppointmentService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<DonationAppointmentDto>>> GetAppointments()
    {
        try
        {
            var appointments = await _donationAppointmentService.GetAppointmentsAsync();
            return Ok(appointments);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving appointments");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<DonationAppointmentDto>> GetAppointment(int id)
    {
        try
        {
            var appointment = await _donationAppointmentService.GetAppointmentByIdAsync(id);
            if (appointment == null)
            {
                return NotFound();
            }
            return Ok(appointment);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<DonationAppointmentDto>> CreateAppointment([FromBody] CreateDonationAppointmentDto createDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var appointment = await _donationAppointmentService.CreateAppointmentAsync(createDto);
            return CreatedAtAction(nameof(GetAppointment), new { id = appointment.Id }, appointment);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating appointment");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<DonationAppointmentDto>> UpdateAppointment(int id, [FromBody] UpdateDonationAppointmentDto updateDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var appointment = await _donationAppointmentService.UpdateAppointmentAsync(id, updateDto);
            if (appointment == null)
            {
                return NotFound();
            }

            return Ok(appointment);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteAppointment(int id)
    {
        try
        {
            var success = await _donationAppointmentService.DeleteAppointmentAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("calendar/{date}")]
    public async Task<ActionResult<IEnumerable<AppointmentSlotDto>>> GetAvailableSlots(DateTime date)
    {
        try
        {
            var slots = await _donationAppointmentService.GetAvailableSlotsAsync(date);
            return Ok(slots);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving available slots for {Date}", date);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id}/checkin")]
    public async Task<IActionResult> CheckIn(int id)
    {
        try
        {
            var success = await _donationAppointmentService.CheckInAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return Ok();
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking in appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id}/complete")]
    public async Task<IActionResult> Complete(int id)
    {
        try
        {
            var success = await _donationAppointmentService.CompleteAppointmentAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return Ok();
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error completing appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id}/cancel")]
    public async Task<IActionResult> Cancel(int id, [FromBody] string? reason = null)
    {
        try
        {
            var success = await _donationAppointmentService.CancelAppointmentAsync(id, reason);
            if (!success)
            {
                return NotFound();
            }

            return Ok();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error cancelling appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id}/reschedule")]
    public async Task<ActionResult<DonationAppointmentDto>> Reschedule(int id, [FromBody] RescheduleAppointmentDto rescheduleDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var appointment = await _donationAppointmentService.RescheduleAppointmentAsync(id, rescheduleDto.NewDateTime, rescheduleDto.Reason);
            if (appointment == null)
            {
                return NotFound();
            }

            return Ok(appointment);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error rescheduling appointment {AppointmentId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("stats")]
    public async Task<ActionResult<AppointmentStatsDto>> GetStats()
    {
        try
        {
            var stats = await _donationAppointmentService.GetStatsAsync();
            return Ok(stats);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving appointment statistics");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("send-reminders")]
    [Authorize(Roles = "Admin,Manager")]
    public async Task<IActionResult> SendReminders()
    {
        try
        {
            await _donationAppointmentService.SendRemindersAsync();
            return Ok();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending appointment reminders");
            return StatusCode(500, "Internal server error");
        }
    }
}