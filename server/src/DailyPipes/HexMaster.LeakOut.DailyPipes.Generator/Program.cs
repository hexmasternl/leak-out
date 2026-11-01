using HexMaster.LeakOut.DailyPipes;
using HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;
using HexMaster.LeakOut.DailyPipes.Generator;
using HexMaster.LeakOut.DailyPipes.Observability;

// Runs GenerateDailyPipe once and exits: 0 when today's pipe is published (or already was), 1 when
// the run failed. Scheduled as an Azure Container Apps job at 00:00 UTC; run once at startup by Aspire.
var builder = Host.CreateApplicationBuilder(args);

// The module also registers the API's handlers, which need the Players module; the job never
// resolves them, so the container is not validated as a whole.
builder.ConfigureContainer(new DefaultServiceProviderFactory(new ServiceProviderOptions { ValidateScopes = true }));

builder.AddServiceDefaults();
builder.Services.AddOpenTelemetry()
    .WithMetrics(metrics => metrics.AddMeter(DailyPipesMetrics.Name))
    .WithTracing(tracing => tracing.AddSource(DailyPipesMetrics.Name));

builder.Services.AddDailyPipesModule();
builder.AddDailyPipesPostgreSqlData();
// Migrates on start like the API, so the job works against a fresh database too.
builder.Services.AddHostedService<DailyPipesDatabaseMigrator>();

using var host = builder.Build();
await host.StartAsync();
var exitCode = await GeneratorRun.RunAsync(
    host.Services,
    host.Services.GetRequiredService<ILoggerFactory>().CreateLogger("HexMaster.LeakOut.DailyPipes.Generator"),
    CancellationToken.None);
// Stopping the host flushes the telemetry of the run before the process exits.
await host.StopAsync();
return exitCode;
