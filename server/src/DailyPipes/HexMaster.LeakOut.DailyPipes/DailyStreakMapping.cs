using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.DomainModels;

namespace HexMaster.LeakOut.DailyPipes;

internal static class DailyStreakMapping
{
    /// <summary>The streak as the app sees it on <paramref name="today"/>; no streak yet reads as 0.</summary>
    public static DailyStreakDto ToDto(this DailyStreak? streak, DateOnly today) =>
        streak is null
            ? new DailyStreakDto(0, 0, WonToday: false)
            : new DailyStreakDto(streak.CurrentOn(today), streak.Best, streak.WonOn(today));

    public static DateOnly UtcDate(this DateTimeOffset moment) => DateOnly.FromDateTime(moment.UtcDateTime);
}
