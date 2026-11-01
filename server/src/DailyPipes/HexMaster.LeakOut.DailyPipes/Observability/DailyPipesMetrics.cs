using System.Diagnostics;
using System.Diagnostics.Metrics;

namespace HexMaster.LeakOut.DailyPipes.Observability;

/// <summary>
/// Telemetry of the DailyPipes module. Metric tags stay low-cardinality: no player ids, dates or scores.
/// </summary>
public sealed class DailyPipesMetrics
{
    /// <summary>The name of the module's <see cref="Meter"/> and <see cref="ActivitySource"/>.</summary>
    public const string Name = "HexMaster.LeakOut.DailyPipes";

    public const string GenerationRunsInstrumentName = "leakout.daily_pipes.generation.runs";
    public const string GenerationAttemptsInstrumentName = "leakout.daily_pipes.generation.attempts";
    public const string GenerationDurationInstrumentName = "leakout.daily_pipes.generation.duration";
    public const string DownloadsInstrumentName = "leakout.daily_pipes.downloads";
    public const string SubmissionsInstrumentName = "leakout.daily_pipes.submissions";
    public const string SubmissionDurationInstrumentName = "leakout.daily_pipes.submission.duration";
    public const string LeaderboardRequestsInstrumentName = "leakout.daily_pipes.leaderboard.requests";

    public const string GenerateDailyPipeActivityName = "daily_pipes.generate";
    public const string GetTodaysDailyPipeActivityName = "daily_pipes.get_today";
    public const string SubmitDailyScoreActivityName = "daily_pipes.submit_score";
    public const string GetDailyTopScoresActivityName = "daily_pipes.get_top_scores";

    public const string OutcomePublished = "published";
    public const string OutcomeAlreadyPublished = "already_published";
    public const string OutcomeFailed = "failed";
    public const string OutcomeServed = "served";
    public const string OutcomeNotFound = "not_found";
    public const string OutcomeNewBest = "new_best";
    public const string OutcomeNotImproved = "not_improved";
    public const string OutcomeEnded = "ended";
    public const string OutcomeRejected = "rejected";
    public const string OutcomeError = "error";

    public static readonly ActivitySource ActivitySource = new(Name);

    // The default histogram buckets are meant for milliseconds; these fit durations in seconds.
    private static readonly double[] DurationBuckets = [0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10, 30, 60];
    private static readonly int[] AttemptBuckets = [1, 2, 3, 5, 10, 20, 50, 100, 200];

    private readonly Counter<long> _generationRuns;
    private readonly Histogram<int> _generationAttempts;
    private readonly Histogram<double> _generationDuration;
    private readonly Counter<long> _downloads;
    private readonly Counter<long> _submissions;
    private readonly Histogram<double> _submissionDuration;
    private readonly Counter<long> _leaderboardRequests;

    public DailyPipesMetrics(IMeterFactory meterFactory)
    {
        ArgumentNullException.ThrowIfNull(meterFactory);

        var meter = meterFactory.Create(Name);
        _generationRuns = meter.CreateCounter<long>(
            GenerationRunsInstrumentName, unit: "{run}", description: "Number of daily pipe generation runs, by outcome.");
        _generationAttempts = meter.CreateHistogram(
            GenerationAttemptsInstrumentName,
            unit: "{attempt}",
            description: "Candidate levels generated per run, by outcome.",
            tags: null,
            advice: new InstrumentAdvice<int> { HistogramBucketBoundaries = AttemptBuckets });
        _generationDuration = meter.CreateHistogram(
            GenerationDurationInstrumentName,
            unit: "s",
            description: "Duration of daily pipe generation runs, by outcome.",
            tags: null,
            advice: new InstrumentAdvice<double> { HistogramBucketBoundaries = DurationBuckets });
        _downloads = meter.CreateCounter<long>(
            DownloadsInstrumentName, unit: "{request}", description: "Number of requests for today's daily pipe, by outcome.");
        _submissions = meter.CreateCounter<long>(
            SubmissionsInstrumentName, unit: "{submission}", description: "Number of daily pipe score submissions, by outcome.");
        _submissionDuration = meter.CreateHistogram(
            SubmissionDurationInstrumentName,
            unit: "s",
            description: "Duration of daily pipe score submissions, by outcome.",
            tags: null,
            advice: new InstrumentAdvice<double> { HistogramBucketBoundaries = DurationBuckets });
        _leaderboardRequests = meter.CreateCounter<long>(
            LeaderboardRequestsInstrumentName, unit: "{request}", description: "Number of daily top score requests, by outcome.");
    }

    /// <summary>Records a generation run with outcome <see cref="OutcomePublished"/>, <see cref="OutcomeAlreadyPublished"/> or <see cref="OutcomeFailed"/>.</summary>
    public void GenerationRun(string outcome, int attempts, TimeSpan elapsed)
    {
        var tag = Outcome(outcome);
        _generationRuns.Add(1, tag);
        _generationAttempts.Record(attempts, tag);
        _generationDuration.Record(elapsed.TotalSeconds, tag);
    }

    /// <summary>Records a request for today's pipe with outcome <see cref="OutcomeServed"/>, <see cref="OutcomeNotFound"/> or <see cref="OutcomeError"/>.</summary>
    public void Download(string outcome) => _downloads.Add(1, Outcome(outcome));

    /// <summary>
    /// Records a submission with outcome <see cref="OutcomeNewBest"/>, <see cref="OutcomeNotImproved"/>,
    /// <see cref="OutcomeEnded"/>, <see cref="OutcomeRejected"/> or <see cref="OutcomeError"/>.
    /// </summary>
    public void Submission(string outcome, TimeSpan elapsed)
    {
        var tag = Outcome(outcome);
        _submissions.Add(1, tag);
        _submissionDuration.Record(elapsed.TotalSeconds, tag);
    }

    /// <summary>Records a top scores request with outcome <see cref="OutcomeServed"/>, <see cref="OutcomeNotFound"/> or <see cref="OutcomeError"/>.</summary>
    public void LeaderboardRequest(string outcome) => _leaderboardRequests.Add(1, Outcome(outcome));

    private static KeyValuePair<string, object?> Outcome(string outcome) => new("outcome", outcome);
}
