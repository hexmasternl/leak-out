using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.DomainModels;
using HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;
using HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;
using HexMaster.LeakOut.DailyPipes.Observability;
using HexMaster.LeakOut.DailyPipes.Tests.Helpers;
using HexMaster.LeakOut.Players.Abstractions.Services;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.DailyPipes.Tests.Features;

/// <summary>The handlers behind the three daily pipe endpoints, with the repository mocked.</summary>
[Collection(TelemetryCollection.Name)]
public sealed class DailyPipeQueryAndSubmitHandlerTests : IDisposable
{
    private static readonly DateOnly Today = new(2026, 11, 2);
    private static readonly DateTimeOffset Now = new(2026, 11, 2, 18, 30, 0, TimeSpan.Zero);
    private static readonly DailyPipe Pipe = DailyPipe.Create(Today, """{"id":20261102,"name":"Daily"}""", 1234, 900, Now.AddHours(-18));

    private readonly Guid _userId = Guid.CreateVersion7();
    private readonly Mock<IDailyPipeRepository> _repository = new();
    private readonly Mock<IPlayerProfileService> _profiles = new();
    private readonly Mock<IClock> _clock = new();
    private readonly TelemetryCollector _telemetry = new();

    public DailyPipeQueryAndSubmitHandlerTests()
    {
        _clock.Setup(c => c.UtcNow).Returns(Now);
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>())).ReturnsAsync(Pipe);
    }

    private static CancellationToken Ct => TestContext.Current.CancellationToken;

    // GetTodaysDailyPipe

    [Fact]
    public async Task Today_Published_ReturnsLevelSeedStreakAndBest()
    {
        _repository.Setup(r => r.GetStreakAsync(_userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(DailyStreak.Restore(_userId, 3, 7, Today.AddDays(-1)));
        _repository.Setup(r => r.GetBestAsync(Today, _userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new BestDailyScore(380, 2, Now));

        var pipe = await TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct);

        Assert.NotNull(pipe);
        Assert.Equal(Today, pipe.Date);
        Assert.Equal(20261102, pipe.Level.GetProperty("id").GetInt32());
        Assert.Equal(1234, pipe.StackSeed);
        Assert.Equal(new DailyStreakDto(3, 7, WonToday: false), pipe.Streak);
        Assert.Equal(new DailyBestScoreDto(380, 2), pipe.BestScore);
        Assert.Equal([DailyPipesMetrics.OutcomeServed], _telemetry.Outcomes(DailyPipesMetrics.DownloadsInstrumentName));
    }

    [Fact]
    public async Task Today_MissedDay_ReportsZeroAndKeepsTheBestStreak()
    {
        _repository.Setup(r => r.GetStreakAsync(_userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(DailyStreak.Restore(_userId, 5, 5, new DateOnly(2026, 10, 30)));

        var pipe = await TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct);

        Assert.Equal(new DailyStreakDto(0, 5, WonToday: false), pipe!.Streak);
        Assert.Null(pipe.BestScore);
    }

    [Fact]
    public async Task Today_NewPlayer_HasNoStreak()
    {
        var pipe = await TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct);

        Assert.Equal(new DailyStreakDto(0, 0, WonToday: false), pipe!.Streak);
    }

    [Fact]
    public async Task Today_NotPublished_ReturnsNullAndNeverAnOlderPipe()
    {
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>())).ReturnsAsync((DailyPipe?)null);
        _repository.Setup(r => r.GetPipeAsync(Today.AddDays(-1), It.IsAny<CancellationToken>())).ReturnsAsync(Pipe);

        Assert.Null(await TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct));
        Assert.Equal([DailyPipesMetrics.OutcomeNotFound], _telemetry.Outcomes(DailyPipesMetrics.DownloadsInstrumentName));
    }

    [Fact]
    public async Task Today_UsesTheServersUtcDate()
    {
        // 23:59 in UTC-5 is already the next day in UTC.
        _clock.Setup(c => c.UtcNow).Returns(new DateTimeOffset(2026, 11, 1, 23, 59, 0, TimeSpan.FromHours(-5)));

        await TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct);

        _repository.Verify(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Today_RepositoryFails_RecordsTheError()
    {
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>())).ThrowsAsync(new InvalidOperationException("db down"));

        await Assert.ThrowsAsync<InvalidOperationException>(() => TodayHandler().Handle(new GetTodaysDailyPipeQuery(_userId), Ct));

        Assert.Equal([DailyPipesMetrics.OutcomeError], _telemetry.Outcomes(DailyPipesMetrics.DownloadsInstrumentName));
        Assert.Equal(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
    }

    // SubmitDailyScore

    [Fact]
    public async Task Submit_Win_StoresTheScoreCountsTheWinAndReturnsRankAndStreak()
    {
        _repository.Setup(r => r.SubmitScoreAsync(Today, _userId, 380, 2, Now, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new DailyScoreSubmission(380, 2, Now, IsNewBest: true));
        var streak = DailyStreak.Restore(_userId, 3, 3, Today.AddDays(-1));
        streak.RecordWin(Today);
        _repository.Setup(r => r.RecordWinAsync(_userId, Today, It.IsAny<CancellationToken>())).ReturnsAsync(streak);
        _repository.Setup(r => r.CountRankedAboveAsync(Today, 380, Now, _userId, It.IsAny<CancellationToken>())).ReturnsAsync(4);

        var outcome = await SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today, 380, 2), Ct);

        var result = Assert.IsType<SubmitDailyScoreOutcome.Accepted>(outcome).Result;
        Assert.Equal(new DailyScoreResultDto(Today, new DailyBestScoreDto(380, 2), true, 5, new DailyStreakDto(4, 4, WonToday: true)), result);
        Assert.Equal([DailyPipesMetrics.OutcomeNewBest], _telemetry.Outcomes(DailyPipesMetrics.SubmissionsInstrumentName));
    }

    [Fact]
    public async Task Submit_ZeroStars_IsNotAWin()
    {
        _repository.Setup(r => r.SubmitScoreAsync(Today, _userId, 50, 0, Now, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new DailyScoreSubmission(300, 1, Now.AddHours(-1), IsNewBest: false));

        var outcome = await SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today, 50, 0), Ct);

        var result = Assert.IsType<SubmitDailyScoreOutcome.Accepted>(outcome).Result;
        Assert.False(result.IsNewBest);
        Assert.Equal(new DailyBestScoreDto(300, 1), result.BestScore);
        Assert.Equal(new DailyStreakDto(0, 0, WonToday: false), result.Streak);
        Assert.Equal(1, result.Rank);
        _repository.Verify(r => r.RecordWinAsync(It.IsAny<Guid>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()), Times.Never);
        Assert.Equal([DailyPipesMetrics.OutcomeNotImproved], _telemetry.Outcomes(DailyPipesMetrics.SubmissionsInstrumentName));
    }

    [Fact]
    public async Task Submit_ForAnotherDate_HasEndedAndStoresNothing()
    {
        var outcome = await SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today.AddDays(-1), 380, 2), Ct);

        Assert.IsType<SubmitDailyScoreOutcome.Ended>(outcome);
        AssertNothingStored();
        Assert.Equal([DailyPipesMetrics.OutcomeEnded], _telemetry.Outcomes(DailyPipesMetrics.SubmissionsInstrumentName));
    }

    [Fact]
    public async Task Submit_TodayNotPublished_HasEnded()
    {
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>())).ReturnsAsync((DailyPipe?)null);

        Assert.IsType<SubmitDailyScoreOutcome.Ended>(await SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today, 380, 2), Ct));
        AssertNothingStored();
    }

    [Fact]
    public async Task Submit_AboveTheMaximum_IsRefused()
    {
        var outcome = await SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today, 901, 3), Ct);

        Assert.Equal(new SubmitDailyScoreOutcome.ScoreTooHigh(900), outcome);
        AssertNothingStored();
        Assert.Equal([DailyPipesMetrics.OutcomeRejected], _telemetry.Outcomes(DailyPipesMetrics.SubmissionsInstrumentName));
    }

    [Fact]
    public async Task Submit_RepositoryFails_RecordsTheError()
    {
        _repository.Setup(r => r.SubmitScoreAsync(Today, _userId, 380, 2, Now, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("db down"));

        await Assert.ThrowsAsync<InvalidOperationException>(() => SubmitHandler().Handle(new SubmitDailyScoreCommand(_userId, Today, 380, 2), Ct));

        Assert.Equal([DailyPipesMetrics.OutcomeError], _telemetry.Outcomes(DailyPipesMetrics.SubmissionsInstrumentName));
    }

    // GetDailyTopScores

    [Fact]
    public async Task Top_RanksWithNamesAndTheCallersRank()
    {
        var other = Guid.CreateVersion7();
        var nameless = Guid.CreateVersion7();
        _repository.Setup(r => r.GetTopAsync(Today, GetDailyTopScoresQueryHandler.TopCount, It.IsAny<CancellationToken>()))
            .ReturnsAsync([
                new RankedDailyScore(1, other, 400, 3, Now.AddHours(-2)),
                new RankedDailyScore(2, _userId, 400, 3, Now.AddHours(-1)),
                new RankedDailyScore(3, nameless, 100, 1, Now),
            ]);
        _profiles.Setup(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Dictionary<Guid, string> { [other] = "Ada", [_userId] = "Me", [nameless] = " " });
        _repository.Setup(r => r.GetBestAsync(Today, _userId, It.IsAny<CancellationToken>())).ReturnsAsync(new BestDailyScore(400, 3, Now.AddHours(-1)));
        _repository.Setup(r => r.CountRankedAboveAsync(Today, 400, Now.AddHours(-1), _userId, It.IsAny<CancellationToken>())).ReturnsAsync(1);

        var top = await TopHandler().Handle(new GetDailyTopScoresQuery(_userId, Today), Ct);

        Assert.NotNull(top);
        Assert.Equal(
            [
                new DailyLeaderboardEntryDto(1, "Ada", 400, 3, false),
                new DailyLeaderboardEntryDto(2, "Me", 400, 3, true),
                new DailyLeaderboardEntryDto(3, GetDailyTopScoresQueryHandler.UnknownPlayerName, 100, 1, false),
            ],
            top.Entries);
        Assert.Equal(new DailyPlayerRankDto(2, 400, 3), top.Player);
        Assert.Equal([DailyPipesMetrics.OutcomeServed], _telemetry.Outcomes(DailyPipesMetrics.LeaderboardRequestsInstrumentName));
    }

    [Fact]
    public async Task Top_NoResultsYet_IsEmptyWithoutLookingUpNames()
    {
        _repository.Setup(r => r.GetTopAsync(Today, It.IsAny<int>(), It.IsAny<CancellationToken>())).ReturnsAsync([]);

        var top = await TopHandler().Handle(new GetDailyTopScoresQuery(_userId, Today), Ct);

        Assert.Empty(top!.Entries);
        Assert.Null(top.Player);
        _profiles.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Top_UnknownDate_ReturnsNull()
    {
        Assert.Null(await TopHandler().Handle(new GetDailyTopScoresQuery(_userId, Today.AddDays(5)), Ct));
        Assert.Equal([DailyPipesMetrics.OutcomeNotFound], _telemetry.Outcomes(DailyPipesMetrics.LeaderboardRequestsInstrumentName));
    }

    [Fact]
    public async Task Top_RepositoryFails_RecordsTheError()
    {
        _repository.Setup(r => r.GetTopAsync(Today, It.IsAny<int>(), It.IsAny<CancellationToken>())).ThrowsAsync(new InvalidOperationException("db down"));

        await Assert.ThrowsAsync<InvalidOperationException>(() => TopHandler().Handle(new GetDailyTopScoresQuery(_userId, Today), Ct));

        Assert.Equal([DailyPipesMetrics.OutcomeError], _telemetry.Outcomes(DailyPipesMetrics.LeaderboardRequestsInstrumentName));
    }

    public void Dispose() => _telemetry.Dispose();

    private void AssertNothingStored()
    {
        _repository.Verify(
            r => r.SubmitScoreAsync(It.IsAny<DateOnly>(), It.IsAny<Guid>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _repository.Verify(r => r.RecordWinAsync(It.IsAny<Guid>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    private GetTodaysDailyPipeQueryHandler TodayHandler() =>
        new(_repository.Object, _clock.Object, _telemetry.Metrics, new FakeLogger<GetTodaysDailyPipeQueryHandler>());

    private SubmitDailyScoreCommandHandler SubmitHandler() =>
        new(_repository.Object, _clock.Object, _telemetry.Metrics, new FakeLogger<SubmitDailyScoreCommandHandler>());

    private GetDailyTopScoresQueryHandler TopHandler() =>
        new(_repository.Object, _profiles.Object, _telemetry.Metrics, new FakeLogger<GetDailyTopScoresQueryHandler>());
}
