using System.Diagnostics;
using OpenTelemetry;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults;

/// <summary>
/// Stops recording spans whose in-process parent is not recorded. The ASP.NET Core filter keeps the
/// <c>/health</c> and <c>/alive</c> request spans out, but those requests stay the current activity, so
/// the database calls of the health checks would still be exported as their children. Not every
/// sampler follows the parent's decision (the Azure Monitor one does not), so this processor does.
/// </summary>
public sealed class DropChildrenOfUnrecordedSpansProcessor : BaseProcessor<Activity>
{
    public override void OnStart(Activity data)
    {
        if (data.Parent is { } parent && (!parent.Recorded || !parent.IsAllDataRequested))
        {
            // Export processors skip activities that are not recorded.
            data.ActivityTraceFlags &= ~ActivityTraceFlags.Recorded;
            data.IsAllDataRequested = false;
        }
    }
}
