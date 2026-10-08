using System.Diagnostics;
using System.Diagnostics.Metrics;

namespace HexMaster.LeakOut.Players.Observability;

/// <summary>
/// Telemetry of the Players module. Tags stay low-cardinality and never carry the Google Play
/// Games player id or tokens.
/// </summary>
public sealed class PlayersMetrics
{
    /// <summary>The name of the module's <see cref="System.Diagnostics.Metrics.Meter"/> and <see cref="ActivitySource"/>.</summary>
    public const string Name = "HexMaster.LeakOut.Players";

    public const string SignInsInstrumentName = "leakout.players.sign_ins";
    public const string SignInDurationInstrumentName = "leakout.players.sign_in.duration";
    public const string UserResolutionsInstrumentName = "leakout.players.user_resolutions";

    public const string SignInActivityName = "players.sign_in";
    public const string ResolveUserActivityName = "players.resolve_user";
    public const string GetDisplayNamesActivityName = "players.get_display_names";

    public const string OutcomeSuccess = "success";
    public const string OutcomeRejected = "rejected";
    public const string OutcomeUnavailable = "unavailable";
    public const string OutcomeError = "error";

    public const string ResolutionCacheHit = "cache_hit";
    public const string ResolutionCacheMiss = "cache_miss";
    public const string ResolutionUnknown = "unknown";

    public static readonly ActivitySource ActivitySource = new(Name);

    // The default histogram buckets are meant for milliseconds; these fit durations in seconds.
    private static readonly double[] DurationBuckets = [0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10];

    private readonly Counter<long> _signIns;
    private readonly Histogram<double> _signInDuration;
    private readonly Counter<long> _userResolutions;

    public PlayersMetrics(IMeterFactory meterFactory)
    {
        var meter = meterFactory.Create(Name);
        _signIns = meter.CreateCounter<long>(
            SignInsInstrumentName,
            unit: "{sign_in}",
            description: "Number of player sign-in attempts, by outcome.");
        _signInDuration = meter.CreateHistogram(
            SignInDurationInstrumentName,
            unit: "s",
            description: "Duration of player sign-ins, by outcome.",
            tags: null,
            advice: new InstrumentAdvice<double> { HistogramBucketBoundaries = DurationBuckets });
        _userResolutions = meter.CreateCounter<long>(
            UserResolutionsInstrumentName,
            unit: "{resolution}",
            description: "Number of access token subjects resolved to a player, by outcome.");
    }

    public void SignInSucceeded(bool newPlayer, TimeSpan elapsed)
    {
        _signIns.Add(1, new KeyValuePair<string, object?>("outcome", OutcomeSuccess), new KeyValuePair<string, object?>("new_player", newPlayer));
        _signInDuration.Record(elapsed.TotalSeconds, new KeyValuePair<string, object?>("outcome", OutcomeSuccess));
    }

    public void SignInFailed(string outcome, TimeSpan elapsed)
    {
        _signIns.Add(1, new KeyValuePair<string, object?>("outcome", outcome));
        _signInDuration.Record(elapsed.TotalSeconds, new KeyValuePair<string, object?>("outcome", outcome));
    }

    /// <summary>
    /// Counts a user resolution by outcome: <see cref="ResolutionCacheHit"/>, <see cref="ResolutionCacheMiss"/>,
    /// <see cref="ResolutionUnknown"/> or <see cref="OutcomeError"/>.
    /// </summary>
    public void UserResolved(string outcome) =>
        _userResolutions.Add(1, new KeyValuePair<string, object?>("outcome", outcome));
}
