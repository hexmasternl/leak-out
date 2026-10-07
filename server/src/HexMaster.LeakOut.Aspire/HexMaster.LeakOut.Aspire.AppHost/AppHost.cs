var builder = DistributedApplication.CreateBuilder(args);

// Secrets come from the AppHost's user secrets (Parameters:<name>) locally, never from appsettings.json.
var googleClientId = builder.AddParameter("google-client-id");
var googleClientSecret = builder.AddParameter("google-client-secret", secret: true);
var signingKey = builder.AddParameter("authentication-signing-key", secret: true);

var postgres = builder.AddPostgres("postgres").WithDataVolume();
var playersDb = postgres.AddDatabase("playersdb");
var scoresDb = postgres.AddDatabase("scoresdb");

builder.AddProject<Projects.HexMaster_LeakOut_Api>("hexmaster-leakout-api")
    .WithReference(playersDb)
    .WaitFor(playersDb)
    .WithReference(scoresDb)
    .WaitFor(scoresDb)
    .WithEnvironment("GooglePlayGames__ClientId", googleClientId)
    .WithEnvironment("GooglePlayGames__ClientSecret", googleClientSecret)
    .WithEnvironment("Authentication__SigningKey", signingKey);

builder.Build().Run();
