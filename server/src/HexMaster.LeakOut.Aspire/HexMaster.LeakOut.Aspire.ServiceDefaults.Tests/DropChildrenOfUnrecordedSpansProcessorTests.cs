using System.Diagnostics;
using System.Net;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.Extensions.Hosting;
using OpenTelemetry.Trace;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults.Tests;

// One class, so no other test starts activities of these sources while these run.
public sealed class DropChildrenOfUnrecordedSpansProcessorTests
{
    private const string RequestSourceName = "HexMaster.LeakOut.Tests.Request";
    private const string DatabaseSourceName = "HexMaster.LeakOut.Tests.Database";

    private static readonly ActivitySource Request = new(RequestSourceName);
    private static readonly ActivitySource Database = new(DatabaseSourceName);

    [Fact]
    public void OnStart_ChildOfUnrecordedParent_IsNoLongerRecorded()
    {
        using var listener = ListenToAll();
        var processor = new DropChildrenOfUnrecordedSpansProcessor();

        using var probe = Request.StartActivity("GET /health")!;
        probe.ActivityTraceFlags &= ~ActivityTraceFlags.Recorded;
        using var query = Database.StartActivity("SELECT 1")!;
        processor.OnStart(query);
        using var nested = Database.StartActivity("nested")!;
        processor.OnStart(nested);

        Assert.False(query.Recorded);
        Assert.False(query.IsAllDataRequested);
        Assert.False(nested.Recorded);
    }

    [Fact]
    public void OnStart_ChildOfParentWithoutAllData_IsNoLongerRecorded()
    {
        using var listener = ListenToAll();

        using var probe = Request.StartActivity("GET /alive")!;
        probe.IsAllDataRequested = false;
        using var query = Database.StartActivity("SELECT 1")!;
        new DropChildrenOfUnrecordedSpansProcessor().OnStart(query);

        Assert.False(query.Recorded);
    }

    [Fact]
    public void OnStart_ChildOfRecordedParent_StaysRecorded()
    {
        using var listener = ListenToAll();

        using var request = Request.StartActivity("GET /api/levels/{level}/scores/top")!;
        using var query = Database.StartActivity("SELECT ranked")!;
        new DropChildrenOfUnrecordedSpansProcessor().OnStart(query);

        Assert.True(query.Recorded);
        Assert.True(query.IsAllDataRequested);
    }

    [Fact]
    public void OnStart_RootSpan_StaysRecorded()
    {
        using var listener = ListenToAll();

        using var migration = Database.StartActivity("migration")!;
        new DropChildrenOfUnrecordedSpansProcessor().OnStart(migration);

        Assert.True(migration.Recorded);
    }

    [Fact]
    public void OnStart_SpanWithRemoteParentOnly_StaysRecorded()
    {
        using var listener = ListenToAll();
        var remoteParent = new ActivityContext(ActivityTraceId.CreateRandom(), ActivitySpanId.CreateRandom(), ActivityTraceFlags.None, isRemote: true);

        using var span = Database.StartActivity("remote", ActivityKind.Client, remoteParent)!;
        new DropChildrenOfUnrecordedSpansProcessor().OnStart(span);

        // Only the in-process parent is judged; a remote caller's sampling decision is the sampler's job.
        Assert.True(span.Recorded);
    }

    [Fact]
    public async Task ServiceDefaults_ExportNeitherProbeRequestsNorTheirDatabaseCalls()
    {
        var exported = new List<Activity>();
        var builder = WebApplication.CreateEmptyBuilder(new WebApplicationOptions());
        builder.WebHost.UseTestServer();
        builder.Services.AddRouting();
        builder.AddServiceDefaults();
        builder.Services.AddHealthChecks().AddCheck("database", () =>
        {
            // Stands in for the Npgsql health check's query.
            Database.StartActivity("health-check-query")?.Dispose();
            return HealthCheckResult.Healthy();
        });
        builder.Services.AddOpenTelemetry().WithTracing(tracing => tracing
            .AddSource(DatabaseSourceName)
            // Test only: the Aspire AppHost runs the API with OTEL_TRACES_SAMPLER=always_on, a sampler
            // that ignores the parent's decision. Without it the default parent-based sampler would
            // hide the bug this test guards against.
            .SetSampler(new AlwaysOnSampler())
            .AddInMemoryExporter(exported));

        await using (var app = builder.Build())
        {
            app.MapDefaultEndpoints();
            app.MapGet("/api/scores", () =>
            {
                Database.StartActivity("submit-query")?.Dispose();
                return "ok";
            });
            await app.StartAsync(TestContext.Current.CancellationToken);
            var client = app.GetTestClient();

            Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/health", TestContext.Current.CancellationToken)).StatusCode);
            Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/alive", TestContext.Current.CancellationToken)).StatusCode);
            Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/api/scores", TestContext.Current.CancellationToken)).StatusCode);

            app.Services.GetRequiredService<TracerProvider>().ForceFlush();
            await app.StopAsync(TestContext.Current.CancellationToken);
        }

        var names = exported.Select(a => a.DisplayName).ToList();
        Assert.Contains("submit-query", names);
        Assert.Contains(exported, a => a.Kind == ActivityKind.Server && a.GetTagItem("url.path") as string == "/api/scores");
        Assert.DoesNotContain("health-check-query", names);
        Assert.DoesNotContain(exported, a => a.GetTagItem("url.path") is "/health" or "/alive");
    }

    private static ActivityListener ListenToAll()
    {
        var listener = new ActivityListener
        {
            // By name: comparing with the static fields could initialise them while the listener is being added.
            ShouldListenTo = source => source.Name is RequestSourceName or DatabaseSourceName,
            Sample = (ref _) => ActivitySamplingResult.AllDataAndRecorded,
        };
        ActivitySource.AddActivityListener(listener);
        return listener;
    }
}
