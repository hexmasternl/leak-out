using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Features.SubmitScore;
using HexMaster.LeakOut.Scores.Observability;
using HexMaster.LeakOut.Scores.Tests.Factories;
using HexMaster.LeakOut.Scores.Tests.Helpers;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests.SubmitScore;

[Collection(TelemetryCollection.Name)]
public sealed class SubmitScoreCommandHandlerTests : IDisposable
{
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);

    private readonly Mock<ILevelScoreRepository> _repository = new();
    private readonly Mock<IClock> _clock = new();
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<SubmitScoreCommandHandler> _logger = new();

    public SubmitScoreCommandHandlerTests()
    {
        _clock.Setup(c => c.UtcNow).Returns(Now);
    }

    [Fact]
    public async Task Handle_FirstScore_ReturnsItAsNewBest()
    {
        var userId = Guid.CreateVersion7();
        RepositoryReturns(userId, level: 4, score: 520, new LevelScoreSubmission(520, IsNewBest: true));

        var result = await CreateHandler().Handle(new SubmitScoreCommand(userId, 4, 520), TestContext.Current.CancellationToken);

        Assert.Equal(4, result.Level);
        Assert.Equal(520, result.BestScore);
        Assert.True(result.IsNewBest);
        _repository.Verify(r => r.SubmitAsync(userId, 4, 520, Now, It.IsAny<CancellationToken>()), Times.Once);
        AssertSubmissionRecorded(ScoresMetrics.OutcomeNewBest);

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ScoresMetrics.SubmitScoreActivityName, activity.OperationName);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(userId, activity.GetTagItem("player.id"));
        Assert.Equal(4, activity.GetTagItem("score.level"));
        Assert.Equal(520, activity.GetTagItem("score.value"));
        Assert.Equal(true, activity.GetTagItem("score.new_best"));
    }

    [Fact]
    public async Task Handle_HigherScore_ReturnsNewBest()
    {
        var stored = new LevelScoreFaker(level: 4, score: 520).Generate();
        RepositoryReturns(stored.UserId, 4, 540, new LevelScoreSubmission(540, IsNewBest: true));

        var result = await CreateHandler().Handle(new SubmitScoreCommand(stored.UserId, 4, 540), TestContext.Current.CancellationToken);

        Assert.Equal(new(4, 540, true), result);
        _repository.Verify(r => r.SubmitAsync(stored.UserId, 4, 540, Now, It.IsAny<CancellationToken>()), Times.Once);
        AssertSubmissionRecorded(ScoresMetrics.OutcomeNewBest);

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Information, log.Level);
        Assert.Equal($"Player {stored.UserId} set a new best score 540 on level 4", log.Message);
    }

    [Theory]
    [InlineData(520)]
    [InlineData(300)]
    public async Task Handle_EqualOrLowerScore_ReturnsStoredBest(int score)
    {
        var stored = new LevelScoreFaker(level: 4, score: 520).Generate();
        RepositoryReturns(stored.UserId, 4, score, new LevelScoreSubmission(stored.Score, IsNewBest: false));

        var result = await CreateHandler().Handle(new SubmitScoreCommand(stored.UserId, 4, score), TestContext.Current.CancellationToken);

        Assert.Equal(new(4, 520, false), result);
        _repository.Verify(r => r.SubmitAsync(stored.UserId, 4, score, Now, It.IsAny<CancellationToken>()), Times.Once);
        AssertSubmissionRecorded(ScoresMetrics.OutcomeNotImproved);
        Assert.Equal(false, Assert.Single(_telemetry.Activities).GetTagItem("score.new_best"));
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_NullCommand_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _repository.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Handle_RepositoryFails_RecordsErrorAndRethrows()
    {
        var failure = new InvalidOperationException("database down");
        _repository.Setup(r => r.SubmitAsync(It.IsAny<Guid>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(failure);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => CreateHandler().Handle(new SubmitScoreCommand(Guid.CreateVersion7(), 1, 10), TestContext.Current.CancellationToken));

        AssertSubmissionRecorded(ScoresMetrics.OutcomeError);
        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Equal("database down", activity.StatusDescription);
        Assert.Contains(activity.Events, e => e.Name == "exception");

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Error, log.Level);
        Assert.Same(failure, log.Exception);
    }

    [Fact]
    public async Task Handle_Cancelled_RecordsNoFailure()
    {
        _repository.Setup(r => r.SubmitAsync(It.IsAny<Guid>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => CreateHandler().Handle(new SubmitScoreCommand(Guid.CreateVersion7(), 1, 10), TestContext.Current.CancellationToken));

        Assert.Empty(_telemetry.Measurements);
        Assert.Empty(_telemetry.DoubleMeasurements);
        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    public void Dispose() => _telemetry.Dispose();

    private void RepositoryReturns(Guid userId, int level, int score, LevelScoreSubmission submission) =>
        _repository.Setup(r => r.SubmitAsync(userId, level, score, Now, It.IsAny<CancellationToken>()))
            .ReturnsAsync(submission);

    private void AssertSubmissionRecorded(string outcome)
    {
        var measurement = Assert.Single(_telemetry.Measurements);
        Assert.Equal(ScoresMetrics.SubmissionsInstrumentName, measurement.Instrument);
        Assert.Equal(1, measurement.Value);
        // The outcome is the only attribute: no player id, level or score.
        Assert.Equal(outcome, Assert.Single(measurement.Tags).Value);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(ScoresMetrics.SubmissionDurationInstrumentName, duration.Instrument);
        Assert.True(duration.Value >= 0);
        Assert.Equal(outcome, Assert.Single(duration.Tags).Value);
    }

    private SubmitScoreCommandHandler CreateHandler() => new(_repository.Object, _clock.Object, _telemetry.Metrics, _logger);
}
