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
    public const string SignInActivityName = "players.sign_in";

    public const string OutcomeSuccess = "success";
    public const string OutcomeRejected = "rejected";
    public const string OutcomeUnavailable = "unavailable";
    public const string OutcomeError = "error";

    public static readonly ActivitySource ActivitySource = new(Name);

    private readonly Counter<long> _signIns;

    public PlayersMetrics(IMeterFactory meterFactory)
    {
        var meter = meterFactory.Create(Name);
        _signIns = meter.CreateCounter<long>(
            SignInsInstrumentName,
            unit: "{sign_in}",
            description: "Number of player sign-in attempts, by outcome.");
    }

    public void SignInSucceeded(bool newPlayer) =>
        _signIns.Add(1, new KeyValuePair<string, object?>("outcome", OutcomeSuccess), new KeyValuePair<string, object?>("new_player", newPlayer));

    public void SignInFailed(string outcome) =>
        _signIns.Add(1, new KeyValuePair<string, object?>("outcome", outcome));
}
