using System.Diagnostics;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Factories;
using HexMaster.LeakOut.Players.Tests.Helpers;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.Services;

[Collection(TelemetryCollection.Name)]
public sealed class UserResolverTests : IDisposable
{
    private const string SubjectId = "g1234567890";

    private readonly Mock<IPlayerRepository> _repository = new();
    private readonly MemoryCache _cache = new(new MemoryCacheOptions());
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<UserResolver> _logger = new();

    [Fact]
    public async Task ResolveAsync_RegisteredPlayer_ReturnsInternalId()
    {
        var player = new PlayerFaker().Generate();
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ReturnsAsync(player);

        var playerId = await CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Equal(player.Id, playerId);
        var activity = AssertResolution(PlayersMetrics.ResolutionCacheMiss);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(player.Id, activity.GetTagItem("player.id"));
    }

    [Fact]
    public async Task ResolveAsync_UnknownPlayer_ReturnsNull()
    {
        var playerId = await CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Null(playerId);
        var activity = AssertResolution(PlayersMetrics.ResolutionUnknown);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Null(activity.GetTagItem("player.id"));

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Debug, log.Level);
        AssertSubjectIdNotInTelemetry();
    }

    [Fact]
    public async Task ResolveAsync_SecondCall_IsServedFromCache()
    {
        var player = new PlayerFaker().Generate();
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ReturnsAsync(player);
        var resolver = CreateResolver();

        await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);
        var playerId = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Equal(player.Id, playerId);
        _repository.Verify(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()), Times.Once);
        Assert.Equal(
            [PlayersMetrics.ResolutionCacheHit, PlayersMetrics.ResolutionCacheMiss],
            _telemetry.Measurements.Select(m => (string)m.Tags["outcome"]!).Order());
        Assert.Contains(_telemetry.Activities, a => Equals(a.GetTagItem("players.resolution"), PlayersMetrics.ResolutionCacheHit)
            && Equals(a.GetTagItem("player.id"), player.Id));
        AssertSubjectIdNotInTelemetry();
    }

    [Fact]
    public async Task ResolveAsync_AfterMiss_FindsNewlyRegisteredPlayer()
    {
        var player = new PlayerFaker().Generate();
        _repository.SetupSequence(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Player?)null)
            .ReturnsAsync(player);
        var resolver = CreateResolver();

        var first = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);
        var second = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Null(first);
        Assert.Equal(player.Id, second);
    }

    [Fact]
    public async Task ResolveAsync_RepositoryFails_RecordsErrorAndRethrows()
    {
        var failure = new InvalidOperationException("database down");
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ThrowsAsync(failure);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken));

        var activity = AssertResolution(PlayersMetrics.OutcomeError);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Contains(activity.Events, e => e.Name == "exception");

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Error, log.Level);
        Assert.Same(failure, log.Exception);
        AssertSubjectIdNotInTelemetry();
    }

    [Fact]
    public async Task ResolveAsync_Cancelled_RecordsNoOutcomeAndNoError()
    {
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken));

        Assert.Empty(_telemetry.Measurements);
        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Theory]
    [InlineData("")]
    [InlineData(" ")]
    public async Task ResolveAsync_BlankSubjectId_ThrowsArgumentException(string subjectId)
    {
        await Assert.ThrowsAsync<ArgumentException>(
            () => CreateResolver().ResolveAsync(subjectId, TestContext.Current.CancellationToken));
    }

    public void Dispose()
    {
        _telemetry.Dispose();
        _cache.Dispose();
    }

    private Activity AssertResolution(string outcome)
    {
        var measurement = Assert.Single(_telemetry.Measurements);
        Assert.Equal(PlayersMetrics.UserResolutionsInstrumentName, measurement.Instrument);
        Assert.Equal(outcome, Assert.Single(measurement.Tags).Value);

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(PlayersMetrics.ResolveUserActivityName, activity.OperationName);
        Assert.Equal(outcome, activity.GetTagItem("players.resolution"));
        return activity;
    }

    private void AssertSubjectIdNotInTelemetry()
    {
        Assert.DoesNotContain(_telemetry.Activities, a => a.TagObjects.Any(t => t.Value?.ToString()?.Contains(SubjectId) == true));
        Assert.DoesNotContain(_telemetry.Measurements, m => m.Tags.Values.Any(v => v?.ToString()?.Contains(SubjectId) == true));
        Assert.DoesNotContain(_logger.Collector.GetSnapshot(), l => l.Message.Contains(SubjectId));
    }

    private UserResolver CreateResolver() => new(_repository.Object, _cache, _telemetry.Metrics, _logger);
}
