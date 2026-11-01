using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

namespace HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;

/// <summary>Submits a player's result on the daily pipe of <paramref name="Date"/>.</summary>
public sealed record SubmitDailyScoreCommand(Guid UserId, DateOnly Date, int Score, int Stars);

/// <summary>What a daily pipe submission did.</summary>
public abstract record SubmitDailyScoreOutcome
{
    private SubmitDailyScoreOutcome()
    {
    }

    /// <summary>The submission was taken; <see cref="Result"/> holds the best result, rank and streak after it.</summary>
    public sealed record Accepted(DailyScoreResultDto Result) : SubmitDailyScoreOutcome;

    /// <summary>The date is not the current daily pipe's: that day has ended, or has not started. Nothing was stored.</summary>
    public sealed record Ended : SubmitDailyScoreOutcome;

    /// <summary>The score is above the highest score the pipe allows. Nothing was stored.</summary>
    public sealed record ScoreTooHigh(int MaxScore) : SubmitDailyScoreOutcome;
}
