using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "level_scores",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    user_id = table.Column<Guid>(type: "uuid", nullable: false),
                    level = table.Column<int>(type: "integer", nullable: false),
                    score = table.Column<int>(type: "integer", nullable: false),
                    achieved_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_level_scores", x => x.id);
                    table.CheckConstraint("ck_level_scores_level", "level >= 1");
                    table.CheckConstraint("ck_level_scores_score", "score >= 0");
                });

            migrationBuilder.CreateIndex(
                name: "ix_level_scores_level_score_achieved_at",
                table: "level_scores",
                columns: new[] { "level", "score", "achieved_at" },
                descending: new[] { false, true, false });

            migrationBuilder.CreateIndex(
                name: "ix_level_scores_user_id_level",
                table: "level_scores",
                columns: new[] { "user_id", "level" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "level_scores");
        }
    }
}
