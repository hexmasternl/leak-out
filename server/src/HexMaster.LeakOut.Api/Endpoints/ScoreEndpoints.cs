using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.SubmitScore;
using Microsoft.AspNetCore.Http.HttpResults;

namespace HexMaster.LeakOut.Api.Endpoints;

public static class ScoreEndpoints
{
    public static IEndpointRouteBuilder MapScoreEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/scores")
            .WithTags("Scores")
            .RequireAuthorization()
            .RequirePlayer();

        group.MapPost("/", SubmitScore)
            .WithName("SubmitScore")
            .WithSummary("Submits the signed-in player's score on a level; only a higher score replaces the stored best.")
            .Produces(StatusCodes.Status401Unauthorized);

        return app;
    }

    private static async Task<Results<Ok<SubmitScoreResult>, ValidationProblem>> SubmitScore(
        SubmitScoreRequest request,
        HttpContext httpContext,
        ICommandHandler<SubmitScoreCommand, SubmitScoreResult> handler,
        CancellationToken cancellationToken)
    {
        var errors = new Dictionary<string, string[]>();
        if (request.Level < 1)
        {
            errors["level"] = ["The level must be at least 1."];
        }

        if (request.Score < 0)
        {
            errors["score"] = ["The score cannot be negative."];
        }

        if (errors.Count > 0)
        {
            return TypedResults.ValidationProblem(errors);
        }

        var command = new SubmitScoreCommand(httpContext.GetPlayerId(), request.Level, request.Score);
        return TypedResults.Ok(await handler.Handle(command, cancellationToken));
    }
}
