using HexMaster.LeakOut.Core;

namespace HexMaster.LeakOut.DailyPipes.DomainModels;

/// <summary>
/// The level every player gets on one UTC date: the level in the level-format JSON, the seed the app
/// deals the stack from, and the highest score a submission for it may claim.
/// </summary>
public sealed class DailyPipe
{
    private DailyPipe()
    {
    }

    /// <summary>The UTC date the pipe is played on; one pipe per date.</summary>
    public DateOnly Date { get; private set; }

    /// <summary>The level in the level-format JSON (<c>docs/level-format.md</c>).</summary>
    public string LevelJson { get; private set; } = string.Empty;

    /// <summary>The seed of the random source the app deals the stack from.</summary>
    public long StackSeed { get; private set; }

    /// <summary>The highest score a submission may claim on this pipe.</summary>
    public int MaxScore { get; private set; }

    public DateTimeOffset PublishedAt { get; private set; }

    public static DailyPipe Create(DateOnly date, string levelJson, long stackSeed, int maxScore, DateTimeOffset publishedAt)
    {
        if (string.IsNullOrWhiteSpace(levelJson))
        {
            throw new DomainException("A daily pipe needs a level.");
        }

        if (stackSeed < 0)
        {
            throw new DomainException("A stack seed cannot be negative.");
        }

        if (maxScore < 1)
        {
            throw new DomainException("The maximum score of a daily pipe must be at least 1.");
        }

        return new DailyPipe
        {
            Date = date,
            LevelJson = levelJson,
            StackSeed = stackSeed,
            MaxScore = maxScore,
            PublishedAt = publishedAt,
        };
    }
}
