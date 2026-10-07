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
    public const string SubmitScoreActivityName = "scores.submit_score";

    public const string OutcomeNewBest = "new_best";
    public const string OutcomeNotImproved = "not_improved";
    public const string OutcomeError = "error";

    public static readonly ActivitySource ActivitySource = new(Name);

    private readonly Counter<long> _submissions;

    public ScoresMetrics(IMeterFactory meterFactory)
    {
        var meter = meterFactory.Create(Name);
        _submissions = meter.CreateCounter<long>(
            SubmissionsInstrumentName,
            unit: "{submission}",
            description: "Number of score submissions, by outcome.");
    }

    public void ScoreSubmitted(bool isNewBest) =>
        _submissions.Add(1, new KeyValuePair<string, object?>("outcome", isNewBest ? OutcomeNewBest : OutcomeNotImproved));

    public void ScoreSubmissionFailed() =>
        _submissions.Add(1, new KeyValuePair<string, object?>("outcome", OutcomeError));
}
