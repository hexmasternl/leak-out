using System.Diagnostics;
using System.Diagnostics.Metrics;

namespace HexMaster.LeakOut.Scores.Observability;

/// <summary>
/// Telemetry of the Scores module. Metric tags stay low-cardinality: no player ids, levels or scores.
/// </summary>
public sealed class ScoresMetrics
{
    /// <summary>The name of the module's <see cref="System.Diagnostics.Metrics.Meter"/> and <see cref="ActivitySource"/>.</summary>
    public const string Name = "HexMaster.LeakOut.Scores";

    public const string SubmissionsInstrumentName = "leakout.scores.submissions";
    public const string SubmissionDurationInstrumentName = "leakout.scores.submission.duration";
    public const string LeaderboardRequestsInstrumentName = "leakout.scores.leaderboard.requests";
    public const string LeaderboardDurationInstrumentName = "leakout.scores.leaderboard.duration";

    public const string SubmitScoreActivityName = "scores.submit_score";
    public const string GetLevelTopScoresActivityName = "scores.get_level_top_scores";
    public const string GetLevelScoresAroundPlayerActivityName = "scores.get_level_scores_around_player";
    public const string GetPublicLevelTopScoresActivityName = "scores.get_public_level_top_scores";

    public const string OutcomeSuccess = "success";
    public const string OutcomeNewBest = "new_best";
    public const string OutcomeNotImproved = "not_improved";
    public const string OutcomeError = "error";

    public const string LeaderboardKindTop = "top";
    public const string LeaderboardKindAroundPlayer = "around_player";
    public const string LeaderboardKindPublicTop = "public_top";

    public static readonly ActivitySource ActivitySource = new(Name);

    // The default histogram buckets are meant for milliseconds; these fit durations in seconds.
    private static readonly double[] DurationBuckets = [0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10];

    private readonly Counter<long> _submissions;
    private readonly Histogram<double> _submissionDuration;
    private readonly Counter<long> _leaderboardRequests;
    private readonly Histogram<double> _leaderboardDuration;

    public ScoresMetrics(IMeterFactory meterFactory)
    {
        var meter = meterFactory.Create(Name);
        _submissions = meter.CreateCounter<long>(
            SubmissionsInstrumentName,
            unit: "{submission}",
            description: "Number of score submissions, by outcome.");
        _submissionDuration = meter.CreateHistogram(
            SubmissionDurationInstrumentName,
            unit: "s",
            description: "Duration of score submissions, by outcome.",
            tags: null,
            advice: new InstrumentAdvice<double> { HistogramBucketBoundaries = DurationBuckets });
        _leaderboardRequests = meter.CreateCounter<long>(
            LeaderboardRequestsInstrumentName,
            unit: "{request}",
            description: "Number of leaderboard requests, by leaderboard kind and outcome.");
        _leaderboardDuration = meter.CreateHistogram(
            LeaderboardDurationInstrumentName,
            unit: "s",
            description: "Duration of leaderboard requests, by leaderboard kind and outcome.",
            tags: null,
            advice: new InstrumentAdvice<double> { HistogramBucketBoundaries = DurationBuckets });
    }

    public void ScoreSubmitted(bool isNewBest, TimeSpan elapsed) =>
        RecordSubmission(isNewBest ? OutcomeNewBest : OutcomeNotImproved, elapsed);

    public void ScoreSubmissionFailed(TimeSpan elapsed) =>
        RecordSubmission(OutcomeError, elapsed);

    /// <summary>Records a served leaderboard of kind <see cref="LeaderboardKindTop"/>, <see cref="LeaderboardKindAroundPlayer"/> or <see cref="LeaderboardKindPublicTop"/>.</summary>
    public void LeaderboardServed(string kind, TimeSpan elapsed) =>
        RecordLeaderboard(kind, OutcomeSuccess, elapsed);

    public void LeaderboardFailed(string kind, TimeSpan elapsed) =>
        RecordLeaderboard(kind, OutcomeError, elapsed);

    private void RecordSubmission(string outcome, TimeSpan elapsed)
    {
        var tag = new KeyValuePair<string, object?>("outcome", outcome);
        _submissions.Add(1, tag);
        _submissionDuration.Record(elapsed.TotalSeconds, tag);
    }

    private void RecordLeaderboard(string kind, string outcome, TimeSpan elapsed)
    {
        var kindTag = new KeyValuePair<string, object?>("leaderboard.kind", kind);
        var outcomeTag = new KeyValuePair<string, object?>("outcome", outcome);
        _leaderboardRequests.Add(1, kindTag, outcomeTag);
        _leaderboardDuration.Record(elapsed.TotalSeconds, kindTag, outcomeTag);
    }
}
