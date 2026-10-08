using HexMaster.LeakOut.Api.Cors;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;
using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.Net.Http.Headers;

namespace HexMaster.LeakOut.Api.Endpoints;

/// <summary>
/// Read-only leaderboard views for anonymous readers such as the public website. They carry no
/// player ids and allow cross-origin requests from the website only.
/// </summary>
public static class PublicLevelScoreEndpoints
{
    /// <summary>How long browsers and proxies may reuse a response; the server keeps its own copy for 30 minutes.</summary>
    public const string CacheControl = "public, max-age=60";

    public static IEndpointRouteBuilder MapPublicLevelScoreEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/public/levels/{level:int}/scores")
            .WithTags("Public scores")
            .AllowAnonymous()
            .RequireCors(WebsiteCorsRegistration.PolicyName);

        group.MapGet("/top", GetPublicLevelTopScores)
            .WithName("GetPublicLevelTopScores")
            .WithSummary("Returns the 25 best scores of a level, highest first, without player ids. Refreshed at most every 30 minutes.")
            .Produces<PublicLevelLeaderboardDto>()
            .ProducesValidationProblem();

        return app;
    }

    private static async Task<Results<Ok<PublicLevelLeaderboardDto>, ValidationProblem>> GetPublicLevelTopScores(
        int level,
        HttpResponse response,
        IQueryHandler<GetPublicLevelTopScoresQuery, PublicLevelLeaderboardDto> handler,
        CancellationToken cancellationToken)
    {
        if (level < 1)
        {
            return TypedResults.ValidationProblem(new Dictionary<string, string[]>
            {
                ["level"] = ["The level must be at least 1."],
            });
        }

        var leaderboard = await handler.Handle(new GetPublicLevelTopScoresQuery(level), cancellationToken);
        response.Headers[HeaderNames.CacheControl] = CacheControl;
        return TypedResults.Ok(leaderboard);
    }
}
