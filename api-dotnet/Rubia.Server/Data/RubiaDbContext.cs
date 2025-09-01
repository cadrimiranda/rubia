using Microsoft.EntityFrameworkCore;
using Rubia.Server.Entities;

namespace Rubia.Server.Data;

public class RubiaDbContext : DbContext
{
    public RubiaDbContext(DbContextOptions<RubiaDbContext> options) : base(options)
    {
    }

    // DbSets para cada entidade
    public DbSet<CompanyGroup> CompanyGroups { get; set; }
    public DbSet<Company> Companies { get; set; }
    public DbSet<Department> Departments { get; set; }
    public DbSet<User> Users { get; set; }
    public DbSet<Customer> Customers { get; set; }
    public DbSet<WhatsAppInstance> WhatsAppInstances { get; set; }
    public DbSet<AIModel> AIModels { get; set; }
    public DbSet<AIAgent> AIAgents { get; set; }
    public DbSet<MessageTemplate> MessageTemplates { get; set; }
    public DbSet<MessageTemplateRevision> MessageTemplateRevisions { get; set; }
    public DbSet<Campaign> Campaigns { get; set; }
    public DbSet<CampaignContact> CampaignContacts { get; set; }
    public DbSet<Conversation> Conversations { get; set; }
    public DbSet<ConversationParticipant> ConversationParticipants { get; set; }
    public DbSet<ConversationMedia> ConversationMedia { get; set; }
    public DbSet<Message> Messages { get; set; }
    public DbSet<UnreadMessageCount> UnreadMessageCounts { get; set; }
    public DbSet<MessageDraft> MessageDrafts { get; set; }
    public DbSet<FAQ> FAQs { get; set; }
    public DbSet<AudioMessage> AudioMessages { get; set; }
    public DbSet<AILog> AILogs { get; set; }
    public DbSet<UserAIAgent> UserAIAgents { get; set; }
    public DbSet<DonationAppointment> DonationAppointments { get; set; }
    public DbSet<ChatLidMapping> ChatLidMappings { get; set; }
    public DbSet<ConversationLastMessage> ConversationLastMessages { get; set; }
    public DbSet<MessageEnhancementAudit> MessageEnhancementAudits { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // Configure relationships and constraints
        ConfigureCompanyGroup(modelBuilder);
        ConfigureCompany(modelBuilder);
        ConfigureDepartment(modelBuilder);
        ConfigureUser(modelBuilder);
        ConfigureCustomer(modelBuilder);
        ConfigureWhatsAppInstance(modelBuilder);
        ConfigureAIModel(modelBuilder);
        ConfigureAIAgent(modelBuilder);
        ConfigureMessageTemplate(modelBuilder);
        ConfigureMessageTemplateRevision(modelBuilder);
        ConfigureCampaign(modelBuilder);
        ConfigureCampaignContact(modelBuilder);
        ConfigureConversation(modelBuilder);
        ConfigureConversationParticipant(modelBuilder);
        ConfigureConversationMedia(modelBuilder);
        ConfigureMessage(modelBuilder);
        ConfigureAudioMessage(modelBuilder);
        ConfigureAILog(modelBuilder);
        ConfigureUserAIAgent(modelBuilder);
        ConfigureDonationAppointment(modelBuilder);
        ConfigureChatLidMapping(modelBuilder);
        ConfigureConversationLastMessage(modelBuilder);
        ConfigureMessageEnhancementAudit(modelBuilder);

        // Configure enum storage as strings for PostgreSQL
        ConfigureEnums(modelBuilder);
    }

    private void ConfigureCompanyGroup(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<CompanyGroup>(entity =>
        {
            entity.HasIndex(e => e.Name).IsUnique();
            
            entity.HasMany(e => e.Companies)
                .WithOne(e => e.CompanyGroup)
                .HasForeignKey(e => e.CompanyGroupId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private void ConfigureCompany(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Company>(entity =>
        {
            entity.HasIndex(e => e.Slug).IsUnique();
            
            entity.HasMany(e => e.Departments)
                .WithOne(e => e.Company)
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.WhatsappInstances)
                .WithOne(e => e.Company)
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Users)
                .WithOne(e => e.Company)
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasMany(e => e.Conversations)
                .WithOne(e => e.Company)
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private void ConfigureDepartment(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Department>(entity =>
        {
            entity.HasMany(e => e.Users)
                .WithOne(e => e.Department)
                .HasForeignKey(e => e.DepartmentId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private void ConfigureUser(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(e => e.Email).IsUnique();
            entity.HasIndex(e => e.WhatsappNumber).IsUnique();

            entity.HasMany(e => e.AssignedConversations)
                .WithOne(e => e.AssignedUser)
                .HasForeignKey(e => e.AssignedUserId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasMany(e => e.OwnedConversations)
                .WithOne(e => e.OwnerUser)
                .HasForeignKey(e => e.OwnerUserId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureCustomer(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Customer>(entity =>
        {
            entity.HasIndex(e => new { e.CompanyId, e.Phone }).IsUnique();
        });
    }

    private void ConfigureWhatsAppInstance(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<WhatsAppInstance>(entity =>
        {
            entity.HasIndex(e => new { e.CompanyId, e.PhoneNumber }).IsUnique();
        });
    }

    private void ConfigureAIModel(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AIModel>(entity =>
        {
            entity.HasIndex(e => e.Name).IsUnique();
            
            entity.HasMany(e => e.Agents)
                .WithOne(e => e.AiModel)
                .HasForeignKey(e => e.AiModelId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private void ConfigureAIAgent(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AIAgent>(entity =>
        {
            entity.HasMany(e => e.Messages)
                .WithOne(e => e.AiAgent)
                .HasForeignKey(e => e.AiAgentId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasMany(e => e.ConversationParticipants)
                .WithOne(e => e.AiAgent)
                .HasForeignKey(e => e.AiAgentId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureMessageTemplate(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<MessageTemplate>(entity =>
        {
            entity.HasMany(e => e.Revisions)
                .WithOne(e => e.Template)
                .HasForeignKey(e => e.TemplateId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Messages)
                .WithOne(e => e.MessageTemplate)
                .HasForeignKey(e => e.MessageTemplateId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasMany(e => e.CampaignsAsInitialTemplate)
                .WithOne(e => e.InitialMessageTemplate)
                .HasForeignKey(e => e.MessageTemplateId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureMessageTemplateRevision(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<MessageTemplateRevision>(entity =>
        {
            entity.HasIndex(e => new { e.TemplateId, e.RevisionNumber }).IsUnique();
        });
    }

    private void ConfigureCampaign(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Campaign>(entity =>
        {
            entity.HasMany(e => e.CampaignContacts)
                .WithOne(e => e.Campaign)
                .HasForeignKey(e => e.CampaignId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Conversations)
                .WithOne(e => e.Campaign)
                .HasForeignKey(e => e.CampaignId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureCampaignContact(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<CampaignContact>(entity =>
        {
            entity.HasIndex(e => new { e.CampaignId, e.CustomerId }).IsUnique();

            entity.HasMany(e => e.Messages)
                .WithOne(e => e.CampaignContact)
                .HasForeignKey(e => e.CampaignContactId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureConversation(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Conversation>(entity =>
        {
            entity.HasIndex(e => e.ChatLid).IsUnique();

            entity.HasMany(e => e.Participants)
                .WithOne(e => e.Conversation)
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(e => e.Messages)
                .WithOne(e => e.Conversation)
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private void ConfigureConversationParticipant(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ConversationParticipant>(entity =>
        {
            // Garantir que apenas um dos participantes seja não-nulo
            entity.ToTable(t => t.HasCheckConstraint("CK_ConversationParticipant_OneParticipant",
                "(CASE WHEN customer_id IS NOT NULL THEN 1 ELSE 0 END + " +
                "CASE WHEN user_id IS NOT NULL THEN 1 ELSE 0 END + " +
                "CASE WHEN ai_agent_id IS NOT NULL THEN 1 ELSE 0 END) = 1"));
        });
    }

    private void ConfigureConversationMedia(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ConversationMedia>(entity =>
        {
            entity.HasMany(e => e.Messages)
                .WithOne(e => e.Media)
                .HasForeignKey(e => e.ConversationMediaId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureMessage(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Message>(entity =>
        {
            entity.HasIndex(e => e.ExternalMessageId);
            entity.HasIndex(e => new { e.ConversationId, e.CreatedAt });
        });
    }

    private void ConfigureAudioMessage(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AudioMessage>(entity =>
        {
            entity.HasIndex(e => e.MessageId).IsUnique();
            entity.HasIndex(e => e.CreatedAt);
            
            entity.HasOne(e => e.Conversation)
                .WithMany()
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureAILog(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AILog>(entity =>
        {
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => new { e.CompanyId, e.Status });

            entity.HasOne(e => e.Company)
                .WithMany()
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.AiAgent)
                .WithMany()
                .HasForeignKey(e => e.AiAgentId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(e => e.User)
                .WithMany()
                .HasForeignKey(e => e.UserId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(e => e.Conversation)
                .WithMany()
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(e => e.Message)
                .WithMany()
                .HasForeignKey(e => e.MessageId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(e => e.MessageTemplate)
                .WithMany()
                .HasForeignKey(e => e.MessageTemplateId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.Property(e => e.EstimatedCost)
                .HasPrecision(10, 8);
        });
    }

    private void ConfigureUserAIAgent(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserAIAgent>(entity =>
        {
            entity.HasIndex(e => new { e.UserId, e.AiAgentId }).IsUnique();
            entity.HasIndex(e => e.CreatedAt);

            entity.HasOne(e => e.Company)
                .WithMany()
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.User)
                .WithMany()
                .HasForeignKey(e => e.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.AiAgent)
                .WithMany()
                .HasForeignKey(e => e.AiAgentId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private void ConfigureDonationAppointment(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<DonationAppointment>(entity =>
        {
            entity.HasIndex(e => e.ExternalAppointmentId);
            entity.HasIndex(e => e.AppointmentDateTime);
            entity.HasIndex(e => new { e.CompanyId, e.Status });

            entity.HasOne(e => e.Company)
                .WithMany()
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.Customer)
                .WithMany()
                .HasForeignKey(e => e.CustomerId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.Conversation)
                .WithMany()
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureChatLidMapping(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ChatLidMapping>(entity =>
        {
            entity.HasIndex(e => e.ChatLid).IsUnique();
            entity.HasIndex(e => new { e.ConversationId, e.Phone });
            entity.HasIndex(e => new { e.CompanyId, e.Phone });
            entity.HasIndex(e => e.WhatsAppInstanceId);
            entity.HasIndex(e => new { e.FromCampaign, e.CompanyId });
            entity.HasIndex(e => e.CreatedAt);

            entity.HasOne(e => e.Conversation)
                .WithMany()
                .HasForeignKey(e => e.ConversationId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.Company)
                .WithMany()
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.WhatsAppInstance)
                .WithMany()
                .HasForeignKey(e => e.WhatsAppInstanceId)
                .OnDelete(DeleteBehavior.SetNull);
        });
    }

    private void ConfigureConversationLastMessage(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ConversationLastMessage>(entity =>
        {
            entity.HasIndex(e => e.ConversationId).IsUnique();
            entity.HasIndex(e => e.MessageCreatedAt);

            entity.HasOne(e => e.Conversation)
                .WithOne()
                .HasForeignKey<ConversationLastMessage>(e => e.ConversationId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.Message)
                .WithMany()
                .HasForeignKey(e => e.MessageId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private void ConfigureMessageEnhancementAudit(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<MessageEnhancementAudit>(entity =>
        {
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => new { e.CompanyId, e.Success });
            entity.HasIndex(e => e.UserId);

            entity.HasOne(e => e.Company)
                .WithMany()
                .HasForeignKey(e => e.CompanyId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.User)
                .WithMany()
                .HasForeignKey(e => e.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(e => e.AiAgent)
                .WithMany()
                .HasForeignKey(e => e.AiAgentId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(e => e.AiModel)
                .WithMany()
                .HasForeignKey(e => e.AiModelId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(e => e.MessageTemplate)
                .WithMany()
                .HasForeignKey(e => e.MessageTemplateId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.Property(e => e.CostEstimate)
                .HasPrecision(10, 8);
        });
    }

    private void ConfigureEnums(ModelBuilder modelBuilder)
    {
        // Configure all enums to be stored as strings in PostgreSQL
        modelBuilder.Entity<User>()
            .Property(e => e.Role)
            .HasConversion<string>();

        modelBuilder.Entity<Company>()
            .Property(e => e.PlanType)
            .HasConversion<string>();

        modelBuilder.Entity<WhatsAppInstance>()
            .Property(e => e.Provider)
            .HasConversion<string>();

        modelBuilder.Entity<Campaign>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<CampaignContact>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<Conversation>()
            .Property(e => e.Channel)
            .HasConversion<string>();

        modelBuilder.Entity<Conversation>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<Conversation>()
            .Property(e => e.ConversationType)
            .HasConversion<string>();

        modelBuilder.Entity<ConversationMedia>()
            .Property(e => e.MediaType)
            .HasConversion<string>();

        modelBuilder.Entity<Message>()
            .Property(e => e.SenderType)
            .HasConversion<string>();

        modelBuilder.Entity<Message>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<MessageTemplateRevision>()
            .Property(e => e.RevisionType)
            .HasConversion<string>();

        modelBuilder.Entity<AudioMessage>()
            .Property(e => e.Direction)
            .HasConversion<string>();

        modelBuilder.Entity<AudioMessage>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<AILog>()
            .Property(e => e.Status)
            .HasConversion<string>();

        modelBuilder.Entity<DonationAppointment>()
            .Property(e => e.Status)
            .HasConversion<string>();
    }

    public override int SaveChanges()
    {
        UpdateTimestamps();
        return base.SaveChanges();
    }

    public override async Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        UpdateTimestamps();
        return await base.SaveChangesAsync(cancellationToken);
    }

    private void UpdateTimestamps()
    {
        var entries = ChangeTracker
            .Entries()
            .Where(e => e.Entity is BaseEntity && (e.State == EntityState.Added || e.State == EntityState.Modified));

        foreach (var entityEntry in entries)
        {
            var entity = (BaseEntity)entityEntry.Entity;

            if (entityEntry.State == EntityState.Added)
            {
                entity.CreatedAt = DateTime.UtcNow;
            }

            entity.UpdatedAt = DateTime.UtcNow;
        }
    }
}