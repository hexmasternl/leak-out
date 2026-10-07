using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Players;
using HexMaster.LeakOut.Players.Data.PostgreSQL;
using HexMaster.LeakOut.Players.Observability;

var builder = WebApplication.CreateBuilder(args);

builder.AddServiceDefaults();
builder.Services.AddOpenTelemetry()
    .WithMetrics(metrics => metrics.AddMeter(PlayersMetrics.Name))
    .WithTracing(tracing => tracing.AddSource(PlayersMetrics.Name));

builder.Services.AddPlayersModule();
builder.AddPlayersPostgreSqlData();
if (builder.Environment.IsDevelopment())
{
    builder.Services.AddHostedService<PlayersDatabaseMigrator>();
}

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

app.Run();

/// <summary>Exposed for <c>WebApplicationFactory&lt;Program&gt;</c> in the API tests.</summary>
public partial class Program;
