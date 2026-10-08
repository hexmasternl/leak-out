using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Players.Tests.Helpers;

namespace HexMaster.LeakOut.Players.Tests.Observability;

public sealed class PlayersMetricsTests : IDisposable
{
    private readonly TelemetryCollector _telemetry = new();

    [Fact]
    public void SignInSucceeded_CountsAndRecordsDurationWithSuccessOutcome()
    {
        _telemetry.Metrics.SignInSucceeded(newPlayer: true, TimeSpan.FromMilliseconds(250));

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(PlayersMetrics.SignInsInstrumentName, count.Instrument);
        Assert.Equal(PlayersMetrics.OutcomeSuccess, count.Tags["outcome"]);
        Assert.Equal(true, count.Tags["new_player"]);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(PlayersMetrics.SignInDurationInstrumentName, duration.Instrument);
        Assert.Equal(0.25, duration.Value, precision: 6);
        Assert.Equal(PlayersMetrics.OutcomeSuccess, Assert.Single(duration.Tags).Value);
    }

    [Theory]
    [InlineData(PlayersMetrics.OutcomeRejected)]
    [InlineData(PlayersMetrics.OutcomeUnavailable)]
    [InlineData(PlayersMetrics.OutcomeError)]
    public void SignInFailed_CountsAndRecordsDurationWithOutcome(string outcome)
    {
        _telemetry.Metrics.SignInFailed(outcome, TimeSpan.FromSeconds(2));

        var count = Assert.Single(_telemetry.Measurements);
        Assert.Equal(outcome, Assert.Single(count.Tags).Value);

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(2, duration.Value, precision: 6);
        Assert.Equal(outcome, Assert.Single(duration.Tags).Value);
    }

    [Theory]
    [InlineData(PlayersMetrics.ResolutionCacheHit)]
    [InlineData(PlayersMetrics.ResolutionCacheMiss)]
    [InlineData(PlayersMetrics.ResolutionUnknown)]
    [InlineData(PlayersMetrics.OutcomeError)]
    public void UserResolved_CountsWithOutcomeOnly(string outcome)
    {
        _telemetry.Metrics.UserResolved(outcome);

        var measurement = Assert.Single(_telemetry.Measurements);
        Assert.Equal(PlayersMetrics.UserResolutionsInstrumentName, measurement.Instrument);
        Assert.Equal(1, measurement.Value);
        var tag = Assert.Single(measurement.Tags);
        Assert.Equal("outcome", tag.Key);
        Assert.Equal(outcome, tag.Value);
    }

    public void Dispose() => _telemetry.Dispose();
}
