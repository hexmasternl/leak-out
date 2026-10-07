namespace HexMaster.LeakOut.Scores.Features.GetLevelTopScores;

/// <summary>Reads the top scores of a level for the requesting player.</summary>
public sealed record GetLevelTopScoresQuery(Guid UserId, int Level);
