using HexMaster.LeakOut.Core;

namespace HexMaster.LeakOut.Scores.DomainModels;

/// <summary>
/// The best score a player achieved on a level. A player has at most one per level; only a
/// strictly higher score replaces it.
/// </summary>
public sealed class LevelScore
{
    private LevelScore()
    {
    }

    public Guid Id { get; private set; }

    /// <summary>The internal id of the player; never the Google Play Games player id.</summary>
    public Guid UserId { get; private set; }

    public int Level { get; private set; }

    public int Score { get; private set; }

    /// <summary>The moment the current best score was achieved; the leaderboards' tie-breaker.</summary>
    public DateTimeOffset AchievedAt { get; private set; }

    /// <summary>Records the first score of a player on a level.</summary>
    public static LevelScore Create(Guid userId, int level, int score, DateTimeOffset achievedAt)
    {
        if (userId == Guid.Empty)
        {
            throw new DomainException("A level score must belong to a player.");
        }

        if (level < 1)
        {
            throw new DomainException("A level number must be at least 1.");
        }

        if (score < 0)
        {
            throw new DomainException("A score cannot be negative.");
        }

        return new LevelScore
        {
            Id = Guid.CreateVersion7(achievedAt),
            UserId = userId,
            Level = level,
            Score = score,
            AchievedAt = achievedAt,
        };
    }

    /// <summary>
    /// Replaces the best score when <paramref name="score"/> is strictly higher. An equal or lower
    /// score leaves the score and the moment it was achieved unchanged.
    /// </summary>
    /// <returns><see langword="true"/> when the score became the new best.</returns>
    public bool TryImprove(int score, DateTimeOffset achievedAt)
    {
        if (score <= Score)
        {
            return false;
        }

        Score = score;
        AchievedAt = achievedAt;
        return true;
    }
}
