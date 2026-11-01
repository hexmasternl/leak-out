using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.DomainModels;

namespace HexMaster.LeakOut.DailyPipes.Tests.DomainModels;

public sealed class DailyScoreTests
{
    private static readonly DateOnly Date = new(2026, 11, 1);
    private static readonly DateTimeOffset Now = new(2026, 11, 1, 9, 0, 0, TimeSpan.Zero);

    [Fact]
    public void Create_StoresTheResult()
    {
        var userId = Guid.CreateVersion7();

        var score = DailyScore.Create(Date, userId, 300, 2, Now);

        Assert.Equal(Date, score.Date);
        Assert.Equal(userId, score.UserId);
        Assert.Equal(300, score.Score);
        Assert.Equal(2, score.Stars);
        Assert.Equal(Now, score.AchievedAt);
    }

    [Theory]
    [InlineData(-1, 1)]
    [InlineData(100, -1)]
    [InlineData(100, 4)]
    public void Create_OutOfRange_Throws(int points, int stars) =>
        Assert.Throws<DomainException>(() => DailyScore.Create(Date, Guid.CreateVersion7(), points, stars, Now));

    [Fact]
    public void Create_WithoutPlayer_Throws() =>
        Assert.Throws<DomainException>(() => DailyScore.Create(Date, Guid.Empty, 100, 1, Now));

    [Fact]
    public void TryImprove_HigherScore_Replaces()
    {
        var score = DailyScore.Create(Date, Guid.CreateVersion7(), 300, 1, Now);

        Assert.True(score.TryImprove(420, 2, Now.AddHours(1)));

        Assert.Equal(420, score.Score);
        Assert.Equal(2, score.Stars);
        Assert.Equal(Now.AddHours(1), score.AchievedAt);
    }

    [Theory]
    [InlineData(420)]
    [InlineData(300)]
    public void TryImprove_EqualOrLowerScore_KeepsTheBest(int points)
    {
        var score = DailyScore.Create(Date, Guid.CreateVersion7(), 420, 2, Now);

        Assert.False(score.TryImprove(points, 1, Now.AddHours(1)));

        Assert.Equal(420, score.Score);
        Assert.Equal(2, score.Stars);
        Assert.Equal(Now, score.AchievedAt);
    }

    [Fact]
    public void TryImprove_InvalidStars_Throws()
    {
        var score = DailyScore.Create(Date, Guid.CreateVersion7(), 100, 1, Now);

        Assert.Throws<DomainException>(() => score.TryImprove(200, 5, Now));
    }
}
