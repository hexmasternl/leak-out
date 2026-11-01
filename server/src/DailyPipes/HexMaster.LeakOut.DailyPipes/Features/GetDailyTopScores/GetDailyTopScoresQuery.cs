namespace HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;

/// <summary>Reads the top scores of the daily pipe of <paramref name="Date"/> for the requesting player.</summary>
public sealed record GetDailyTopScoresQuery(Guid UserId, DateOnly Date);
