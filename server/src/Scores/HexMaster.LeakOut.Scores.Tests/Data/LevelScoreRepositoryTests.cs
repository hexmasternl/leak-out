using HexMaster.LeakOut.Scores.Data.PostgreSQL;
using HexMaster.LeakOut.Scores.Tests.Helpers;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Scores.Tests.Data;

/// <summary>Runs the repository's upsert against a real, migrated PostgreSQL database.</summary>
[Collection(PostgreSqlCollection.Name)]
public sealed class LevelScoreRepositoryTests(PostgreSqlFixture database)
{
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);

    private readonly Guid _userId = Guid.CreateVersion7();

    [Fact]
    public async Task SubmitAsync_FirstScore_StoresItAsNewBest()
    {
        var result = await SubmitAsync(4, 520, Now);

        Assert.Equal(new LevelScoreSubmission(520, IsNewBest: true), result);
        var stored = await SingleStoredAsync(4);
        Assert.Equal(520, stored.Score);
        Assert.Equal(Now, stored.AchievedAt);
    }

    [Fact]
    public async Task SubmitAsync_ZeroScore_IsStored()
    {
        var result = await SubmitAsync(3, 0, Now);

        Assert.Equal(new LevelScoreSubmission(0, IsNewBest: true), result);
        Assert.Equal(0, (await SingleStoredAsync(3)).Score);
    }

    [Fact]
    public async Task SubmitAsync_HigherScore_ReplacesScoreAndMoment()
    {
        await SubmitAsync(4, 520, Now);

        var result = await SubmitAsync(4, 540, Now.AddHours(1));

        Assert.Equal(new LevelScoreSubmission(540, IsNewBest: true), result);
        var stored = await SingleStoredAsync(4);
        Assert.Equal(540, stored.Score);
        Assert.Equal(Now.AddHours(1), stored.AchievedAt);
    }

    [Theory]
    [InlineData(520)]
    [InlineData(300)]
    public async Task SubmitAsync_EqualOrLowerScore_KeepsScoreAndMoment(int score)
    {
        await SubmitAsync(4, 520, Now);

        var result = await SubmitAsync(4, score, Now.AddHours(1));

        Assert.Equal(new LevelScoreSubmission(520, IsNewBest: false), result);
        var stored = await SingleStoredAsync(4);
        Assert.Equal(520, stored.Score);
        Assert.Equal(Now, stored.AchievedAt);
    }

    [Fact]
    public async Task SubmitAsync_OtherLevel_DoesNotAffectStoredLevel()
    {
        await SubmitAsync(1, 900, Now);

        var result = await SubmitAsync(2, 100, Now);

        Assert.Equal(new LevelScoreSubmission(100, IsNewBest: true), result);
        Assert.Equal(900, (await SingleStoredAsync(1)).Score);
        Assert.Equal(100, (await SingleStoredAsync(2)).Score);
    }

    [Fact]
    public async Task SubmitAsync_OtherPlayer_DoesNotAffectStoredScore()
    {
        await SubmitAsync(1, 900, Now);
        var otherUserId = Guid.CreateVersion7();

        await using (var dbContext = database.CreateDbContext())
        {
            await new LevelScoreRepository(dbContext).SubmitAsync(otherUserId, 1, 50, Now, TestContext.Current.CancellationToken);
        }

        Assert.Equal(900, (await SingleStoredAsync(1)).Score);
    }

    [Fact]
    public async Task SubmitAsync_ConcurrentSubmissions_KeepOneRowWithTheHighestScore()
    {
        var scores = Enumerable.Range(1, 20).Select(i => i * 10).Reverse().Prepend(100).Append(200).ToArray();

        var results = await Task.WhenAll(scores.Select(score => Task.Run(() => SubmitAsync(2, score, Now))));

        var stored = await SingleStoredAsync(2);
        Assert.Equal(scores.Max(), stored.Score);
        Assert.Contains(results, r => r.IsNewBest && r.BestScore == scores.Max());
    }

    [Fact]
    public async Task Database_RejectsInvalidLevelAndScore()
    {
        await using var dbContext = database.CreateDbContext();
        dbContext.LevelScores.Add(new LevelScoreEntity { Id = Guid.CreateVersion7(), UserId = _userId, Level = 0, Score = -1, AchievedAt = Now });

        await Assert.ThrowsAsync<DbUpdateException>(() => dbContext.SaveChangesAsync(TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task GetRankedAsync_RanksByScoreThenEarliestThenPlayer()
    {
        var level = UniqueLevel();
        var (low, high, tieLater, tieEarlierB, tieEarlierA) = await SubmitTiedRankingAsync(level);

        var ranked = await GetRankedAsync(level, 0, 10);

        Assert.Equal(
            [
                new RankedLevelScore(1, high, 900),
                new RankedLevelScore(2, tieEarlierA, 500),
                new RankedLevelScore(3, tieEarlierB, 500),
                new RankedLevelScore(4, tieLater, 500),
                new RankedLevelScore(5, low, 100),
            ],
            ranked);
    }

    [Fact]
    public async Task GetRankedAsync_MoreScoresThanCount_ReturnsOnlyTheFirstCount()
    {
        var level = UniqueLevel();
        await SubmitAscendingScoresAsync(level, 25);

        var ranked = await GetRankedAsync(level, 0, 10);

        Assert.Equal(Enumerable.Range(1, 10), ranked.Select(s => s.Position));
        Assert.Equal(Enumerable.Range(16, 10).Reverse().Select(i => i * 10), ranked.Select(s => s.Score));
    }

    [Fact]
    public async Task GetRankedAsync_WithOffset_NumbersPositionsFromTheOffset()
    {
        var level = UniqueLevel();
        await SubmitAscendingScoresAsync(level, 25);

        var ranked = await GetRankedAsync(level, 5, 10);

        Assert.Equal(Enumerable.Range(6, 10), ranked.Select(s => s.Position));
        Assert.Equal(Enumerable.Range(11, 10).Reverse().Select(i => i * 10), ranked.Select(s => s.Score));
    }

    [Fact]
    public async Task GetRankedAsync_OffsetNearTheEnd_ReturnsTheRemainingEntries()
    {
        var level = UniqueLevel();
        await SubmitAscendingScoresAsync(level, 12);

        var ranked = await GetRankedAsync(level, 10, 10);

        Assert.Equal([new RankedLevelScore(11, PlayerId(2), 20), new RankedLevelScore(12, PlayerId(1), 10)], ranked);
    }

    [Fact]
    public async Task GetRankedAsync_LevelWithoutScores_ReturnsEmpty()
    {
        await SubmitAsync(PlayerId(1), UniqueLevel(), 100, Now);

        Assert.Empty(await GetRankedAsync(UniqueLevel(), 0, 10));
    }

    [Theory]
    [InlineData(0, -1)]
    [InlineData(-1, 10)]
    public async Task GetRankedAsync_NegativeOffsetOrCount_ThrowsArgumentOutOfRangeException(int offset, int count)
    {
        await using var dbContext = database.CreateDbContext();

        await Assert.ThrowsAsync<ArgumentOutOfRangeException>(
            () => new LevelScoreRepository(dbContext).GetRankedAsync(1, offset, count, TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task GetBestAsync_StoredScore_ReturnsScoreAndMoment()
    {
        await SubmitAsync(4, 520, Now);

        Assert.Equal(new BestLevelScore(520, Now), await GetBestAsync(_userId, 4));
    }

    [Fact]
    public async Task GetBestAsync_NoScoreOnTheLevel_ReturnsNull()
    {
        await SubmitAsync(4, 520, Now);

        Assert.Null(await GetBestAsync(_userId, 5));
    }

    [Fact]
    public async Task CountRankedAboveAsync_TiesOnScoreAndMoment_CountsLikeTheRanking()
    {
        var level = UniqueLevel();
        var (low, high, tieLater, tieEarlierB, tieEarlierA) = await SubmitTiedRankingAsync(level);

        Assert.Equal(0, await CountRankedAboveAsync(level, high));
        Assert.Equal(1, await CountRankedAboveAsync(level, tieEarlierA));
        Assert.Equal(2, await CountRankedAboveAsync(level, tieEarlierB));
        Assert.Equal(3, await CountRankedAboveAsync(level, tieLater));
        Assert.Equal(4, await CountRankedAboveAsync(level, low));
    }

    [Fact]
    public async Task CountRankedAboveAsync_MatchesThePositionsOfGetRankedAsync()
    {
        var level = UniqueLevel();
        await SubmitTiedRankingAsync(level);

        foreach (var entry in await GetRankedAsync(level, 0, 10))
        {
            Assert.Equal(entry.Position - 1, await CountRankedAboveAsync(level, entry.UserId));
        }
    }

    [Fact]
    public async Task CountAsync_CountsOnlyTheScoresOfTheLevel()
    {
        var level = UniqueLevel();
        await SubmitAscendingScoresAsync(level, 7);
        await SubmitAsync(PlayerId(1), UniqueLevel(), 100, Now);

        await using var dbContext = database.CreateDbContext();
        Assert.Equal(7, await new LevelScoreRepository(dbContext).CountAsync(level, TestContext.Current.CancellationToken));
    }

    /// <summary>A level no other test writes to, since rankings span all players of a level.</summary>
    private static int UniqueLevel() => Random.Shared.Next(1_000, int.MaxValue);

    /// <summary>Player ids whose order is the same in .NET and in PostgreSQL's uuid comparison.</summary>
    private static Guid PlayerId(int n) => Guid.Parse($"00000000-0000-0000-0000-{n:D12}");

    private async Task SubmitAsync(Guid userId, int level, int score, DateTimeOffset achievedAt)
    {
        await using var dbContext = database.CreateDbContext();
        await new LevelScoreRepository(dbContext).SubmitAsync(userId, level, score, achievedAt, TestContext.Current.CancellationToken);
    }

    /// <summary>Stores 5 scores on <paramref name="level"/>, three of them tied on 500.</summary>
    private async Task<(Guid Low, Guid High, Guid TieLater, Guid TieEarlierB, Guid TieEarlierA)> SubmitTiedRankingAsync(int level)
    {
        var low = PlayerId(1);
        var high = PlayerId(2);
        var tieLater = PlayerId(3);
        var tieEarlierB = PlayerId(5);
        var tieEarlierA = PlayerId(4);
        await SubmitAsync(low, level, 100, Now);
        await SubmitAsync(high, level, 900, Now);
        await SubmitAsync(tieLater, level, 500, Now.AddDays(1));
        await SubmitAsync(tieEarlierB, level, 500, Now);
        await SubmitAsync(tieEarlierA, level, 500, Now);
        return (low, high, tieLater, tieEarlierB, tieEarlierA);
    }

    /// <summary>Stores <paramref name="count"/> scores: player <c>i</c> scores <c>i * 10</c>.</summary>
    private async Task SubmitAscendingScoresAsync(int level, int count)
    {
        for (var i = 1; i <= count; i++)
        {
            await SubmitAsync(PlayerId(i), level, i * 10, Now);
        }
    }

    private async Task<IReadOnlyList<RankedLevelScore>> GetRankedAsync(int level, int offset, int count)
    {
        await using var dbContext = database.CreateDbContext();
        return await new LevelScoreRepository(dbContext).GetRankedAsync(level, offset, count, TestContext.Current.CancellationToken);
    }

    private async Task<BestLevelScore?> GetBestAsync(Guid userId, int level)
    {
        await using var dbContext = database.CreateDbContext();
        return await new LevelScoreRepository(dbContext).GetBestAsync(userId, level, TestContext.Current.CancellationToken);
    }

    /// <summary>Counts what ranks above <paramref name="userId"/>'s stored score, as the handler does.</summary>
    private async Task<int> CountRankedAboveAsync(int level, Guid userId)
    {
        var best = await GetBestAsync(userId, level) ?? throw new InvalidOperationException("No stored score.");
        await using var dbContext = database.CreateDbContext();
        return await new LevelScoreRepository(dbContext).CountRankedAboveAsync(
            level, best.Score, best.AchievedAt, userId, TestContext.Current.CancellationToken);
    }

    private async Task<LevelScoreSubmission> SubmitAsync(int level, int score, DateTimeOffset achievedAt)
    {
        await using var dbContext = database.CreateDbContext();
        return await new LevelScoreRepository(dbContext).SubmitAsync(_userId, level, score, achievedAt, TestContext.Current.CancellationToken);
    }

    private async Task<LevelScoreEntity> SingleStoredAsync(int level)
    {
        await using var dbContext = database.CreateDbContext();
        return await dbContext.LevelScores
            .AsNoTracking()
            .SingleAsync(s => s.UserId == _userId && s.Level == level, TestContext.Current.CancellationToken);
    }
}
