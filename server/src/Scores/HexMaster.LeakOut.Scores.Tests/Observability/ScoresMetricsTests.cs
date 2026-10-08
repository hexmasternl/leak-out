using HexMaster.LeakOut.Scores.Observability;
using HexMaster.LeakOut.Scores.Tests.Helpers;

namespace HexMaster.LeakOut.Scores.Tests.Observability;

public sealed class ScoresMetricsTests : IDisposable
{
    private readonly TelemetryCollector _telemetry = new();

    [Theory]
    [InlineData(true, ScoresMetrics.OutcomeNewBest)]
    [InlineData(false, ScoresMetrics.OutcomeNotImproved)]
    public void ScoreSubmitted_CountsAndRecordsDurationWithOutcomeOnly(bool isNewBest, string outcome)
    {
        _telemetry.Metrics.ScoreSubmitted(isNewBest, TimeSpan.FromMilliseconds(40));

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(ScoresMetrics.SubmissionsInstrumentName, count.Instrument);
        Assert.Equal(outcome, Assert.Single(count.Tags).Value);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(ScoresMetrics.SubmissionDurationInstrumentName, duration.Instrument);
        Assert.Equal(0.04, duration.Value, precision: 6);
        Assert.Equal(outcome, Assert.Single(duration.Tags).Value);
    }

    [Fact]
    public void ScoreSubmissionFailed_CountsAndRecordsDurationWithErrorOutcome()
    {
        _telemetry.Metrics.ScoreSubmissionFailed(TimeSpan.FromSeconds(1));

        Assert.Equal(ScoresMetrics.OutcomeError, Assert.Single(Assert.Single(_telemetry.Measurements).Tags).Value);
        Assert.Equal(ScoresMetrics.OutcomeError, Assert.Single(Assert.Single(_telemetry.DoubleMeasurements).Tags).Value);
    }

    [Theory]
    [InlineData(ScoresMetrics.LeaderboardKindTop)]
    [InlineData(ScoresMetrics.LeaderboardKindAroundPlayer)]
    public void LeaderboardServed_CountsAndRecordsDurationByKindWithSuccessOutcome(string kind)
    {
        _telemetry.Metrics.LeaderboardServed(kind, TimeSpan.FromMilliseconds(15));

        AssertLeaderboardRecorded(kind, ScoresMetrics.OutcomeSuccess, 0.015);
    }

    [Theory]
    [InlineData(ScoresMetrics.LeaderboardKindTop)]
    [InlineData(ScoresMetrics.LeaderboardKindAroundPlayer)]
    public void LeaderboardFailed_CountsAndRecordsDurationByKindWithErrorOutcome(string kind)
    {
        _telemetry.Metrics.LeaderboardFailed(kind, TimeSpan.FromSeconds(3));

        AssertLeaderboardRecorded(kind, ScoresMetrics.OutcomeError, 3);
    }

    public void Dispose() => _telemetry.Dispose();

    private void AssertLeaderboardRecorded(string kind, string outcome, double seconds)
    {
        var expectedTags = new Dictionary<string, object?> { ["leaderboard.kind"] = kind, ["outcome"] = outcome };

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(ScoresMetrics.LeaderboardRequestsInstrumentName, count.Instrument);
        Assert.Equal(1, count.Value);
        Assert.Equal(expectedTags, count.Tags);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(ScoresMetrics.LeaderboardDurationInstrumentName, duration.Instrument);
        Assert.Equal(seconds, duration.Value, precision: 6);
        Assert.Equal(expectedTags, duration.Tags);
    }
}
