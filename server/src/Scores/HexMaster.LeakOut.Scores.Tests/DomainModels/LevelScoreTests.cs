using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.DomainModels;
using HexMaster.LeakOut.Scores.Tests.Factories;

namespace HexMaster.LeakOut.Scores.Tests.DomainModels;

public sealed class LevelScoreTests
{
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);
    private static readonly Guid UserId = Guid.CreateVersion7();

    [Theory]
    [InlineData(1, 0)]
    [InlineData(4, 520)]
    public void Create_WithValidInput_SetsAllValues(int level, int score)
    {
        var levelScore = LevelScore.Create(UserId, level, score, Now);

        Assert.NotEqual(Guid.Empty, levelScore.Id);
        Assert.Equal(UserId, levelScore.UserId);
        Assert.Equal(level, levelScore.Level);
        Assert.Equal(score, levelScore.Score);
        Assert.Equal(Now, levelScore.AchievedAt);
    }

    [Fact]
    public void Create_GivesEveryScoreANewId()
    {
        Assert.NotEqual(LevelScore.Create(UserId, 1, 10, Now).Id, LevelScore.Create(UserId, 1, 10, Now).Id);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-1)]
    public void Create_WithLevelBelowOne_ThrowsDomainException(int level)
    {
        Assert.Throws<DomainException>(() => LevelScore.Create(UserId, level, 100, Now));
    }

    [Fact]
    public void Create_WithNegativeScore_ThrowsDomainException()
    {
        Assert.Throws<DomainException>(() => LevelScore.Create(UserId, 3, -5, Now));
    }

    [Fact]
    public void Create_WithoutPlayer_ThrowsDomainException()
    {
        Assert.Throws<DomainException>(() => LevelScore.Create(Guid.Empty, 3, 5, Now));
    }

    [Fact]
    public void TryImprove_WithHigherScore_ReplacesScoreAndMoment()
    {
        var levelScore = new LevelScoreFaker(score: 520).Generate();

        var improved = levelScore.TryImprove(540, Now);

        Assert.True(improved);
        Assert.Equal(540, levelScore.Score);
        Assert.Equal(Now, levelScore.AchievedAt);
    }

    [Theory]
    [InlineData(520)]
    [InlineData(300)]
    [InlineData(0)]
    public void TryImprove_WithEqualOrLowerScore_KeepsScoreAndMoment(int score)
    {
        var levelScore = new LevelScoreFaker(score: 520).Generate();
        var achievedAt = levelScore.AchievedAt;

        var improved = levelScore.TryImprove(score, Now);

        Assert.False(improved);
        Assert.Equal(520, levelScore.Score);
        Assert.Equal(achievedAt, levelScore.AchievedAt);
    }
}
