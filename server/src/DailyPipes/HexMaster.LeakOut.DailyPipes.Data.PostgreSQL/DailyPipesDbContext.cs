using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

public sealed class DailyPipesDbContext(DbContextOptions<DailyPipesDbContext> options) : DbContext(options)
{
    public DbSet<DailyPipeEntity> DailyPipes => Set<DailyPipeEntity>();

    public DbSet<DailyScoreEntity> DailyScores => Set<DailyScoreEntity>();

    public DbSet<DailyStreakEntity> DailyStreaks => Set<DailyStreakEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<DailyPipeEntity>(pipe =>
        {
            pipe.ToTable("daily_pipes", table =>
            {
                table.HasCheckConstraint("ck_daily_pipes_stack_seed", "stack_seed >= 0");
                table.HasCheckConstraint("ck_daily_pipes_max_score", "max_score >= 1");
            });
            // One pipe per UTC date; the idempotent publication relies on it.
            pipe.HasKey(p => p.Date).HasName("pk_daily_pipes");
            pipe.Property(p => p.Date).HasColumnName("date");
            pipe.Property(p => p.LevelJson).HasColumnName("level_json");
            pipe.Property(p => p.StackSeed).HasColumnName("stack_seed");
            pipe.Property(p => p.MaxScore).HasColumnName("max_score");
            pipe.Property(p => p.PublishedAt).HasColumnName("published_at");
        });

        modelBuilder.Entity<DailyScoreEntity>(score =>
        {
            score.ToTable("daily_scores", table =>
            {
                table.HasCheckConstraint("ck_daily_scores_score", "score >= 0");
                table.HasCheckConstraint("ck_daily_scores_stars", "stars BETWEEN 0 AND 3");
            });
            score.HasKey(s => s.Id).HasName("pk_daily_scores");
            score.Property(s => s.Id).HasColumnName("id").ValueGeneratedNever();
            score.Property(s => s.Date).HasColumnName("date");
            score.Property(s => s.UserId).HasColumnName("user_id");
            score.Property(s => s.Score).HasColumnName("score");
            score.Property(s => s.Stars).HasColumnName("stars");
            score.Property(s => s.AchievedAt).HasColumnName("achieved_at");

            // One best result per player per date; the upsert in DailyPipeRepository relies on it.
            score.HasIndex(s => new { s.Date, s.UserId })
                .IsUnique()
                .HasDatabaseName("ix_daily_scores_date_user_id");

            // Ranks a date's results: highest first, earliest achiever wins a tie.
            score.HasIndex(s => new { s.Date, s.Score, s.AchievedAt })
                .IsDescending(false, true, false)
                .HasDatabaseName("ix_daily_scores_date_score_achieved_at");
        });

        modelBuilder.Entity<DailyStreakEntity>(streak =>
        {
            streak.ToTable("daily_streaks", table =>
            {
                table.HasCheckConstraint("ck_daily_streaks_current", "current_streak >= 0 AND current_streak <= best_streak");
                table.HasCheckConstraint("ck_daily_streaks_last_win", "(current_streak = 0) = (last_win_date IS NULL)");
            });
            streak.HasKey(s => s.UserId).HasName("pk_daily_streaks");
            streak.Property(s => s.UserId).HasColumnName("user_id").ValueGeneratedNever();
            streak.Property(s => s.CurrentStreak).HasColumnName("current_streak");
            streak.Property(s => s.BestStreak).HasColumnName("best_streak");
            streak.Property(s => s.LastWinDate).HasColumnName("last_win_date");
        });
    }
}
