namespace HexMaster.LeakOut.Scores;

/// <summary>Stores the best score of every player on every level.</summary>
public interface ILevelScoreRepository
{
    /// <summary>
    /// Stores <paramref name="score"/> as the player's best score on <paramref name="level"/> when the
    /// player has none yet or it is strictly higher than the stored one, atomically with respect to
    /// concurrent submissions.
    /// </summary>
    /// <returns>The stored best score after the submission and whether this submission became it.</returns>
    Task<LevelScoreSubmission> SubmitAsync(Guid userId, int level, int score, DateTimeOffset achievedAt, CancellationToken cancellationToken);

    /// <summary>
    /// Reads <paramref name="count"/> entries of the ranking of <paramref name="level"/>, skipping the
    /// first <paramref name="offset"/>: score descending, then the earliest achieved first, then by
    /// player id. Positions are 1-based row numbers, so two entries never share one.
    /// </summary>
    Task<IReadOnlyList<RankedLevelScore>> GetRankedAsync(int level, int offset, int count, CancellationToken ct);

    /// <summary>Reads the player's best score on <paramref name="level"/>, or <see langword="null"/> when there is none.</summary>
    Task<BestLevelScore?> GetBestAsync(Guid userId, int level, CancellationToken ct);

    /// <summary>
    /// Counts the scores on <paramref name="level"/> that rank above the given player's best score,
    /// using the same order as <see cref="GetRankedAsync"/>.
    /// </summary>
    Task<int> CountRankedAboveAsync(int level, int score, DateTimeOffset achievedAt, Guid userId, CancellationToken ct);

    /// <summary>Counts the scores on <paramref name="level"/>.</summary>
    Task<int> CountAsync(int level, CancellationToken ct);
}

/// <summary>The outcome of <see cref="ILevelScoreRepository.SubmitAsync"/>.</summary>
public sealed record LevelScoreSubmission(int BestScore, bool IsNewBest);

/// <summary>A player's stored best score on a level and the moment it was achieved.</summary>
public sealed record BestLevelScore(int Score, DateTimeOffset AchievedAt);

/// <summary>
/// A player's best score on a level with its position in the level's ranking and the moment the server
/// received that score.
/// </summary>
public sealed record RankedLevelScore(int Position, Guid UserId, int Score, DateTimeOffset AchievedAt);
