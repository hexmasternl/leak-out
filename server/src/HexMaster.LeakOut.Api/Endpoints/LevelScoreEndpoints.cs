using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using Microsoft.AspNetCore.Http.HttpResults;

namespace HexMaster.LeakOut.Api.Endpoints;

/// <summary>The leaderboard views of a level, built on the level's ranking.</summary>
public static class LevelScoreEndpoints
{
    public static IEndpointRouteBuilder MapLevelScoreEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/levels/{level:int}/scores")
            .WithTags("Scores")
            .RequireAuthorization()
            .RequirePlayer();

        group.MapGet("/top", GetLevelTopScores)
            .WithName("GetLevelTopScores")
            .WithSummary("Returns the 10 best scores of a level, highest first, with the signed-in player's entry marked.")
            .Produces<LevelLeaderboardDto>()
            .Produces(StatusCodes.Status401Unauthorized);

        return app;
    }

    private static async Task<Results<Ok<LevelLeaderboardDto>, ValidationProblem>> GetLevelTopScores(
        int level,
        HttpContext httpContext,
        IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto> handler,
        CancellationToken cancellationToken)
    {
        if (level < 1)
        {
            return TypedResults.ValidationProblem(new Dictionary<string, string[]>
            {
                ["level"] = ["The level must be at least 1."],
            });
        }

        var query = new GetLevelTopScoresQuery(httpContext.GetPlayerId(), level);
        return TypedResults.Ok(await handler.Handle(query, cancellationToken));
    }
}
