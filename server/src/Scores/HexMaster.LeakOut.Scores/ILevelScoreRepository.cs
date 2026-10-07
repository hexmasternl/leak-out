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
    /// Reads the first <paramref name="count"/> entries of the ranking of <paramref name="level"/>:
    /// score descending, then the earliest achieved first, then by player id. Positions are 1-based
    /// row numbers, so two entries never share one.
    /// </summary>
    Task<IReadOnlyList<RankedLevelScore>> GetTopAsync(int level, int count, CancellationToken ct);
}

/// <summary>The outcome of <see cref="ILevelScoreRepository.SubmitAsync"/>.</summary>
public sealed record LevelScoreSubmission(int BestScore, bool IsNewBest);

/// <summary>A player's best score on a level with its position in the level's ranking.</summary>
public sealed record RankedLevelScore(int Position, Guid UserId, int Score);
