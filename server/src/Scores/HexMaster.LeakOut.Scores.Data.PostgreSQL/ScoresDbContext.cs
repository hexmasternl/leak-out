using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

public sealed class ScoresDbContext(DbContextOptions<ScoresDbContext> options) : DbContext(options)
{
    public DbSet<LevelScoreEntity> LevelScores => Set<LevelScoreEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<LevelScoreEntity>(levelScore =>
        {
            levelScore.ToTable("level_scores", table =>
            {
                table.HasCheckConstraint("ck_level_scores_level", "level >= 1");
                table.HasCheckConstraint("ck_level_scores_score", "score >= 0");
            });
            levelScore.HasKey(s => s.Id).HasName("pk_level_scores");
            levelScore.Property(s => s.Id).HasColumnName("id").ValueGeneratedNever();
            levelScore.Property(s => s.UserId).HasColumnName("user_id");
            levelScore.Property(s => s.Level).HasColumnName("level");
            levelScore.Property(s => s.Score).HasColumnName("score");
            levelScore.Property(s => s.AchievedAt).HasColumnName("achieved_at");

            // One best score per player per level; the upsert in LevelScoreRepository relies on it.
            levelScore.HasIndex(s => new { s.UserId, s.Level })
                .IsUnique()
                .HasDatabaseName("ix_level_scores_user_id_level");

            // Ranks a level's scores for the leaderboards: highest first, earliest achiever wins a tie.
            levelScore.HasIndex(s => new { s.Level, s.Score, s.AchievedAt })
                .IsDescending(false, true, false)
                .HasDatabaseName("ix_level_scores_level_score_achieved_at");
        });
    }
}
