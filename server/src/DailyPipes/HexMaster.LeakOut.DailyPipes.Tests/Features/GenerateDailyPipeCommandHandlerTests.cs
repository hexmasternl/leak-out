using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.DomainModels;
using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using HexMaster.LeakOut.DailyPipes.Generation;
using HexMaster.LeakOut.DailyPipes.Observability;
using HexMaster.LeakOut.DailyPipes.Tests.Generation;
using HexMaster.LeakOut.DailyPipes.Tests.Helpers;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.DailyPipes.Tests.Features;

[Collection(TelemetryCollection.Name)]
public sealed class GenerateDailyPipeCommandHandlerTests : IDisposable
{
    private static readonly DateTimeOffset Midnight = new(2026, 11, 2, 0, 0, 3, TimeSpan.Zero);
    private static readonly DateOnly Today = new(2026, 11, 2);

    private readonly Mock<IDailyPipeRepository> _repository = new();
    private readonly Mock<IDailyLevelGenerator> _generator = new();
    private readonly Mock<IClock> _clock = new();
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<GenerateDailyPipeCommandHandler> _logger = new();

    public GenerateDailyPipeCommandHandlerTests()
    {
        _clock.Setup(c => c.UtcNow).Returns(Midnight);
        _repository
            .Setup(r => r.PublishAsync(It.IsAny<DailyPipe>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(true);
    }

    private static CancellationToken Ct => TestContext.Current.CancellationToken;

    [Fact]
    public async Task Handle_UnsolvableCandidateIsDiscarded_PublishesTheSolvableOne()
    {
        _generator.SetupSequence(g => g.Generate(Today))
            .Returns(Candidate("unsolvable_sealed.json", seed: 1))
            .Returns(Candidate("solvable_through_portals.json", seed: 2));

        var result = await CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct);

        Assert.Equal(new GenerateDailyPipeResult(Today, Published: true, Attempts: 2), result);
        _repository.Verify(r => r.PublishAsync(
            It.Is<DailyPipe>(p => p.Date == Today && p.StackSeed == 2 && p.LevelJson.Contains("Through the wall") && p.PublishedAt == Midnight),
            Today.AddDays(-7),
            It.IsAny<CancellationToken>()), Times.Once);
        Assert.Equal([DailyPipesMetrics.OutcomePublished], _telemetry.Outcomes(DailyPipesMetrics.GenerationRunsInstrumentName));
        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(2, activity.GetTagItem("daily_pipe.attempts"));
        Assert.Equal("2026-11-02", activity.GetTagItem("daily_pipe.date"));
        Assert.Contains(_logger.Collector.GetSnapshot(), l => l.Message.StartsWith("Published the daily pipe of", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Handle_InvalidCandidateIsDiscarded()
    {
        _generator.SetupSequence(g => g.Generate(Today))
            .Returns(Candidate("invalid_walled_finish.json", seed: 1))
            .Returns(Candidate("unsolvable_sealed.json", seed: 2, blocked: false));

        var result = await CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct);

        Assert.Equal(2, result.Attempts);
        _repository.Verify(r => r.PublishAsync(It.Is<DailyPipe>(p => p.StackSeed == 2), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_NoFeasibleCandidate_FailsWithoutPublishing()
    {
        _generator.Setup(g => g.Generate(Today)).Returns(Candidate("unsolvable_sealed.json", seed: 1));

        await Assert.ThrowsAsync<DailyPipeGenerationException>(() => CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct));

        _generator.Verify(g => g.Generate(Today), Times.Exactly(GenerateDailyPipeCommandHandler.MaxAttempts));
        _repository.Verify(r => r.PublishAsync(It.IsAny<DailyPipe>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()), Times.Never);
        Assert.Equal([DailyPipesMetrics.OutcomeFailed], _telemetry.Outcomes(DailyPipesMetrics.GenerationRunsInstrumentName));
        Assert.Equal(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        var log = Assert.Single(_logger.Collector.GetSnapshot(), l => l.Level == LogLevel.Error);
        Assert.IsType<DailyPipeGenerationException>(log.Exception);
    }

    [Fact]
    public async Task Handle_TodayAlreadyPublished_GeneratesNothingAndLeavesThePipe()
    {
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>()))
            .ReturnsAsync(DailyPipe.Create(Today, "{}", 5, 100, Midnight));

        var result = await CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct);

        Assert.Equal(new GenerateDailyPipeResult(Today, Published: false, Attempts: 0), result);
        _generator.VerifyNoOtherCalls();
        _repository.Verify(r => r.PublishAsync(It.IsAny<DailyPipe>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()), Times.Never);
        Assert.Equal([DailyPipesMetrics.OutcomeAlreadyPublished], _telemetry.Outcomes(DailyPipesMetrics.GenerationRunsInstrumentName));
    }

    [Fact]
    public async Task Handle_AnotherRunPublishedFirst_ReportsNotPublished()
    {
        _generator.Setup(g => g.Generate(Today)).Returns(Candidate("solvable_through_portals.json", seed: 1));
        _repository
            .Setup(r => r.PublishAsync(It.IsAny<DailyPipe>(), It.IsAny<DateOnly>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var result = await CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct);

        Assert.False(result.Published);
        Assert.Equal([DailyPipesMetrics.OutcomeAlreadyPublished], _telemetry.Outcomes(DailyPipesMetrics.GenerationRunsInstrumentName));
    }

    [Fact]
    public async Task Handle_Cancelled_IsNotRecordedAsAFailure()
    {
        _repository.Setup(r => r.GetPipeAsync(Today, It.IsAny<CancellationToken>())).ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(() => CreateHandler().Handle(new GenerateDailyPipeCommand(), Ct));

        Assert.Empty(_telemetry.Outcomes(DailyPipesMetrics.GenerationRunsInstrumentName));
    }

    public void Dispose() => _telemetry.Dispose();

    private GenerateDailyPipeCommandHandler CreateHandler() =>
        new(_repository.Object, _generator.Object, _clock.Object, _telemetry.Metrics, _logger);

    private static DailyPipeCandidate Candidate(string fixture, long seed, bool blocked = true)
    {
        var level = LevelJson.Parse(File.ReadAllText(Path.Combine(Fixtures.Directory, "levels", fixture)));
        return new DailyPipeCandidate(blocked ? level : level with { Blocked = [] }, seed, DelayPerRouteCellMs: 500);
    }
}
