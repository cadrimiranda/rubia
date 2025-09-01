using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rubia.Server.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddMessageDraftsAndAnalytics : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Create Message Drafts table (V69)
            migrationBuilder.CreateTable(
                name: "message_drafts",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    content = table.Column<string>(type: "text", nullable: false),
                    message_type = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false, defaultValue: "TEXT"),
                    media_attachments = table.Column<string>(type: "text", nullable: true),
                    is_template_based = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    template_variables = table.Column<string>(type: "text", nullable: true),
                    auto_save_enabled = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    last_auto_save_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    expires_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_message_drafts", x => x.id);
                    table.ForeignKey(
                        name: "FK_message_drafts_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_message_drafts_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.CheckConstraint("CK_message_drafts_message_type", "message_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'DOCUMENT', 'LOCATION', 'CONTACT', 'TEMPLATE')");
                });

            // Create Message Draft History table (V70)
            migrationBuilder.CreateTable(
                name: "message_draft_history",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    draft_id = table.Column<Guid>(type: "uuid", nullable: false),
                    content = table.Column<string>(type: "text", nullable: false),
                    change_type = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    changed_by_user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    version_number = table.Column<int>(type: "integer", nullable: false),
                    change_summary = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    character_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    word_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_message_draft_history", x => x.id);
                    table.ForeignKey(
                        name: "FK_message_draft_history_message_drafts_draft_id",
                        column: x => x.draft_id,
                        principalTable: "message_drafts",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_message_draft_history_users_changed_by_user_id",
                        column: x => x.changed_by_user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.CheckConstraint("CK_message_draft_history_change_type", "change_type IN ('CREATED', 'UPDATED', 'AUTO_SAVED', 'RESTORED', 'DELETED')");
                });

            // Create Conversation Analytics table (V64 equivalent)
            migrationBuilder.CreateTable(
                name: "conversation_analytics",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    total_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    customer_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    agent_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    ai_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    unread_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    media_messages = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    avg_response_time_minutes = table.Column<decimal>(type: "numeric(10,2)", precision: 10, scale: 2, nullable: true),
                    first_response_time_minutes = table.Column<decimal>(type: "numeric(10,2)", precision: 10, scale: 2, nullable: true),
                    resolution_time_minutes = table.Column<decimal>(type: "numeric(10,2)", precision: 10, scale: 2, nullable: true),
                    customer_satisfaction_rating = table.Column<int>(type: "integer", nullable: true),
                    sentiment_positive_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    sentiment_negative_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    sentiment_neutral_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    peak_activity_hour = table.Column<int>(type: "integer", nullable: true),
                    tags = table.Column<string>(type: "text", nullable: true),
                    last_calculated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_conversation_analytics", x => x.id);
                    table.ForeignKey(
                        name: "FK_conversation_analytics_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_conversation_analytics_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.CheckConstraint("CK_conversation_analytics_satisfaction_rating", "customer_satisfaction_rating >= 1 AND customer_satisfaction_rating <= 5");
                    table.CheckConstraint("CK_conversation_analytics_peak_hour", "peak_activity_hour >= 0 AND peak_activity_hour <= 23");
                });

            // Create User Activity Log table (V63 equivalent)
            migrationBuilder.CreateTable(
                name: "user_activity_log",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    activity_type = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    activity_description = table.Column<string>(type: "text", nullable: true),
                    conversation_id = table.Column<Guid>(type: "uuid", nullable: true),
                    message_id = table.Column<Guid>(type: "uuid", nullable: true),
                    campaign_id = table.Column<Guid>(type: "uuid", nullable: true),
                    ip_address = table.Column<string>(type: "character varying(45)", maxLength: 45, nullable: true),
                    user_agent = table.Column<string>(type: "text", nullable: true),
                    session_id = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    request_duration_ms = table.Column<long>(type: "bigint", nullable: true),
                    success = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    error_message = table.Column<string>(type: "text", nullable: true),
                    additional_data = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_user_activity_log", x => x.id);
                    table.ForeignKey(
                        name: "FK_user_activity_log_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_user_activity_log_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_user_activity_log_conversations_conversation_id",
                        column: x => x.conversation_id,
                        principalTable: "conversations",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_user_activity_log_messages_message_id",
                        column: x => x.message_id,
                        principalTable: "messages",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_user_activity_log_campaigns_campaign_id",
                        column: x => x.campaign_id,
                        principalTable: "campaigns",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            // Create AI Agent Usage Stats table (V72-V75 equivalent)
            migrationBuilder.CreateTable(
                name: "ai_agent_usage_stats",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    ai_agent_id = table.Column<Guid>(type: "uuid", nullable: false),
                    company_id = table.Column<Guid>(type: "uuid", nullable: false),
                    date = table.Column<DateOnly>(type: "date", nullable: false),
                    messages_generated = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    tokens_used = table.Column<long>(type: "bigint", nullable: false, defaultValue: 0),
                    api_calls_made = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    total_cost = table.Column<decimal>(type: "numeric(12,6)", precision: 12, scale: 6, nullable: false, defaultValue: 0),
                    avg_response_time_ms = table.Column<decimal>(type: "numeric(10,2)", precision: 10, scale: 2, nullable: true),
                    success_rate = table.Column<decimal>(type: "numeric(5,4)", precision: 5, scale: 4, nullable: true),
                    conversations_handled = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    user_satisfaction_avg = table.Column<decimal>(type: "numeric(3,2)", precision: 3, scale: 2, nullable: true),
                    error_count = table.Column<int>(type: "integer", nullable: false, defaultValue: 0),
                    peak_usage_hour = table.Column<int>(type: "integer", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: false, defaultValueSql: "CURRENT_TIMESTAMP"),
                    updated_at = table.Column<DateTime>(type: "timestamp without time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ai_agent_usage_stats", x => x.id);
                    table.ForeignKey(
                        name: "FK_ai_agent_usage_stats_ai_agents_ai_agent_id",
                        column: x => x.ai_agent_id,
                        principalTable: "ai_agents",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_ai_agent_usage_stats_companies_company_id",
                        column: x => x.company_id,
                        principalTable: "companies",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.CheckConstraint("CK_ai_agent_usage_stats_satisfaction", "user_satisfaction_avg >= 1.0 AND user_satisfaction_avg <= 5.0");
                    table.CheckConstraint("CK_ai_agent_usage_stats_success_rate", "success_rate >= 0.0 AND success_rate <= 1.0");
                    table.CheckConstraint("CK_ai_agent_usage_stats_peak_hour", "peak_usage_hour >= 0 AND peak_usage_hour <= 23");
                });

            // Create triggers
            migrationBuilder.Sql("CREATE TRIGGER update_message_drafts_updated_at BEFORE UPDATE ON message_drafts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_conversation_analytics_updated_at BEFORE UPDATE ON conversation_analytics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");
            migrationBuilder.Sql("CREATE TRIGGER update_ai_agent_usage_stats_updated_at BEFORE UPDATE ON ai_agent_usage_stats FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();");

            // Create indexes
            migrationBuilder.CreateIndex(
                name: "IX_message_drafts_conversation_id_user_id",
                table: "message_drafts",
                columns: new[] { "conversation_id", "user_id" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_message_drafts_expires_at",
                table: "message_drafts",
                column: "expires_at");

            migrationBuilder.CreateIndex(
                name: "IX_message_draft_history_draft_id_version_number",
                table: "message_draft_history",
                columns: new[] { "draft_id", "version_number" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_conversation_analytics_conversation_id",
                table: "conversation_analytics",
                column: "conversation_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_conversation_analytics_company_id_last_calculated_at",
                table: "conversation_analytics",
                columns: new[] { "company_id", "last_calculated_at" });

            migrationBuilder.CreateIndex(
                name: "IX_user_activity_log_user_id_created_at",
                table: "user_activity_log",
                columns: new[] { "user_id", "created_at" });

            migrationBuilder.CreateIndex(
                name: "IX_user_activity_log_company_id_activity_type",
                table: "user_activity_log",
                columns: new[] { "company_id", "activity_type" });

            migrationBuilder.CreateIndex(
                name: "IX_user_activity_log_session_id",
                table: "user_activity_log",
                column: "session_id");

            migrationBuilder.CreateIndex(
                name: "IX_ai_agent_usage_stats_ai_agent_id_date",
                table: "ai_agent_usage_stats",
                columns: new[] { "ai_agent_id", "date" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_ai_agent_usage_stats_company_id_date",
                table: "ai_agent_usage_stats",
                columns: new[] { "company_id", "date" });

            // Create function to automatically cleanup expired drafts
            migrationBuilder.Sql(@"
                CREATE OR REPLACE FUNCTION cleanup_expired_drafts()
                RETURNS void AS $$
                BEGIN
                    DELETE FROM message_drafts 
                    WHERE expires_at IS NOT NULL AND expires_at < NOW();
                END;
                $$ LANGUAGE plpgsql;
            ");

            // Create function to update conversation analytics
            migrationBuilder.Sql(@"
                CREATE OR REPLACE FUNCTION update_conversation_analytics(conversation_uuid uuid)
                RETURNS void AS $$
                DECLARE
                    analytics_record RECORD;
                    msg_counts RECORD;
                    sentiment_counts RECORD;
                BEGIN
                    -- Get message counts
                    SELECT 
                        COUNT(*) as total,
                        COUNT(CASE WHEN sender_type = 'CUSTOMER' THEN 1 END) as customer,
                        COUNT(CASE WHEN sender_type = 'USER' THEN 1 END) as agent,
                        COUNT(CASE WHEN is_ai_generated = true THEN 1 END) as ai,
                        COUNT(CASE WHEN read_at IS NULL THEN 1 END) as unread
                    INTO msg_counts
                    FROM messages 
                    WHERE conversation_id = conversation_uuid;

                    -- Get sentiment counts
                    SELECT 
                        COUNT(CASE WHEN sentiment = 'POSITIVO' THEN 1 END) as positive,
                        COUNT(CASE WHEN sentiment = 'NEGATIVO' THEN 1 END) as negative,
                        COUNT(CASE WHEN sentiment = 'NEUTRO' THEN 1 END) as neutral
                    INTO sentiment_counts
                    FROM messages 
                    WHERE conversation_id = conversation_uuid AND sentiment IS NOT NULL;

                    -- Update or insert analytics
                    INSERT INTO conversation_analytics (
                        id, conversation_id, company_id, total_messages, customer_messages,
                        agent_messages, ai_messages, unread_messages, sentiment_positive_count,
                        sentiment_negative_count, sentiment_neutral_count, last_calculated_at
                    )
                    SELECT 
                        gen_random_uuid(),
                        conversation_uuid,
                        c.company_id,
                        msg_counts.total,
                        msg_counts.customer,
                        msg_counts.agent,
                        msg_counts.ai,
                        msg_counts.unread,
                        sentiment_counts.positive,
                        sentiment_counts.negative,
                        sentiment_counts.neutral,
                        NOW()
                    FROM conversations c
                    WHERE c.id = conversation_uuid
                    ON CONFLICT (conversation_id) DO UPDATE SET
                        total_messages = EXCLUDED.total_messages,
                        customer_messages = EXCLUDED.customer_messages,
                        agent_messages = EXCLUDED.agent_messages,
                        ai_messages = EXCLUDED.ai_messages,
                        unread_messages = EXCLUDED.unread_messages,
                        sentiment_positive_count = EXCLUDED.sentiment_positive_count,
                        sentiment_negative_count = EXCLUDED.sentiment_negative_count,
                        sentiment_neutral_count = EXCLUDED.sentiment_neutral_count,
                        last_calculated_at = NOW(),
                        updated_at = NOW();
                END;
                $$ LANGUAGE plpgsql;
            ");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("DROP FUNCTION IF EXISTS update_conversation_analytics(uuid);");
            migrationBuilder.Sql("DROP FUNCTION IF EXISTS cleanup_expired_drafts();");
            
            migrationBuilder.DropTable(name: "ai_agent_usage_stats");
            migrationBuilder.DropTable(name: "user_activity_log");
            migrationBuilder.DropTable(name: "conversation_analytics");
            migrationBuilder.DropTable(name: "message_draft_history");
            migrationBuilder.DropTable(name: "message_drafts");
        }
    }
}