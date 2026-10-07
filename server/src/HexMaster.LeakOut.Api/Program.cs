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

// Add services to the container.
// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi();

var app = builder.Build();

app.MapDefaultEndpoints();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();

app.Run();
