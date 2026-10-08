using System.Diagnostics;
using Bogus;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using HexMaster.LeakOut.Scores.Observability;
using HexMaster.LeakOut.Scores.Tests.Helpers;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests.GetLevelTopScores;

[Collection(TelemetryCollection.Name)]
public sealed class GetLevelTopScoresQueryHandlerTests : IDisposable
{
    private const int Level = 3;

    private readonly Faker _faker = new();
    private readonly Mock<ILevelScoreRepository> _repository = new();
    private readonly Mock<IPlayerProfileService> _playerProfiles = new();
    private readonly Guid _requestingPlayer = Guid.CreateVersion7();
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<GetLevelTopScoresQueryHandler> _logger = new();

    [Fact]
    public async Task Handle_RankedScoresWithTies_KeepsRankingOrderAndPositions()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 600),
            new RankedLevelScore(2, Guid.CreateVersion7(), 500),
            new RankedLevelScore(3, Guid.CreateVersion7(), 500),
            new RankedLevelScore(4, Guid.CreateVersion7(), 500),
        };
        var names = RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(Level, result.Level);
        Assert.Equal(
            ranked.Select(s => new LeaderboardEntryDto(s.Position, names[s.UserId], s.Score, IsCurrentPlayer: false)),
            result.Entries);
    }

    [Fact]
    public async Task Handle_AsksRepositoryForTheTopTen()
    {
        RepositoryReturns([]);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        _repository.Verify(r => r.GetRankedAsync(Level, 0, 10, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_FewerThanTenScores_ReturnsThemAll()
    {
        var ranked = Enumerable.Range(1, 4)
            .Select(p => new RankedLevelScore(p, Guid.CreateVersion7(), 1000 - p * 10))
            .ToArray();
        RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal([1, 2, 3, 4], result.Entries.Select(e => e.Position));
    }

    [Fact]
    public async Task Handle_LevelWithoutScores_ReturnsEmptyEntriesWithoutNameLookup()
    {
        RepositoryReturns([]);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, 7), TestContext.Current.CancellationToken);

        Assert.Equal(7, result.Level);
        Assert.Empty(result.Entries);
        _playerProfiles.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Handle_RequestingPlayerRanked_MarksOnlyTheirEntry()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 900),
            new RankedLevelScore(2, _requestingPlayer, 800),
            new RankedLevelScore(3, Guid.CreateVersion7(), 700),
        };
        RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(2, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
    }

    [Fact]
    public async Task Handle_LooksUpAllNamesInOneBatch()
    {
        var ranked = Enumerable.Range(1, 10)
            .Select(p => new RankedLevelScore(p, Guid.CreateVersion7(), 1000 - p))
            .ToArray();
        RepositoryReturns(ranked);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        _playerProfiles.Verify(
            p => p.GetDisplayNamesAsync(
                It.Is<IReadOnlyCollection<Guid>>(ids => ids.SequenceEqual(ranked.Select(s => s.UserId))),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("  ")]
    public async Task Handle_PlayerWithoutDisplayName_ShowsUnknownPlayer(string? displayName)
    {
        var known = new RankedLevelScore(1, Guid.CreateVersion7(), 900);
        var nameless = new RankedLevelScore(2, Guid.CreateVersion7(), 800);
        var names = new Dictionary<Guid, string> { [known.UserId] = "Ann" };
        if (displayName is not null)
        {
            names[nameless.UserId] = displayName;
        }

        RepositoryReturns([known, nameless], names);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(["Ann", "Unknown player"], result.Entries.Select(e => e.DisplayName));
    }

    [Fact]
    public async Task Handle_NullQuery_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _repository.VerifyNoOtherCalls();
        _playerProfiles.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Handle_FilledLeaderboard_RecordsSpanAndMetrics()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 900),
            new RankedLevelScore(2, _requestingPlayer, 800),
        };
        var names = RepositoryReturns(ranked);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ScoresMetrics.GetLevelTopScoresActivityName, activity.OperationName);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(_requestingPlayer, activity.GetTagItem("player.id"));
        Assert.Equal(Level, activity.GetTagItem("score.level"));
        Assert.Equal(ScoresMetrics.LeaderboardKindTop, activity.GetTagItem("leaderboard.kind"));
        Assert.Equal(2, activity.GetTagItem("leaderboard.entries"));
        Assert.DoesNotContain(activity.TagObjects, t => t.Value is string value && names.ContainsValue(value));
        AssertLeaderboardRecorded(ScoresMetrics.OutcomeSuccess);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_LevelWithoutScores_RecordsZeroEntries()
    {
        RepositoryReturns([]);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(0, Assert.Single(_telemetry.Activities).GetTagItem("leaderboard.entries"));
        AssertLeaderboardRecorded(ScoresMetrics.OutcomeSuccess);
    }

    [Fact]
    public async Task Handle_RepositoryFails_RecordsErrorLogsAndRethrows()
    {
        var failure = new InvalidOperationException("database down");
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(failure);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken));

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Contains(activity.Events, e => e.Name == "exception");
        AssertLeaderboardRecorded(ScoresMetrics.OutcomeError);

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Error, log.Level);
        Assert.Same(failure, log.Exception);
        Assert.Equal("top leaderboard request failed unexpectedly", log.Message);
    }

    [Fact]
    public async Task Handle_Cancelled_RecordsNoFailure()
    {
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken));

        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        Assert.Empty(_telemetry.Measurements);
        Assert.Empty(_telemetry.DoubleMeasurements);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    private void AssertLeaderboardRecorded(string outcome)
    {
        // Only the kind and the outcome: no player id, level or score.
        var expectedTags = new Dictionary<string, object?> { ["leaderboard.kind"] = ScoresMetrics.LeaderboardKindTop, ["outcome"] = outcome };

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(ScoresMetrics.LeaderboardRequestsInstrumentName, count.Instrument);
        Assert.Equal(expectedTags, count.Tags);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(ScoresMetrics.LeaderboardDurationInstrumentName, duration.Instrument);
        Assert.True(duration.Value >= 0);
        Assert.Equal(expectedTags, duration.Tags);
    }

    private Dictionary<Guid, string> RepositoryReturns(RankedLevelScore[] ranked, Dictionary<Guid, string>? names = null)
    {
        names ??= ranked.ToDictionary(s => s.UserId, _ => _faker.Internet.UserName());
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(ranked);
        _playerProfiles
            .Setup(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(names);
        return names;
    }

    private GetLevelTopScoresQueryHandler CreateHandler() => new(_repository.Object, _playerProfiles.Object, _telemetry.Metrics, _logger);

    public void Dispose() => _telemetry.Dispose();
}
