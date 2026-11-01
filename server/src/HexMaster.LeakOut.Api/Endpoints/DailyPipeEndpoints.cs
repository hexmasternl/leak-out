using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.DomainModels;
using HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;
using HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;
using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.AspNetCore.Mvc;

namespace HexMaster.LeakOut.Api.Endpoints;

/// <summary>
/// The daily pipe: today's level, submitting a result and the day's top scores. Every endpoint is for
/// a signed-in player; the day is always the server's UTC date, never the device's.
/// </summary>
public static class DailyPipeEndpoints
{
    public static IEndpointRouteBuilder MapDailyPipeEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/daily-pipes")
            .WithTags("DailyPipes")
            .RequireAuthorization()
            .RequirePlayer();

        group.MapGet("/today", GetTodaysDailyPipe)
            .WithName("GetTodaysDailyPipe")
            .WithSummary("Returns today's daily pipe with the signed-in player's streak and best result; 404 until it is published.")
            .Produces(StatusCodes.Status401Unauthorized);

        group.MapPost("/{date}/scores", SubmitDailyScore)
            .WithName("SubmitDailyScore")
            .WithSummary("Submits the signed-in player's result on the current daily pipe; 409 when that day has ended.")
            .Produces(StatusCodes.Status401Unauthorized);

        group.MapGet("/{date}/scores/top", GetDailyTopScores)
            .WithName("GetDailyTopScores")
            .WithSummary("Returns the 10 best results on the daily pipe of a date and the signed-in player's rank.")
            .Produces(StatusCodes.Status401Unauthorized);

        return app;
    }

    private static async Task<Results<Ok<DailyPipeDto>, NotFound>> GetTodaysDailyPipe(
        HttpContext httpContext,
        IQueryHandler<GetTodaysDailyPipeQuery, DailyPipeDto?> handler,
        CancellationToken cancellationToken)
    {
        var pipe = await handler.Handle(new GetTodaysDailyPipeQuery(httpContext.GetPlayerId()), cancellationToken);
        return pipe is null ? TypedResults.NotFound() : TypedResults.Ok(pipe);
    }

    private static async Task<Results<Ok<DailyScoreResultDto>, ValidationProblem, Conflict<ProblemDetails>>> SubmitDailyScore(
        DateOnly date,
        SubmitDailyScoreRequest request,
        HttpContext httpContext,
        ICommandHandler<SubmitDailyScoreCommand, SubmitDailyScoreOutcome> handler,
        CancellationToken cancellationToken)
    {
        var errors = new Dictionary<string, string[]>();
        if (request.Score < 0)
        {
            errors["score"] = ["The score cannot be negative."];
        }

        if (request.Stars is < 0 or > DailyScore.MaxStars)
        {
            errors["stars"] = [$"Stars must be between 0 and {DailyScore.MaxStars}."];
        }

        if (errors.Count > 0)
        {
            return TypedResults.ValidationProblem(errors);
        }

        var command = new SubmitDailyScoreCommand(httpContext.GetPlayerId(), date, request.Score, request.Stars);
        return await handler.Handle(command, cancellationToken) switch
        {
            SubmitDailyScoreOutcome.Accepted accepted => TypedResults.Ok(accepted.Result),
            SubmitDailyScoreOutcome.ScoreTooHigh tooHigh => TypedResults.ValidationProblem(new Dictionary<string, string[]>
            {
                ["score"] = [$"The score cannot be above {tooHigh.MaxScore} on this daily pipe."],
            }),
            _ => TypedResults.Conflict(new ProblemDetails
            {
                Status = StatusCodes.Status409Conflict,
                Title = "This daily pipe has ended.",
                Detail = $"Only the daily pipe of the current UTC date accepts results, and {date:yyyy-MM-dd} is not it.",
            }),
        };
    }

    private static async Task<Results<Ok<DailyTopScoresDto>, NotFound>> GetDailyTopScores(
        DateOnly date,
        HttpContext httpContext,
        IQueryHandler<GetDailyTopScoresQuery, DailyTopScoresDto?> handler,
        CancellationToken cancellationToken)
    {
        var top = await handler.Handle(new GetDailyTopScoresQuery(httpContext.GetPlayerId(), date), cancellationToken);
        return top is null ? TypedResults.NotFound() : TypedResults.Ok(top);
    }
}
