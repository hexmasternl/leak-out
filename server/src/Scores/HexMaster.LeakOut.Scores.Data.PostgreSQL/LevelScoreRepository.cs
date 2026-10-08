using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

public sealed class LevelScoreRepository(ScoresDbContext dbContext) : ILevelScoreRepository
{
    /// <remarks>
    /// One upsert statement keeps the best score: the unique <c>(user_id, level)</c> index makes
    /// concurrent first submissions collide instead of creating two rows, and the row lock taken by
    /// <c>ON CONFLICT DO UPDATE</c> makes every submission compare against the latest stored score.
    /// This is the concurrency-safe form of <see cref="DomainModels.LevelScore.TryImprove"/>.
    /// </remarks>
    public async Task<LevelScoreSubmission> SubmitAsync(
        Guid userId, int level, int score, DateTimeOffset achievedAt, CancellationToken cancellationToken)
    {
        var id = Guid.CreateVersion7(achievedAt);
        var achievedAtUtc = achievedAt.ToUniversalTime();

        var stored = await dbContext.Database
            .SqlQuery<int>($"""
                INSERT INTO level_scores (id, user_id, level, score, achieved_at)
                VALUES ({id}, {userId}, {level}, {score}, {achievedAtUtc})
                ON CONFLICT (user_id, level) DO UPDATE
                   SET score = EXCLUDED.score, achieved_at = EXCLUDED.achieved_at
                 WHERE level_scores.score < EXCLUDED.score
                RETURNING score AS "Value"
                """)
            .ToListAsync(cancellationToken);

        if (stored.Count == 1)
        {
            return new LevelScoreSubmission(stored[0], IsNewBest: true);
        }

        // The stored score was equal or higher, so the update was filtered out and returned nothing.
        var bestScore = await dbContext.LevelScores
            .AsNoTracking()
            .Where(s => s.UserId == userId && s.Level == level)
            .Select(s => s.Score)
            .SingleAsync(cancellationToken);

        return new LevelScoreSubmission(bestScore, IsNewBest: false);
    }

    /// <remarks>
    /// Served by the <c>(level, score desc, achieved_at)</c> index. Numbering the rows in memory from
    /// <paramref name="offset"/> equals <c>ROW_NUMBER() OVER (ORDER BY score DESC, achieved_at, user_id)</c>.
    /// </remarks>
    public async Task<IReadOnlyList<RankedLevelScore>> GetRankedAsync(int level, int offset, int count, CancellationToken ct)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(offset);
        ArgumentOutOfRangeException.ThrowIfNegative(count);

        var rows = await dbContext.LevelScores
            .AsNoTracking()
            .Where(s => s.Level == level)
            .OrderByDescending(s => s.Score)
            .ThenBy(s => s.AchievedAt)
            .ThenBy(s => s.UserId)
            .Skip(offset)
            .Take(count)
            .Select(s => new { s.UserId, s.Score, s.AchievedAt })
            .ToListAsync(ct);

        return rows
            .Select((row, index) => new RankedLevelScore(offset + index + 1, row.UserId, row.Score, row.AchievedAt))
            .ToList();
    }

    public async Task<BestLevelScore?> GetBestAsync(Guid userId, int level, CancellationToken ct) =>
        await dbContext.LevelScores
            .AsNoTracking()
            .Where(s => s.UserId == userId && s.Level == level)
            .Select(s => new BestLevelScore(s.Score, s.AchievedAt))
            .SingleOrDefaultAsync(ct);

    /// <remarks>
    /// Raw SQL because LINQ has no <c>&lt;</c> on <see cref="Guid"/>; PostgreSQL compares uuids the same
    /// way the <c>ORDER BY user_id</c> of <see cref="GetRankedAsync"/> does.
    /// </remarks>
    public async Task<int> CountRankedAboveAsync(
        int level, int score, DateTimeOffset achievedAt, Guid userId, CancellationToken ct)
    {
        var achievedAtUtc = achievedAt.ToUniversalTime();

        return await dbContext.Database
            .SqlQuery<int>($"""
                SELECT count(*)::int AS "Value"
                  FROM level_scores
                 WHERE level = {level}
                   AND (score > {score}
                        OR (score = {score} AND achieved_at < {achievedAtUtc})
                        OR (score = {score} AND achieved_at = {achievedAtUtc} AND user_id < {userId}))
                """)
            .SingleAsync(ct);
    }

    public Task<int> CountAsync(int level, CancellationToken ct) =>
        dbContext.LevelScores.CountAsync(s => s.Level == level, ct);
}
