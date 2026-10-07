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
