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
    /// Served by the <c>(level, score desc, achieved_at)</c> index. For the top N, numbering the
    /// rows in memory equals <c>ROW_NUMBER() OVER (ORDER BY score DESC, achieved_at, user_id)</c>.
    /// </remarks>
    public async Task<IReadOnlyList<RankedLevelScore>> GetTopAsync(int level, int count, CancellationToken ct)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(count);

        var rows = await dbContext.LevelScores
            .AsNoTracking()
            .Where(s => s.Level == level)
            .OrderByDescending(s => s.Score)
            .ThenBy(s => s.AchievedAt)
            .ThenBy(s => s.UserId)
            .Take(count)
            .Select(s => new { s.UserId, s.Score })
            .ToListAsync(ct);

        return rows.Select((row, index) => new RankedLevelScore(index + 1, row.UserId, row.Score)).ToList();
    }
}
