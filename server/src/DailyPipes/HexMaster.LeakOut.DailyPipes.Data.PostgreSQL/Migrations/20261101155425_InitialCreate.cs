using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "daily_pipes",
                columns: table => new
                {
                    date = table.Column<DateOnly>(type: "date", nullable: false),
                    level_json = table.Column<string>(type: "text", nullable: false),
                    stack_seed = table.Column<long>(type: "bigint", nullable: false),
                    max_score = table.Column<int>(type: "integer", nullable: false),
                    published_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_daily_pipes", x => x.date);
                    table.CheckConstraint("ck_daily_pipes_max_score", "max_score >= 1");
                    table.CheckConstraint("ck_daily_pipes_stack_seed", "stack_seed >= 0");
                });

            migrationBuilder.CreateTable(
                name: "daily_scores",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    date = table.Column<DateOnly>(type: "date", nullable: false),
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    score = table.Column<int>(type: "integer", nullable: false),
                    stars = table.Column<int>(type: "integer", nullable: false),
                    achieved_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_daily_scores", x => x.id);
                    table.CheckConstraint("ck_daily_scores_score", "score >= 0");
                    table.CheckConstraint("ck_daily_scores_stars", "stars BETWEEN 0 AND 3");
                });

            migrationBuilder.CreateTable(
                name: "daily_streaks",
                columns: table => new
                {
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    current_streak = table.Column<int>(type: "integer", nullable: false),
                    best_streak = table.Column<int>(type: "integer", nullable: false),
                    last_win_date = table.Column<DateOnly>(type: "date", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_daily_streaks", x => x.user_id);
                    table.CheckConstraint("ck_daily_streaks_current", "current_streak >= 0 AND current_streak <= best_streak");
                    table.CheckConstraint("ck_daily_streaks_last_win", "(current_streak = 0) = (last_win_date IS NULL)");
                });

            migrationBuilder.CreateIndex(
                name: "ix_daily_scores_date_score_achieved_at",
                table: "daily_scores",
                columns: new[] { "date", "score", "achieved_at" },
                descending: new[] { false, true, false });

            migrationBuilder.CreateIndex(
                name: "ix_daily_scores_date_user_id",
                table: "daily_scores",
                columns: new[] { "date", "user_id" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "daily_pipes");

            migrationBuilder.DropTable(
                name: "daily_scores");

            migrationBuilder.DropTable(
                name: "daily_streaks");
        }
    }
}
