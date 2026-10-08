namespace HexMaster.LeakOut.Scores.Features.SubmitScore;

/// <summary>Submits a score the player reached on a level.</summary>
public sealed record SubmitScoreCommand(Guid UserId, int Level, int Score);
