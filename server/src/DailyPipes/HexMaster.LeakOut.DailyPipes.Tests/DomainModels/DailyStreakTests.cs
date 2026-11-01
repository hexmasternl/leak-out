using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.DomainModels;

namespace HexMaster.LeakOut.DailyPipes.Tests.DomainModels;

/// <summary>The scenarios of the <c>server-daily-pipe-streaks</c> spec, on the domain model.</summary>
public sealed class DailyStreakTests
{
    private static readonly Guid UserId = Guid.CreateVersion7();
    private static readonly DateOnly Nov1 = new(2026, 11, 1);
    private static readonly DateOnly Nov2 = new(2026, 11, 2);

    [Fact]
    public void Start_HasNoStreak()
    {
        var streak = DailyStreak.Start(UserId);

        Assert.Equal(UserId, streak.UserId);
        Assert.Equal(0, streak.Current);
        Assert.Equal(0, streak.Best);
        Assert.Null(streak.LastWinDate);
        Assert.Equal(0, streak.CurrentOn(Nov1));
        Assert.False(streak.WonOn(Nov1));
    }

    [Fact]
    public void Start_WithoutPlayer_Throws() =>
        Assert.Throws<DomainException>(() => DailyStreak.Start(Guid.Empty));

    [Fact]
    public void RecordWin_FirstWin_StartsAtOne()
    {
        var streak = DailyStreak.Start(UserId);

        Assert.True(streak.RecordWin(Nov1));

        Assert.Equal(1, streak.Current);
        Assert.Equal(1, streak.Best);
        Assert.Equal(Nov1, streak.LastWinDate);
        Assert.True(streak.WonOn(Nov1));
    }

    [Fact]
    public void RecordWin_ConsecutiveDay_GoesUpByOne()
    {
        var streak = DailyStreak.Restore(UserId, current: 3, best: 3, lastWinDate: Nov1);

        Assert.True(streak.RecordWin(Nov2));

        Assert.Equal(4, streak.Current);
        Assert.Equal(4, streak.Best);
        Assert.Equal(Nov2, streak.LastWinDate);
    }

    [Fact]
    public void RecordWin_SecondWinOnTheSameDay_ChangesNothing()
    {
        var streak = DailyStreak.Restore(UserId, current: 3, best: 5, lastWinDate: Nov2);

        Assert.False(streak.RecordWin(Nov2));

        Assert.Equal(3, streak.Current);
        Assert.Equal(5, streak.Best);
        Assert.Equal(Nov2, streak.LastWinDate);
    }

    [Fact]
    public void RecordWin_EarlierDate_ChangesNothing()
    {
        var streak = DailyStreak.Restore(UserId, current: 2, best: 2, lastWinDate: Nov2);

        Assert.False(streak.RecordWin(Nov1));

        Assert.Equal(2, streak.Current);
    }

    [Fact]
    public void RecordWin_AfterAMissedDay_RestartsAtOneAndKeepsBest()
    {
        var streak = DailyStreak.Restore(UserId, current: 5, best: 5, lastWinDate: new DateOnly(2026, 10, 30));

        Assert.True(streak.RecordWin(Nov2));

        Assert.Equal(1, streak.Current);
        Assert.Equal(5, streak.Best);
    }

    [Fact]
    public void CurrentOn_MissedDay_ReportsZeroAndKeepsBest()
    {
        var streak = DailyStreak.Restore(UserId, current: 5, best: 5, lastWinDate: new DateOnly(2026, 10, 30));

        Assert.Equal(0, streak.CurrentOn(Nov2));
        Assert.Equal(5, streak.Best);
    }

    [Theory]
    [InlineData(2026, 11, 1)]
    [InlineData(2026, 11, 2)]
    public void CurrentOn_LastWinYesterdayOrToday_ReportsTheStreak(int year, int month, int day)
    {
        var streak = DailyStreak.Restore(UserId, current: 4, best: 6, lastWinDate: new DateOnly(year, month, day));

        Assert.Equal(4, streak.CurrentOn(Nov2));
    }

    [Theory]
    [InlineData(-1, 0, true)]
    [InlineData(3, 2, true)]
    [InlineData(1, 1, false)]
    [InlineData(0, 2, true)]
    public void Restore_InconsistentValues_Throws(int current, int best, bool withDate)
    {
        // (0, 2, with a date) and (1, 1, without one) break the date rule; the others the range rule.
        DateOnly? lastWin = withDate ? Nov1 : null;

        Assert.Throws<DomainException>(() => DailyStreak.Restore(UserId, current, best, lastWin));
    }

    [Fact]
    public void Restore_NoWinYet_IsAllowed()
    {
        var streak = DailyStreak.Restore(UserId, current: 0, best: 4, lastWinDate: null);

        Assert.Equal(0, streak.Current);
        Assert.Equal(4, streak.Best);
    }
}
