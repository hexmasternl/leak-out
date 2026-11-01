using HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;
using HexMaster.LeakOut.DailyPipes.DomainModels;
using HexMaster.LeakOut.DailyPipes.Tests.Helpers;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.DailyPipes.Tests.Data;

/// <summary>Runs the repository against a real, migrated PostgreSQL database.</summary>
[Collection(PostgreSqlCollection.Name)]
public sealed class DailyPipeRepositoryTests(PostgreSqlFixture database)
{
    private static int _nextDay = 1_000;

    private static readonly DateTimeOffset Now = new(2026, 11, 1, 12, 0, 0, TimeSpan.Zero);

    private readonly Guid _userId = Guid.CreateVersion7();

    // Every test plays on dates of its own, so tests never see each other's pipes and scores.
    private readonly DateOnly _date = DateOnly.FromDayNumber(DateOnly.FromDateTime(new DateTime(2100, 1, 1)).DayNumber
        + Interlocked.Add(ref _nextDay, 30));

    private CancellationToken Ct => TestContext.Current.CancellationToken;

    [Fact]
    public async Task PublishAsync_NewDate_StoresThePipe()
    {
        await using var db = database.CreateDbContext();
        var pipe = DailyPipe.Create(_date, """{"id":1}""", 77, 900, Now);

        Assert.True(await new DailyPipeRepository(db).PublishAsync(pipe, _date.AddDays(-7), Ct));

        var stored = await new DailyPipeRepository(db).GetPipeAsync(_date, Ct);
        Assert.NotNull(stored);
        Assert.Equal("""{"id":1}""", stored.LevelJson);
        Assert.Equal(77, stored.StackSeed);
        Assert.Equal(900, stored.MaxScore);
        Assert.Equal(Now, stored.PublishedAt);
    }

    [Fact]
    public async Task PublishAsync_DateAlreadyPublished_LeavesThePipeUnchanged()
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        await repository.PublishAsync(DailyPipe.Create(_date, """{"id":1}""", 1, 900, Now), _date.AddDays(-7), Ct);

        var published = await repository.PublishAsync(DailyPipe.Create(_date, """{"id":2}""", 2, 500, Now.AddHours(1)), _date.AddDays(-7), Ct);

        Assert.False(published);
        var stored = await repository.GetPipeAsync(_date, Ct);
        Assert.Equal("""{"id":1}""", stored!.LevelJson);
        Assert.Equal(1, stored.StackSeed);
    }

    [Fact]
    public async Task PublishAsync_DeletesOlderScoresAndOldPipes_KeepsStreaks()
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        var yesterday = _date.AddDays(-1);
        var lastWeek = _date.AddDays(-8);
        await repository.PublishAsync(DailyPipe.Create(lastWeek, "{}", 1, 100, Now), lastWeek.AddDays(-7), Ct);
        await repository.PublishAsync(DailyPipe.Create(yesterday, "{}", 1, 100, Now), yesterday.AddDays(-7), Ct);
        await repository.SubmitScoreAsync(yesterday, _userId, 80, 2, Now, Ct);
        await repository.RecordWinAsync(_userId, yesterday, Ct);

        await repository.PublishAsync(DailyPipe.Create(_date, "{}", 1, 100, Now), _date.AddDays(-7), Ct);

        Assert.Null(await repository.GetBestAsync(yesterday, _userId, Ct));
        Assert.Null(await repository.GetPipeAsync(lastWeek, Ct));
        Assert.NotNull(await repository.GetPipeAsync(yesterday, Ct));
        var streak = await repository.GetStreakAsync(_userId, Ct);
        Assert.Equal(1, streak!.Current);
        Assert.Equal(yesterday, streak.LastWinDate);
    }

    [Fact]
    public async Task SubmitScoreAsync_FirstResult_IsStoredAsNewBest()
    {
        await using var db = database.CreateDbContext();

        var result = await new DailyPipeRepository(db).SubmitScoreAsync(_date, _userId, 300, 1, Now, Ct);

        Assert.Equal(new DailyScoreSubmission(300, 1, Now, IsNewBest: true), result);
        Assert.Equal(new BestDailyScore(300, 1, Now), await new DailyPipeRepository(db).GetBestAsync(_date, _userId, Ct));
    }

    [Fact]
    public async Task SubmitScoreAsync_HigherScore_ReplacesTheBest()
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        await repository.SubmitScoreAsync(_date, _userId, 300, 1, Now, Ct);

        var result = await repository.SubmitScoreAsync(_date, _userId, 420, 2, Now.AddHours(1), Ct);

        Assert.Equal(new DailyScoreSubmission(420, 2, Now.AddHours(1), IsNewBest: true), result);
    }

    [Theory]
    [InlineData(420)]
    [InlineData(300)]
    public async Task SubmitScoreAsync_EqualOrLowerScore_KeepsTheBest(int score)
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        await repository.SubmitScoreAsync(_date, _userId, 420, 2, Now, Ct);

        var result = await repository.SubmitScoreAsync(_date, _userId, score, 1, Now.AddHours(1), Ct);

        Assert.Equal(new DailyScoreSubmission(420, 2, Now, IsNewBest: false), result);
    }

    [Fact]
    public async Task GetTopAsync_RanksByScoreThenEarliest()
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        var early = Guid.CreateVersion7();
        var late = Guid.CreateVersion7();
        var low = Guid.CreateVersion7();
        await repository.SubmitScoreAsync(_date, late, 400, 2, Now.AddMinutes(5), Ct);
        await repository.SubmitScoreAsync(_date, low, 100, 1, Now, Ct);
        await repository.SubmitScoreAsync(_date, early, 400, 2, Now, Ct);

        var top = await repository.GetTopAsync(_date, 2, Ct);

        Assert.Equal(
            [new RankedDailyScore(1, early, 400, 2, Now), new RankedDailyScore(2, late, 400, 2, Now.AddMinutes(5))],
            top);
        Assert.Equal(0, await repository.CountRankedAboveAsync(_date, 400, Now, early, Ct));
        Assert.Equal(1, await repository.CountRankedAboveAsync(_date, 400, Now.AddMinutes(5), late, Ct));
        Assert.Equal(2, await repository.CountRankedAboveAsync(_date, 100, Now, low, Ct));
    }

    [Fact]
    public async Task RecordWinAsync_CountsConsecutiveDaysOnce()
    {
        await using var db = database.CreateDbContext();
        var repository = new DailyPipeRepository(db);
        Assert.Null(await repository.GetStreakAsync(_userId, Ct));

        await repository.RecordWinAsync(_userId, _date, Ct);
        await repository.RecordWinAsync(_userId, _date, Ct);
        var streak = await repository.RecordWinAsync(_userId, _date.AddDays(1), Ct);

        Assert.Equal(2, streak.Current);
        Assert.Equal(2, streak.Best);
        var stored = await repository.GetStreakAsync(_userId, Ct);
        Assert.Equal(2, stored!.Current);
        Assert.Equal(_date.AddDays(1), stored.LastWinDate);
    }

    [Fact]
    public async Task RecordWinAsync_ConcurrentWins_CountOnce()
    {
        var wins = Enumerable.Range(0, 5).Select(async _ =>
        {
            await using var db = database.CreateDbContext();
            return await new DailyPipeRepository(db).RecordWinAsync(_userId, _date, Ct);
        });

        await Task.WhenAll(wins);

        await using var check = database.CreateDbContext();
        var stored = await new DailyPipeRepository(check).GetStreakAsync(_userId, Ct);
        Assert.Equal(1, stored!.Current);
    }

    [Fact]
    public async Task Migrator_CreatesTablesWithConstraintsAndIndexes()
    {
        await using var db = database.CreateDbContext();

        Assert.Empty(await db.Database.GetPendingMigrationsAsync(Ct));
        var indexes = await db.Database
            .SqlQuery<string>($"""SELECT indexdef AS "Value" FROM pg_indexes WHERE tablename = 'daily_scores'""")
            .ToListAsync(Ct);
        Assert.Contains(indexes, i => i.Contains("UNIQUE INDEX ix_daily_scores_date_user_id", StringComparison.Ordinal)
            && i.EndsWith("(date, user_id)", StringComparison.Ordinal));
        Assert.Contains(indexes, i => i.EndsWith("(date, score DESC, achieved_at)", StringComparison.Ordinal));
        var constraints = await db.Database
            .SqlQuery<string>($"""
                SELECT conname AS "Value" FROM pg_constraint
                 WHERE conrelid IN ('daily_pipes'::regclass, 'daily_scores'::regclass, 'daily_streaks'::regclass)
                """)
            .ToListAsync(Ct);
        Assert.Superset(
            new HashSet<string> { "pk_daily_pipes", "pk_daily_scores", "pk_daily_streaks", "ck_daily_scores_stars", "ck_daily_streaks_last_win" },
            constraints.ToHashSet());
    }

    [Fact]
    public void Model_HasNoChangesMissingFromMigrations()
    {
        using var dbContext = new DailyPipesDbContextDesignTimeFactory().CreateDbContext([]);

        Assert.False(
            dbContext.Database.HasPendingModelChanges(),
            "The EF model changed without a migration; run 'dotnet ef migrations add'.");
    }

    [Fact]
    public async Task Migrator_StopAsync_DoesNothing() =>
        await new DailyPipesDatabaseMigrator(null!).StopAsync(Ct);
}
