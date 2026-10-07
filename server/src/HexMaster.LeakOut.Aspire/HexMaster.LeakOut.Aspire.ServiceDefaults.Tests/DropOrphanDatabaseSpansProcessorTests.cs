using System.Diagnostics;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using OpenTelemetry.Trace;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults.Tests;

// One class, so no other test starts Npgsql activities while these run.
public sealed class DropOrphanDatabaseSpansProcessorTests
{
    private const string OperationSourceName = "HexMaster.LeakOut.Tests.Operation";

    private static readonly ActivitySource Npgsql = new(DropOrphanDatabaseSpansProcessor.NpgsqlSourceName);
    private static readonly ActivitySource Operation = new(OperationSourceName);

    [Fact]
    public void OnStart_RootNpgsqlSpan_IsNoLongerRecorded()
    {
        using var listener = ListenToAll();

        using var span = Npgsql.StartActivity("health-check-query")!;
        new DropOrphanDatabaseSpansProcessor().OnStart(span);

        Assert.False(span.Recorded);
        Assert.False(span.IsAllDataRequested);
    }

    [Fact]
    public void OnStart_NpgsqlSpanWithinAnOperation_StaysRecorded()
    {
        using var listener = ListenToAll();

        using var parent = Operation.StartActivity("scores.get_level_top_scores")!;
        using var span = Npgsql.StartActivity("leaderboard-query")!;
        new DropOrphanDatabaseSpansProcessor().OnStart(span);

        Assert.True(span.Recorded);
    }

    [Fact]
    public void OnStart_NpgsqlSpanWithRemoteParent_StaysRecorded()
    {
        using var listener = ListenToAll();
        var remoteParent = new ActivityContext(ActivityTraceId.CreateRandom(), ActivitySpanId.CreateRandom(), ActivityTraceFlags.Recorded, isRemote: true);

        using var span = Npgsql.StartActivity("remote-query", ActivityKind.Client, remoteParent)!;
        new DropOrphanDatabaseSpansProcessor().OnStart(span);

        Assert.True(span.Recorded);
    }

    [Fact]
    public void OnStart_RootSpanOfAnotherSource_StaysRecorded()
    {
        using var listener = ListenToAll();

        using var span = Operation.StartActivity("background-job")!;
        new DropOrphanDatabaseSpansProcessor().OnStart(span);

        Assert.True(span.Recorded);
    }

    [Fact]
    public void ConfigureOpenTelemetry_ExportsDatabaseSpansOfOperationsButNotOrphans()
    {
        var exported = new List<Activity>();
        var builder = Host.CreateEmptyApplicationBuilder(new HostApplicationBuilderSettings());
        builder.ConfigureOpenTelemetry();
        builder.Services.AddOpenTelemetry().WithTracing(tracing => tracing
            .AddSource(DropOrphanDatabaseSpansProcessor.NpgsqlSourceName, OperationSourceName)
            .AddInMemoryExporter(exported));

        using (var host = builder.Build())
        {
            var tracerProvider = host.Services.GetRequiredService<TracerProvider>();

            Npgsql.StartActivity("orphan-health-check-query")?.Dispose();
            using (Operation.StartActivity("operation"))
            {
                Npgsql.StartActivity("operation-query")?.Dispose();
            }

            tracerProvider.ForceFlush();
        }

        var names = exported.Select(a => a.OperationName).ToList();
        Assert.Contains("operation-query", names);
        Assert.Contains("operation", names);
        Assert.DoesNotContain("orphan-health-check-query", names);
    }

    private static ActivityListener ListenToAll()
    {
        var listener = new ActivityListener
        {
            // By name: comparing with the static fields could initialise them while the listener is being added.
            ShouldListenTo = source => source.Name is DropOrphanDatabaseSpansProcessor.NpgsqlSourceName or OperationSourceName,
            Sample = (ref _) => ActivitySamplingResult.AllDataAndRecorded,
        };
        ActivitySource.AddActivityListener(listener);
        return listener;
    }
}
