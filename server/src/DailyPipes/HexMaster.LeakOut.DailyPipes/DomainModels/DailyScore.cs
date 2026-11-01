using HexMaster.LeakOut.Core;

namespace HexMaster.LeakOut.DailyPipes.DomainModels;

/// <summary>
/// The best result of a player on the daily pipe of one date. A player has at most one per date;
/// only a strictly higher score replaces it.
/// </summary>
public sealed class DailyScore
{
    public const int MaxStars = 3;

    private DailyScore()
    {
    }

    public DateOnly Date { get; private set; }

    /// <summary>The internal id of the player; never the Google Play Games player id.</summary>
    public Guid UserId { get; private set; }

    public int Score { get; private set; }

    public int Stars { get; private set; }

    /// <summary>The moment the current best score was achieved; the ranking's tie-breaker.</summary>
    public DateTimeOffset AchievedAt { get; private set; }

    public static DailyScore Create(DateOnly date, Guid userId, int score, int stars, DateTimeOffset achievedAt)
    {
        if (userId == Guid.Empty)
        {
            throw new DomainException("A daily score must belong to a player.");
        }

        Validate(score, stars);
        return new DailyScore
        {
            Date = date,
            UserId = userId,
            Score = score,
            Stars = stars,
            AchievedAt = achievedAt,
        };
    }

    /// <summary>
    /// Replaces the best result when <paramref name="score"/> is strictly higher. An equal or lower
    /// score leaves the result and the moment it was achieved unchanged.
    /// </summary>
    /// <returns><see langword="true"/> when the score became the new best.</returns>
    public bool TryImprove(int score, int stars, DateTimeOffset achievedAt)
    {
        Validate(score, stars);
        if (score <= Score)
        {
            return false;
        }

        Score = score;
        Stars = stars;
        AchievedAt = achievedAt;
        return true;
    }

    private static void Validate(int score, int stars)
    {
        if (score < 0)
        {
            throw new DomainException("A score cannot be negative.");
        }

        if (stars is < 0 or > MaxStars)
        {
            throw new DomainException($"Stars must be between 0 and {MaxStars}.");
        }
    }
}
