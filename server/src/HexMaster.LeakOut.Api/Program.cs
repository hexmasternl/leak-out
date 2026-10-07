using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Api.Endpoints;
using HexMaster.LeakOut.Players;
using HexMaster.LeakOut.Players.Data.PostgreSQL;
using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Scores;
using HexMaster.LeakOut.Scores.Data.PostgreSQL;
using HexMaster.LeakOut.Scores.Observability;

var builder = WebApplication.CreateBuilder(args);

builder.AddServiceDefaults();
builder.Services.AddOpenTelemetry()
    .WithMetrics(metrics => metrics.AddMeter(PlayersMetrics.Name, ScoresMetrics.Name))
    .WithTracing(tracing => tracing.AddSource(PlayersMetrics.Name, ScoresMetrics.Name));

builder.Services.AddPlayersModule();
builder.AddPlayersPostgreSqlData();
builder.Services.AddScoresModule();
builder.AddScoresPostgreSqlData();
// Every environment: the API does not listen until both schemas are up to date, so a failed
// migration keeps a new revision from taking traffic.
builder.Services.AddHostedService<PlayersDatabaseMigrator>();
builder.Services.AddHostedService<ScoresDatabaseMigrator>();

builder.Services.AddAccessTokenAuthentication();

// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi(options =>
{
    options.AddDocumentTransformer<BearerSecurityTransformer>();
    options.AddOperationTransformer<BearerSecurityTransformer>();
});

var app = builder.Build();

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

app.MapDefaultEndpoints();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi().AllowAnonymous();
}

app.MapPlayerEndpoints();
app.MapScoreEndpoints();
app.MapLevelScoreEndpoints();

app.Run();

/// <summary>Exposed for <c>WebApplicationFactory&lt;Program&gt;</c> in the API tests.</summary>
public partial class Program;
