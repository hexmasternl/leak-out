using HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;

namespace HexMaster.LeakOut.Scores.Tests.GetLevelScoresAroundPlayer;

public sealed class LeaderboardWindowTests
{
    [Theory]
    [InlineData(128, 200, 123, 10)]
    [InlineData(3, 50, 1, 10)]
    [InlineData(49, 50, 41, 10)]
    [InlineData(4, 6, 1, 6)]
    [InlineData(1, 1, 1, 1)]
    [InlineData(6, 10, 1, 10)]
    [InlineData(10, 10, 1, 10)]
    [InlineData(6, 15, 1, 10)]
    [InlineData(7, 15, 2, 10)]
    public void Compute_ReturnsStartAndCount(int position, int total, int expectedStart, int expectedCount)
    {
        Assert.Equal(new LeaderboardWindow(expectedStart, expectedCount), LeaderboardWindow.Compute(position, total));
    }

    [Theory]
    [InlineData(0, 10)]
    [InlineData(11, 10)]
    public void Compute_PositionOutsideTheRanking_ThrowsArgumentOutOfRangeException(int position, int total)
    {
        Assert.Throws<ArgumentOutOfRangeException>(() => LeaderboardWindow.Compute(position, total));
    }
}
