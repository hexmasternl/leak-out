using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Services;
using Microsoft.AspNetCore.Http.HttpResults;

namespace HexMaster.LeakOut.Api.Endpoints;

public static class PlayerEndpoints
{
    public static IEndpointRouteBuilder MapPlayerEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/players").WithTags("Players");

        group.MapPost("/sessions", CreateSession)
            .AllowAnonymous()
            .WithName("CreateSession")
            .WithSummary("Signs a player in with a Google Play Games server auth code.")
            .ProducesProblem(StatusCodes.Status401Unauthorized)
            .ProducesProblem(StatusCodes.Status503ServiceUnavailable);

        return app;
    }

    private static async Task<Results<Ok<SessionDto>, ValidationProblem, ProblemHttpResult>> CreateSession(
        CreateSessionRequest request,
        ICommandHandler<SignInPlayerCommand, SessionDto> handler,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.ServerAuthCode))
        {
            return TypedResults.ValidationProblem(new Dictionary<string, string[]>
            {
                ["serverAuthCode"] = ["A server auth code is required."],
            });
        }

        try
        {
            return TypedResults.Ok(await handler.Handle(new SignInPlayerCommand(request.ServerAuthCode), cancellationToken));
        }
        catch (GooglePlayGamesAuthenticationException)
        {
            return TypedResults.Problem(
                title: "The server auth code was rejected.",
                statusCode: StatusCodes.Status401Unauthorized);
        }
        catch (GooglePlayGamesUnavailableException)
        {
            return TypedResults.Problem(
                title: "Google Play Games is unavailable. Try again later.",
                statusCode: StatusCodes.Status503ServiceUnavailable);
        }
    }
}
