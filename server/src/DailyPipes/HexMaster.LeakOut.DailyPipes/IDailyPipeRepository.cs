using HexMaster.LeakOut.DailyPipes.DomainModels;

namespace HexMaster.LeakOut.DailyPipes;

/// <summary>Stores the daily pipes, the players' best results on them and their streaks.</summary>
public interface IDailyPipeRepository
{
    /// <summary>Reads the pipe of <paramref name="date"/>, or <see langword="null"/> when none is published.</summary>
    Task<DailyPipe?> GetPipeAsync(DateOnly date, CancellationToken ct);

    /// <summary>
    /// Publishes <paramref name="pipe"/> unless its date already has one, and in the same transaction
    /// deletes the scores of every earlier date and the pipes before <paramref name="keepPipesFrom"/>.
    /// Streaks are never deleted.
    /// </summary>
    /// <returns><see langword="true"/> when the pipe was published; <see langword="false"/> when its date already had one, which is left unchanged.</returns>
    Task<bool> PublishAsync(DailyPipe pipe, DateOnly keepPipesFrom, CancellationToken ct);

    /// <summary>
    /// Stores the result as the player's best on <paramref name="date"/> when the player has none yet
    /// or <paramref name="score"/> is strictly higher, atomically with respect to concurrent submissions.
    /// </summary>
    Task<DailyScoreSubmission> SubmitScoreAsync(DateOnly date, Guid userId, int score, int stars, DateTimeOffset achievedAt, CancellationToken ct);

    /// <summary>Reads the player's best result on <paramref name="date"/>, or <see langword="null"/> when there is none.</summary>
    Task<BestDailyScore?> GetBestAsync(DateOnly date, Guid userId, CancellationToken ct);

    /// <summary>
    /// Counts the results on <paramref name="date"/> that rank above the given one: score descending,
    /// then the earliest achieved first, then by player id.
    /// </summary>
    Task<int> CountRankedAboveAsync(DateOnly date, int score, DateTimeOffset achievedAt, Guid userId, CancellationToken ct);

    /// <summary>Reads the first <paramref name="count"/> entries of the ranking of <paramref name="date"/>, with 1-based positions.</summary>
    Task<IReadOnlyList<RankedDailyScore>> GetTopAsync(DateOnly date, int count, CancellationToken ct);

    /// <summary>Reads the player's streak, or <see langword="null"/> when the player never won a daily pipe.</summary>
    Task<DailyStreak?> GetStreakAsync(Guid userId, CancellationToken ct);

    /// <summary>Counts a win on <paramref name="date"/> in the player's streak, atomically, and returns the streak after it.</summary>
    Task<DailyStreak> RecordWinAsync(Guid userId, DateOnly date, CancellationToken ct);
}

/// <summary>The outcome of <see cref="IDailyPipeRepository.SubmitScoreAsync"/>: the stored best result and whether this submission became it.</summary>
public sealed record DailyScoreSubmission(int BestScore, int BestStars, DateTimeOffset AchievedAt, bool IsNewBest);

/// <summary>A player's stored best result on a date and the moment it was achieved.</summary>
public sealed record BestDailyScore(int Score, int Stars, DateTimeOffset AchievedAt);

/// <summary>A player's best result on a date with its 1-based position in that date's ranking.</summary>
public sealed record RankedDailyScore(int Position, Guid UserId, int Score, int Stars, DateTimeOffset AchievedAt);
