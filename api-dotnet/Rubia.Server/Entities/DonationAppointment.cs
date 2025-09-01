using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("donation_appointments")]
public class DonationAppointment : BaseEntity
{
    public Guid CompanyId { get; set; }
    public Company Company { get; set; } = null!;
    public Guid CustomerId { get; set; }
    public Customer Customer { get; set; } = null!;
    public Guid? ConversationId { get; set; }
    public Conversation? Conversation { get; set; }
    public string ExternalAppointmentId { get; set; } = string.Empty;
    public DateTime AppointmentDateTime { get; set; }
    public DonationAppointmentStatus Status { get; set; } = DonationAppointmentStatus.SCHEDULED;
    public string? ConfirmationUrl { get; set; }
    public string? Notes { get; set; }
}