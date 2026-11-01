using HexMaster.LeakOut.DailyPipes.DomainModels;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

public sealed class DailyPipeRepository(DailyPipesDbContext dbContext) : IDailyPipeRepository
{
    public async Task<DailyPipe?> GetPipeAsync(DateOnly date, CancellationToken ct)
    {
        var entity = await dbContext.DailyPipes.AsNoTracking().SingleOrDefaultAsync(p => p.Date == date, ct);
        return entity is null
            ? null
            : DailyPipe.Create(entity.Date, entity.LevelJson, entity.StackSeed, entity.MaxScore, entity.PublishedAt);
    }

    /// <remarks>
    /// <c>ON CONFLICT DO NOTHING</c> on the date key makes a rerun, or two runs at once, publish one
    /// pipe; the cleanup only runs with the insert that won, in its transaction. The transaction runs
    /// inside the execution strategy, because the Aspire context retries on transient failures.
    /// </remarks>
    public Task<bool> PublishAsync(DailyPipe pipe, DateOnly keepPipesFrom, CancellationToken ct)
    {
        ArgumentNullException.ThrowIfNull(pipe);

        var publishedAt = pipe.PublishedAt.ToUniversalTime();
        return dbContext.Database.CreateExecutionStrategy().ExecuteAsync(async cancellationToken =>
        {
            await using var transaction = await dbContext.Database.BeginTransactionAsync(cancellationToken);
            var inserted = await dbContext.Database.ExecuteSqlInterpolatedAsync($"""
                INSERT INTO daily_pipes (date, level_json, stack_seed, max_score, published_at)
                VALUES ({pipe.Date}, {pipe.LevelJson}, {pipe.StackSeed}, {pipe.MaxScore}, {publishedAt})
                ON CONFLICT (date) DO NOTHING
                """, cancellationToken);
            if (inserted == 0)
            {
                await transaction.RollbackAsync(cancellationToken);
                return false;
            }

            await dbContext.Database.ExecuteSqlInterpolatedAsync(
                $"DELETE FROM daily_scores WHERE date < {pipe.Date}", cancellationToken);
            await dbContext.Database.ExecuteSqlInterpolatedAsync(
                $"DELETE FROM daily_pipes WHERE date < {keepPipesFrom}", cancellationToken);
            await transaction.CommitAsync(cancellationToken);
            return true;
        }, ct);
    }

    /// <remarks>
    /// One upsert statement keeps the best result, the concurrency-safe form of
    /// <see cref="DailyScore.TryImprove"/>: the unique <c>(date, user_id)</c> index makes concurrent
    /// first submissions collide, and the row lock of <c>ON CONFLICT DO UPDATE</c> makes every
    /// submission compare against the latest stored score.
    /// </remarks>
    public async Task<DailyScoreSubmission> SubmitScoreAsync(
        DateOnly date, Guid userId, int score, int stars, DateTimeOffset achievedAt, CancellationToken ct)
    {
        var id = Guid.CreateVersion7(achievedAt);
        var achievedAtUtc = achievedAt.ToUniversalTime();

        var stored = await dbContext.Database
            .SqlQuery<ScoreRow>($"""
                INSERT INTO daily_scores (id, date, user_id, score, stars, achieved_at)
                VALUES ({id}, {date}, {userId}, {score}, {stars}, {achievedAtUtc})
                ON CONFLICT (date, user_id) DO UPDATE
                   SET score = EXCLUDED.score, stars = EXCLUDED.stars, achieved_at = EXCLUDED.achieved_at
                 WHERE daily_scores.score < EXCLUDED.score
                RETURNING score AS "Score", stars AS "Stars", achieved_at AS "AchievedAt"
                """)
            .ToListAsync(ct);

        if (stored.Count == 1)
        {
            return new DailyScoreSubmission(stored[0].Score, stored[0].Stars, stored[0].AchievedAt, IsNewBest: true);
        }

        // The stored score was equal or higher, so the update was filtered out and returned nothing.
        var best = await GetBestAsync(date, userId, ct)
            ?? throw new InvalidOperationException("The stored daily score disappeared during the submission.");
        return new DailyScoreSubmission(best.Score, best.Stars, best.AchievedAt, IsNewBest: false);
    }

    public async Task<BestDailyScore?> GetBestAsync(DateOnly date, Guid userId, CancellationToken ct) =>
        await dbContext.DailyScores
            .AsNoTracking()
            .Where(s => s.Date == date && s.UserId == userId)
            .Select(s => new BestDailyScore(s.Score, s.Stars, s.AchievedAt))
            .SingleOrDefaultAsync(ct);

    /// <remarks>
    /// Raw SQL because LINQ has no <c>&lt;</c> on <see cref="Guid"/>; PostgreSQL compares uuids the same
    /// way the <c>ORDER BY user_id</c> of <see cref="GetTopAsync"/> does.
    /// </remarks>
    public async Task<int> CountRankedAboveAsync(
        DateOnly date, int score, DateTimeOffset achievedAt, Guid userId, CancellationToken ct)
    {
        var achievedAtUtc = achievedAt.ToUniversalTime();

        return await dbContext.Database
            .SqlQuery<int>($"""
                SELECT count(*)::int AS "Value"
                  FROM daily_scores
                 WHERE date = {date}
                   AND (score > {score}
                        OR (score = {score} AND achieved_at < {achievedAtUtc})
                        OR (score = {score} AND achieved_at = {achievedAtUtc} AND user_id < {userId}))
                """)
            .SingleAsync(ct);
    }

    public async Task<IReadOnlyList<RankedDailyScore>> GetTopAsync(DateOnly date, int count, CancellationToken ct)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(count);

        var rows = await dbContext.DailyScores
            .AsNoTracking()
            .Where(s => s.Date == date)
            .OrderByDescending(s => s.Score)
            .ThenBy(s => s.AchievedAt)
            .ThenBy(s => s.UserId)
            .Take(count)
            .Select(s => new { s.UserId, s.Score, s.Stars, s.AchievedAt })
            .ToListAsync(ct);

        return rows
            .Select((row, index) => new RankedDailyScore(index + 1, row.UserId, row.Score, row.Stars, row.AchievedAt))
            .ToList();
    }

    public async Task<DailyStreak?> GetStreakAsync(Guid userId, CancellationToken ct)
    {
        var entity = await dbContext.DailyStreaks.AsNoTracking().SingleOrDefaultAsync(s => s.UserId == userId, ct);
        return entity is null ? null : ToDomain(entity);
    }

    /// <remarks>
    /// The player's row is created when missing and locked with <c>FOR UPDATE</c>, so two wins at once
    /// are counted one after the other by <see cref="DailyStreak.RecordWin"/>.
    /// </remarks>
    public Task<DailyStreak> RecordWinAsync(Guid userId, DateOnly date, CancellationToken ct) =>
        dbContext.Database.CreateExecutionStrategy().ExecuteAsync(async cancellationToken =>
        {
            await using var transaction = await dbContext.Database.BeginTransactionAsync(cancellationToken);
            await dbContext.Database.ExecuteSqlInterpolatedAsync($"""
                INSERT INTO daily_streaks (user_id, current_streak, best_streak, last_win_date)
                VALUES ({userId}, 0, 0, NULL)
                ON CONFLICT (user_id) DO NOTHING
                """, cancellationToken);
            var row = await dbContext.DailyStreaks
                .FromSql($"SELECT * FROM daily_streaks WHERE user_id = {userId} FOR UPDATE")
                .AsNoTracking()
                .SingleAsync(cancellationToken);

            var streak = ToDomain(row);
            if (streak.RecordWin(date))
            {
                await dbContext.Database.ExecuteSqlInterpolatedAsync($"""
                    UPDATE daily_streaks
                       SET current_streak = {streak.Current}, best_streak = {streak.Best}, last_win_date = {streak.LastWinDate}
                     WHERE user_id = {userId}
                    """, cancellationToken);
            }

            await transaction.CommitAsync(cancellationToken);
            return streak;
        }, ct);

    private static DailyStreak ToDomain(DailyStreakEntity entity) =>
        DailyStreak.Restore(entity.UserId, entity.CurrentStreak, entity.BestStreak, entity.LastWinDate);

    /// <summary>A row returned by the score upsert.</summary>
    private sealed class ScoreRow
    {
        public int Score { get; init; }

        public int Stars { get; init; }

        public DateTimeOffset AchievedAt { get; init; }
    }
}
