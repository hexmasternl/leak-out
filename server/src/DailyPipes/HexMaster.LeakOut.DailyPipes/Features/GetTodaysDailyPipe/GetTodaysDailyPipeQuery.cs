namespace HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;

/// <summary>Reads today's daily pipe for the requesting player; the day is the server's UTC date.</summary>
public sealed record GetTodaysDailyPipeQuery(Guid UserId);
