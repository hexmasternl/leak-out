namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>
/// The player's stored best result on the daily pipe after a submission, whether the submission
/// became it, the player's rank on that pipe, and the streak after the submission.
/// </summary>
public sealed record DailyScoreResultDto(DateOnly Date, DailyBestScoreDto BestScore, bool IsNewBest, int Rank, DailyStreakDto Streak);
