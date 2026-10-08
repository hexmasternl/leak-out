namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>The player's stored best score on the level after a submission, and whether the submission became it.</summary>
public sealed record SubmitScoreResult(int Level, int BestScore, bool IsNewBest);
