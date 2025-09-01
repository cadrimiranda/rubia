using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rubia.Server.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddAdvancedMessagingTables : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Create Conversation Media table (V19)
            migrationBuilder.CreateTable(
                name: "conversation_media",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    message_id = table.Column<Guid>(type: "uuid", nullable: true),
                    file_name = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    original_file_name = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    file_type = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    file_size = table.Column<long>(type: "bigint", nullable: false),
                    file_path = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    mime_type = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    caption = table.Column<string>(type: "text", nullable: true),
                    external_media_id = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    download_url = table.Column<string>(type: "text", nullable: true),
                    thumbnail_path = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    duration_seconds = table.Column<int>(type: "integer", nullable: true),
                    width = table.Column<int>(type: "integer", nullable: true),
                    height = table.Column<int>(type: "integer", nullable: true),
                    is_uploaded = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    upload_progress = table.Column<decimal>(type: "numeric(5,2)", precision: 5, scale: 2, nullable: true),
                    error_message = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_conversation_media", x => x.id);
                    table.ForeignKey(
                        name: "FK_conversation_media_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_conversation_media_messages_message_id",
                        column: x => x.message_id,
                        principalTable: "messages",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Company API Settings table (V33)
            migrationBuilder.CreateTable(
                name: "company_api_settings",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    openai_api_key = table.Column<string>(type: "text", nullable: true),
                    openai_model = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true, defaultValue: "gpt-3.5-turbo"),
                    openai_temperature = table.Column<double>(type: "double precision", nullable: false, defaultValue: 0.7),
                    openai_max_tokens = table.Column<int>(type: "integer", nullable: false, defaultValue: 150),
                    twilio_account_sid = table.Column<string>(type: "text", nullable: true),
                    twilio_auth_token = table.Column<string>(type: "text", nullable: true),
                    twilio_phone_number = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    z_api_instance_id = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    z_api_token = table.Column<string>(type: "text", nullable: true),
                    z_api_phone = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    webhook_url = table.Column<string>(type: "text", nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_company_api_settings", x => x.id);
                    table.ForeignKey(
                        name: "FK_company_api_settings_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            // Create FAQs table (V67)
            migrationBuilder.CreateTable(
                name: "faqs",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    ai_agent_id = table.Column<Guid>(type: "uuid", nullable: true),
                    question = table.Column<string>(type: "text", nullable: false),
                    answer = table.Column<string>(type: "text", nullable: false),
                    keywords = table.Column<string>(type: "text", nullable: true),
                    category = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    priority = table.Column<int>(type: "integer", nullable: false, defaultValue: 1),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    usage_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    last_used_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    similarity_threshold = table.Column<decimal>(type: "numeric(3,2)", precision: 3, scale: 2, nullable: false, defaultValue: 0.8m),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_faqs", x => x.id);
                    table.ForeignKey(
                        name: "FK_faqs_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_faqs_ai_agents_ai_agent_id",
                        column: x => x.ai_agent_id,
                        principalTable: "ai_agents",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create FAQ Usage Log table (V68)
            migrationBuilder.CreateTable(
                name: "faq_usage_log",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    faq_id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    message_id = table.Column<Guid>(type: "uuid", nullable: true),
                    user_question = table.Column<string>(type: "text", nullable: false),
                    similarity_score = table.Column<decimal>(type: "numeric(5,4)", precision: 5, scale: 4, nullable: false),
                    was_helpful = table.Column<bool>(type: "boolean", nullable: true),
                    feedback_comment = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_faq_usage_log", x => x.id);
                    table.ForeignKey(
                        name: "FK_faq_usage_log_faqs_faq_id",
                        column: x => x.faq_id,
                        principalTable: "faqs",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_faq_usage_log_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_faq_usage_log_messages_message_id",
                        column: x => x.message_id,
                        principalTable: "messages",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Audio Messages table (V47)
            migrationBuilder.CreateTable(
                name: "audio_messages",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    message_id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_media_id = table.Column<Guid>(type: "uuid", nullable: false),
                    transcription = table.Column<string>(type: "text", nullable: true),
                    transcription_confidence = table.Column<decimal>(type: "numeric(5,4)", precision: 5, scale: 4, nullable: true),
                    transcription_language = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: true),
                    transcription_status = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false, defaultValue: "PENDING"),
                    transcription_provider = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    transcription_error = table.Column<string>(type: "text", nullable: true),
                    processing_started_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    processing_completed_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_audio_messages", x => x.id);
                    table.ForeignKey(
                        name: "FK_audio_messages_messages_message_id",
                        column: x => x.message_id,
                        principalTable: "messages",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_audio_messages_conversation_media_conversation_media_id",
                        column: x => x.conversation_media_id,
                        principalTable: "conversation_media",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.CheckConstraint("CK_audio_messages_transcription_status", "transcription_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')");
                });

            // Create Conversation Last Messages table (V66)
            migrationBuilder.CreateTable(
                name: "conversation_last_messages",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    last_message_id = table.Column<Guid>(type: "uuid", nullable: true),
                    last_message_content = table.Column<string>(type: "text", nullable: true),
                    last_message_sender_type = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    last_message_created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    unread_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_conversation_last_messages", x => x.id);
                    table.ForeignKey(
                        name: "FK_conversation_last_messages_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_conversation_last_messages_messages_last_message_id",
                        column: x => x.last_message_id,
                        principalTable: "messages",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create Message Enhancement Audit table
            migrationBuilder.CreateTable(
                name: "message_enhancement_audit",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    ai_agent_id = table.Column<Guid>(type: "uuid", nullable: true),
                    ai_model_id = table.Column<Guid>(type: "uuid", nullable: true),
                    message_template_id = table.Column<Guid>(type: "uuid", nullable: true),
                    original_content = table.Column<string>(type: "text", nullable: false),
                    enhanced_content = table.Column<string>(type: "text", nullable: false),
                    system_message = table.Column<string>(type: "text", nullable: true),
                    model_name = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    temperature = table.Column<double>(type: "double precision", nullable: true),
                    max_tokens = table.Column<int>(type: "integer", nullable: true),
                    tokens_used = table.Column<int>(type: "integer", nullable: true),
                    response_time_ms = table.Column<long>(type: "bigint", nullable: true),
                    openai_payload_json = table.Column<string>(type: "text", nullable: true),
                    cost_estimate = table.Column<decimal>(type: "numeric(10,6)", precision: 10, scale: 6, nullable: true),
                    success = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    error_message = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_message_enhancement_audit", x => x.id);
                    table.ForeignKey(
                        name: "FK_message_enhancement_audit_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_message_enhancement_audit_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_message_enhancement_audit_ai_agents_ai_agent_id",
                        column: x => x.ai_agent_id,
                        principalTable: "ai_agents",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_message_enhancement_audit_ai_models_ai_model_id",
                        column: x => x.ai_model_id,
                        principalTable: "ai_models",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_message_enhancement_audit_message_templates_message_template_id",
                        column: x => x.message_template_id,
                        principalTable: "message_templates",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create triggers
            migrationBuilder.Sql("CREATE TRIGGER update_conversation_media_updated_at BEFORE UPDATE ON conversation_media FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_company_api_settings_updated_at BEFORE UPDATE ON company_api_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_faqs_updated_at BEFORE UPDATE ON faqs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_audio_messages_updated_at BEFORE UPDATE ON audio_messages FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_conversation_last_messages_updated_at BEFORE UPDATE ON conversation_last_messages FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_message_enhancement_audit_updated_at BEFORE UPDATE ON message_enhancement_audit FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");

            // Create indexes
            migrationBuilder.CreateIndex(
                name: "IX_conversation_media_conversation_id",
                table: "conversation_media",
                column: "conversation_id");

            migrationBuilder.CreateIndex(
                name: "IX_conversation_media_external_media_id",
                table: "conversation_media",
                column: "external_media_id");

            migrationBuilder.CreateIndex(
                name: "IX_company_api_settings_company_id",
                table: "company_api_settings",
                column: "company_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_faqs_company_id_category",
                table: "faqs",
                columns: new[] { "company_id", "category" });

            migrationBuilder.CreateIndex(
                name: "IX_faqs_ai_agent_id",
                table: "faqs",
                column: "ai_agent_id");

            migrationBuilder.CreateIndex(
                name: "IX_faq_usage_log_faq_id_created_at",
                table: "faq_usage_log",
                columns: new[] { "faq_id", "created_at" });

            migrationBuilder.CreateIndex(
                name: "IX_audio_messages_message_id",
                table: "audio_messages",
                column: "message_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_conversation_last_messages_conversation_id",
                table: "conversation_last_messages",
                column: "conversation_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_message_enhancement_audit_company_id_created_at",
                table: "message_enhancement_audit",
                columns: new[] { "company_id", "created_at" });

            migrationBuilder.CreateIndex(
                name: "IX_message_enhancement_audit_user_id",
                table: "message_enhancement_audit",
                column: "user_id");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(name: "message_enhancement_audit");
            migrationBuilder.DropTable(name: "conversation_last_messages");
            migrationBuilder.DropTable(name: "audio_messages");
            migrationBuilder.DropTable(name: "faq_usage_log");
            migrationBuilder.DropTable(name: "faqs");
            migrationBuilder.DropTable(name: "company_api_settings");
            migrationBuilder.DropTable(name: "conversation_media");
        }
    }
}