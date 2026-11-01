var builder = DistributedApplication.CreateBuilder(args);

// Secrets come from the AppHost's user secrets (Parameters:<name>) locally, never from appsettings.json.
var googleClientId = builder.AddParameter("google-client-id");
var googleClientSecret = builder.AddParameter("google-client-secret", secret: true);
var signingKey = builder.AddParameter("authentication-signing-key", secret: true);

var postgres = builder.AddPostgres("postgres").WithDataVolume();
var playersDb = postgres.AddDatabase("playersdb");
var scoresDb = postgres.AddDatabase("scoresdb");
var dailyPipesDb = postgres.AddDatabase("dailypipesdb");

builder.AddProject<Projects.HexMaster_LeakOut_Api>("hexmaster-leakout-api")
    .WithReference(playersDb)
    .WaitFor(playersDb)
    .WithReference(scoresDb)
    .WaitFor(scoresDb)
    .WithReference(dailyPipesDb)
    .WaitFor(dailyPipesDb)
    .WithEnvironment("GooglePlayGames__ClientId", googleClientId)
    .WithEnvironment("GooglePlayGames__ClientSecret", googleClientSecret)
    .WithEnvironment("Authentication__SigningKey", signingKey);

// Runs once at startup and exits, so a developer always has today's daily pipe. In Azure the same
// project runs as a Container Apps job at 00:00 UTC (server/infrastructure/modules/daily-pipe-job.bicep).
builder.AddProject<Projects.HexMaster_LeakOut_DailyPipes_Generator>("daily-pipe-generator")
    .WithReference(dailyPipesDb)
    .WaitFor(dailyPipesDb);

builder.Build().Run();
