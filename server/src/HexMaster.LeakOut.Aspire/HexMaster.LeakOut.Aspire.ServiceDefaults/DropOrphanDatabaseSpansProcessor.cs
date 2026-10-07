using System.Diagnostics;
using OpenTelemetry;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults;

/// <summary>
/// Keeps database spans that belong to no operation out of the exported telemetry. The health
/// checks behind the <c>/health</c> readiness probe query the databases outside any request span,
/// so every probe would otherwise export root database traces. Database calls made by a request,
/// handler or job always have a parent span and are kept.
/// </summary>
public sealed class DropOrphanDatabaseSpansProcessor : BaseProcessor<Activity>
{
    /// <summary>The name of the <see cref="ActivitySource"/> Npgsql creates its spans with.</summary>
    public const string NpgsqlSourceName = "Npgsql";

    public override void OnStart(Activity data)
    {
        if (data.Source.Name == NpgsqlSourceName && data.ParentSpanId == default)
        {
            // Export processors skip activities that are not recorded.
            data.ActivityTraceFlags &= ~ActivityTraceFlags.Recorded;
            data.IsAllDataRequested = false;
        }
    }
}
