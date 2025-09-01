using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rubia.Server.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddWhatsAppAndMessagingTables : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Create WhatsApp Instances table (V44)
            migrationBuilder.CreateTable(
                name: "whatsapp_instances",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    phone_number = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    instance_name = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    external_instance_id = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    provider = table.Column<string>(type: "text", nullable: false, defaultValue: "Z_API"),
                    access_token = table.Column<string>(type: "text", nullable: true),
                    webhook_url = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    status = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false, defaultValue: "NOT_CONFIGURED"),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    is_primary = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    last_connected_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    last_status_check = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    configuration_data = table.Column<string>(type: "text", nullable: true),
                    error_message = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_whatsapp_instances", x => x.id);
                    table.ForeignKey(
                        name: "FK_whatsapp_instances_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.CheckConstraint("CK_whatsapp_instances_provider", "provider IN ('Z_API', 'TWILIO', 'WHATSAPP_BUSINESS_API', 'MOCK')");
                    table.CheckConstraint("CK_whatsapp_instances_status", "status IN ('NOT_CONFIGURED', 'CONFIGURING', 'AWAITING_QR_SCAN', 'CONNECTING', 'CONNECTED', 'DISCONNECTED', 'ERROR', 'SUSPENDED')");
                });

            // Create Message Templates table (V10)
            migrationBuilder.CreateTable(
                name: "message_templates",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    name = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    content = table.Column<string>(type: "text", nullable: false),
                    description = table.Column<string>(type: "text", nullable: true),
                    category = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    deleted_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_message_templates", x => x.id);
                    table.ForeignKey(
                        name: "FK_message_templates_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            // Create Campaigns table (V12)
            migrationBuilder.CreateTable(
                name: "campaigns",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    message_template_id = table.Column<Guid>(type: "uuid", nullable: true),
                    name = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    description = table.Column<string>(type: "text", nullable: true),
                    status = table.Column<string>(type: "text", nullable: false, defaultValue: "DRAFT"),
                    scheduled_date = table.Column<DateOnly>(type: "date", nullable: true),
                    start_date = table.Column<DateOnly>(type: "date", nullable: true),
                    end_date = table.Column<DateOnly>(type: "date", nullable: true),
                    target_audience_criteria = table.Column<string>(type: "text", nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_campaigns", x => x.id);
                    table.ForeignKey(
                        name: "FK_campaigns_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_campaigns_message_templates_message_template_id",
                        column: x => x.message_template_id,
                        principalTable: "message_templates",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Campaign Contacts table (V13)
            migrationBuilder.CreateTable(
                name: "campaign_contacts",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    campaign_id = table.Column<Guid>(type: "uuid", nullable: false),
                    customer_id = table.Column<Guid>(type: "uuid", nullable: false),
                    status = table.Column<string>(type: "text", nullable: false, defaultValue: "PENDING"),
                    sent_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    delivered_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    read_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    responded_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    error_message = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_campaign_contacts", x => x.id);
                    table.ForeignKey(
                        name: "FK_campaign_contacts_campaigns_campaign_id",
                        column: x => x.campaign_id,
                        principalTable: "campaigns",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_campaign_contacts_customers_customer_id",
                        column: x => x.customer_id,
                        principalTable: "customers",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            // Create Conversations table (V14)
            migrationBuilder.CreateTable(
                name: "conversations",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    customer_id = table.Column<Guid>(type: "uuid", nullable: false),
                    campaign_id = table.Column<Guid>(type: "uuid", nullable: true),
                    assigned_user_id = table.Column<Guid>(type: "uuid", nullable: true),
                    owner_user_id = table.Column<Guid>(type: "uuid", nullable: true),
                    external_id = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    chat_lid = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    channel = table.Column<string>(type: "text", nullable: false, defaultValue: "WHATSAPP"),
                    conversation_type = table.Column<string>(type: "text", nullable: false, defaultValue: "ONE_TO_ONE"),
                    status = table.Column<string>(type: "text", nullable: false, defaultValue: "OPEN"),
                    ai_auto_response_enabled = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    ai_message_limit_per_day = table.Column<int>(type: "integer", nullable: true),
                    ai_messages_sent_today = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    ai_message_limit_reset_date = table.Column<DateOnly>(type: "date", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_conversations", x => x.id);
                    table.ForeignKey(
                        name: "FK_conversations_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_conversations_customers_customer_id",
                        column: x => x.customer_id,
                        principalTable: "customers",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_conversations_campaigns_campaign_id",
                        column: x => x.campaign_id,
                        principalTable: "campaigns",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_conversations_users_assigned_user_id",
                        column: x => x.assigned_user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_conversations_users_owner_user_id",
                        column: x => x.owner_user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Messages table (V17)
            migrationBuilder.CreateTable(
                name: "messages",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    content = table.Column<string>(type: "text", nullable: true),
                    sender_type = table.Column<string>(type: "text", nullable: false),
                    sender_id = table.Column<Guid>(type: "uuid", nullable: true),
                    status = table.Column<string>(type: "text", nullable: false, defaultValue: "SENT"),
                    delivered_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    read_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    external_message_id = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    is_ai_generated = table.Column<bool>(type: "boolean", nullable: true),
                    ai_confidence = table.Column<double>(type: "double precision", nullable: true),
                    sentiment = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    keywords = table.Column<string>(type: "text", nullable: true),
                    ai_agent_id = table.Column<Guid>(type: "uuid", nullable: true),
                    message_template_id = table.Column<Guid>(type: "uuid", nullable: true),
                    campaign_contact_id = table.Column<Guid>(type: "uuid", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_messages", x => x.id);
                    table.ForeignKey(
                        name: "FK_messages_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_messages_ai_agents_ai_agent_id",
                        column: x => x.ai_agent_id,
                        principalTable: "ai_agents",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_messages_message_templates_message_template_id",
                        column: x => x.message_template_id,
                        principalTable: "message_templates",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_messages_campaign_contacts_campaign_contact_id",
                        column: x => x.campaign_contact_id,
                        principalTable: "campaign_contacts",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Chat Lid Mappings table (V62)
            migrationBuilder.CreateTable(
                name: "chat_lid_mappings",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    chat_lid = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    phone = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    whatsapp_instance_id = table.Column<Guid>(type: "uuid", nullable: true),
                    from_campaign = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_chat_lid_mappings", x => x.id);
                    table.ForeignKey(
                        name: "FK_chat_lid_mappings_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_chat_lid_mappings_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_chat_lid_mappings_whatsapp_instances_whatsapp_instance_id",
                        column: x => x.whatsapp_instance_id,
                        principalTable: "whatsapp_instances",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create triggers
            migrationBuilder.Sql("CREATE TRIGGER update_whatsapp_instances_updated_at BEFORE UPDATE ON whatsapp_instances FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_message_templates_updated_at BEFORE UPDATE ON message_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_campaigns_updated_at BEFORE UPDATE ON campaigns FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_chat_lid_mappings_updated_at BEFORE UPDATE ON chat_lid_mappings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");

            // Create indexes
            migrationBuilder.CreateIndex(
                name: "IX_whatsapp_instances_company_id_phone_number",
                table: "whatsapp_instances",
                columns: new[] { "company_id", "phone_number" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_whatsapp_instances_external_instance_id",
                table: "whatsapp_instances",
                column: "external_instance_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_conversations_chat_lid",
                table: "conversations",
                column: "chat_lid",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_messages_external_message_id",
                table: "messages",
                column: "external_message_id");

            migrationBuilder.CreateIndex(
                name: "IX_messages_conversation_id_created_at",
                table: "messages",
                columns: new[] { "conversation_id", "created_at" });

            migrationBuilder.CreateIndex(
                name: "IX_chat_lid_mappings_chat_lid",
                table: "chat_lid_mappings",
                column: "chat_lid",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_chat_lid_mappings_conversation_id_phone",
                table: "chat_lid_mappings",
                columns: new[] { "conversation_id", "phone" });

            migrationBuilder.CreateIndex(
                name: "IX_campaign_contacts_campaign_id_customer_id",
                table: "campaign_contacts",
                columns: new[] { "campaign_id", "customer_id" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(name: "chat_lid_mappings");
            migrationBuilder.DropTable(name: "messages");
            migrationBuilder.DropTable(name: "conversations");
            migrationBuilder.DropTable(name: "campaign_contacts");
            migrationBuilder.DropTable(name: "campaigns");
            migrationBuilder.DropTable(name: "message_templates");
            migrationBuilder.DropTable(name: "whatsapp_instances");
        }
    }
}