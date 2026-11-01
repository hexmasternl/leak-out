namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>The body of a daily pipe submission: the final score and the stars it earned.</summary>
public sealed record SubmitDailyScoreRequest(int Score, int Stars);
