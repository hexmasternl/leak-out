using System.Diagnostics;
using Bogus;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;
using HexMaster.LeakOut.Scores.Observability;
using HexMaster.LeakOut.Scores.Tests.Helpers;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests.GetPublicLevelTopScores;

[Collection(TelemetryCollection.Name)]
public sealed class GetPublicLevelTopScoresQueryHandlerTests : IDisposable
{
    private const int Level = 3;

    private static readonly DateTimeOffset AchievedAt = new(2026, 10, 1, 14, 30, 0, TimeSpan.Zero);

    private readonly Faker _faker = new();
    private readonly Mock<ILevelScoreRepository> _repository = new();
    private readonly Mock<IPlayerProfileService> _playerProfiles = new();
    private readonly MemoryCache _cache = new(new MemoryCacheOptions());
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<GetPublicLevelTopScoresQueryHandler> _logger = new();

    [Fact]
    public async Task Handle_RankedScores_ReturnsPositionsNamesScoresAndMomentsInRankingOrder()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 600, AchievedAt.AddDays(3)),
            new RankedLevelScore(2, Guid.CreateVersion7(), 500, AchievedAt),
            new RankedLevelScore(3, Guid.CreateVersion7(), 500, AchievedAt.AddHours(2)),
        };
        var names = RepositoryReturns(ranked);

        var result = await Handle(Level);

        Assert.Equal(Level, result.Level);
        Assert.Equal(
            ranked.Select(s => new PublicLeaderboardEntryDto(s.Position, names[s.UserId], s.Score, s.AchievedAt)),
            result.Entries);
    }

    [Fact]
    public async Task Handle_PassesTheMomentTheScoreWasReceivedThrough()
    {
        RepositoryReturns([new RankedLevelScore(1, Guid.CreateVersion7(), 540, AchievedAt)]);

        var result = await Handle(Level);

        Assert.Equal(new DateTimeOffset(2026, 10, 1, 14, 30, 0, TimeSpan.Zero), Assert.Single(result.Entries).AchievedAt);
    }

    [Fact]
    public async Task Handle_AsksRepositoryForTheTop25()
    {
        RepositoryReturns([]);

        await Handle(Level);

        _repository.Verify(r => r.GetRankedAsync(Level, 0, 25, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_LevelWithoutScores_ReturnsEmptyEntriesWithoutNameLookup()
    {
        RepositoryReturns([]);

        var result = await Handle(7);

        Assert.Equal(7, result.Level);
        Assert.Empty(result.Entries);
        _playerProfiles.VerifyNoOtherCalls();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("  ")]
    public async Task Handle_PlayerWithoutDisplayName_ShowsUnknownPlayer(string? displayName)
    {
        var known = new RankedLevelScore(1, Guid.CreateVersion7(), 900, AchievedAt);
        var nameless = new RankedLevelScore(2, Guid.CreateVersion7(), 800, AchievedAt);
        var names = new Dictionary<Guid, string> { [known.UserId] = "Ann" };
        if (displayName is not null)
        {
            names[nameless.UserId] = displayName;
        }

        RepositoryReturns([known, nameless], names);

        var result = await Handle(Level);

        Assert.Equal(["Ann", "Unknown player"], result.Entries.Select(e => e.DisplayName));
    }

    [Fact]
    public async Task Handle_SameLevelTwice_ReadsTheDatabaseOnce()
    {
        RepositoryReturns([new RankedLevelScore(1, Guid.CreateVersion7(), 900, AchievedAt)]);

        var first = await Handle(Level);
        var second = await Handle(Level);

        Assert.Same(first, second);
        _repository.Verify(r => r.GetRankedAsync(Level, 0, 25, It.IsAny<CancellationToken>()), Times.Once);
        _playerProfiles.Verify(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()), Times.Once);
        Assert.Equal(
            [false, true],
            _telemetry.Activities.OrderBy(a => a.StartTimeUtc).Select(a => a.GetTagItem("leaderboard.cache_hit")));
    }

    [Fact]
    public async Task Handle_LevelWithoutScores_IsCachedToo()
    {
        RepositoryReturns([]);

        await Handle(7);
        await Handle(7);

        _repository.Verify(r => r.GetRankedAsync(7, 0, 25, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_DifferentLevels_AreCachedSeparately()
    {
        RepositoryReturns([]);

        var first = await Handle(1);
        var second = await Handle(2);

        Assert.Equal(1, first.Level);
        Assert.Equal(2, second.Level);
        _repository.Verify(r => r.GetRankedAsync(1, 0, 25, It.IsAny<CancellationToken>()), Times.Once);
        _repository.Verify(r => r.GetRankedAsync(2, 0, 25, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_CachesTheResultFor30Minutes()
    {
        RepositoryReturns([]);
        var entry = new Mock<ICacheEntry>();
        entry.SetupAllProperties();
        var cache = new Mock<IMemoryCache>();
        object? none = null;
        cache.Setup(c => c.TryGetValue(It.IsAny<object>(), out none)).Returns(false);
        cache.Setup(c => c.CreateEntry(GetPublicLevelTopScoresQueryHandler.CacheKey(Level))).Returns(entry.Object);

        var result = await CreateHandler(cache.Object).Handle(new GetPublicLevelTopScoresQuery(Level), TestContext.Current.CancellationToken);

        Assert.Equal(TimeSpan.FromMinutes(30), entry.Object.AbsoluteExpirationRelativeToNow);
        Assert.Same(result, entry.Object.Value);
        Assert.Null(entry.Object.SlidingExpiration);
    }

    [Fact]
    public async Task Handle_RepositoryFails_CachesNothingAndRecordsError()
    {
        var failure = new InvalidOperationException("database down");
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(failure);

        await Assert.ThrowsAsync<InvalidOperationException>(() => Handle(Level));

        Assert.False(_cache.TryGetValue(GetPublicLevelTopScoresQueryHandler.CacheKey(Level), out _));
        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Contains(activity.Events, e => e.Name == "exception");
        AssertLeaderboardRecorded(ScoresMetrics.OutcomeError);

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Error, log.Level);
        Assert.Same(failure, log.Exception);
        Assert.Equal("public_top leaderboard request failed unexpectedly", log.Message);
    }

    [Fact]
    public async Task Handle_AfterAFailure_ReadsTheDatabaseAgain()
    {
        _repository
            .SetupSequence(r => r.GetRankedAsync(Level, 0, 25, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("database down"))
            .ReturnsAsync([]);

        await Assert.ThrowsAsync<InvalidOperationException>(() => Handle(Level));
        var result = await Handle(Level);

        Assert.Empty(result.Entries);
        _repository.Verify(r => r.GetRankedAsync(Level, 0, 25, It.IsAny<CancellationToken>()), Times.Exactly(2));
    }

    [Fact]
    public async Task Handle_FilledLeaderboard_RecordsSpanAndMetricsWithoutPlayerData()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 900, AchievedAt),
            new RankedLevelScore(2, Guid.CreateVersion7(), 800, AchievedAt),
        };
        var names = RepositoryReturns(ranked);

        await Handle(Level);

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ScoresMetrics.GetPublicLevelTopScoresActivityName, activity.OperationName);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(Level, activity.GetTagItem("score.level"));
        Assert.Equal(ScoresMetrics.LeaderboardKindPublicTop, activity.GetTagItem("leaderboard.kind"));
        Assert.Equal(2, activity.GetTagItem("leaderboard.entries"));
        Assert.Equal(false, activity.GetTagItem("leaderboard.cache_hit"));
        Assert.Null(activity.GetTagItem("player.id"));
        Assert.DoesNotContain(activity.TagObjects, t => t.Value is string value && names.ContainsValue(value));
        AssertLeaderboardRecorded(ScoresMetrics.OutcomeSuccess);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_Cancelled_RecordsNoFailure()
    {
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(() => Handle(Level));

        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        Assert.Empty(_telemetry.Measurements);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_NullQuery_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _repository.VerifyNoOtherCalls();
    }

    private void AssertLeaderboardRecorded(string outcome)
    {
        // Only the kind and the outcome: no level, player or score.
        var expectedTags = new Dictionary<string, object?> { ["leaderboard.kind"] = ScoresMetrics.LeaderboardKindPublicTop, ["outcome"] = outcome };

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(ScoresMetrics.LeaderboardRequestsInstrumentName, count.Instrument);
        Assert.Equal(expectedTags, count.Tags);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(ScoresMetrics.LeaderboardDurationInstrumentName, duration.Instrument);
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

    private Task<PublicLevelLeaderboardDto> Handle(int level) =>
        CreateHandler().Handle(new GetPublicLevelTopScoresQuery(level), TestContext.Current.CancellationToken);

    private GetPublicLevelTopScoresQueryHandler CreateHandler(IMemoryCache? cache = null) =>
        new(_repository.Object, _playerProfiles.Object, cache ?? _cache, _telemetry.Metrics, _logger);

    public void Dispose()
    {
        _cache.Dispose();
        _telemetry.Dispose();
    }
}
