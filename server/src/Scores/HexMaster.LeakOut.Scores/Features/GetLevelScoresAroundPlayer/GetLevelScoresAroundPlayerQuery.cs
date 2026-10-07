namespace HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;

/// <summary>Reads the part of a level's ranking around the requesting player's best score.</summary>
public sealed record GetLevelScoresAroundPlayerQuery(Guid UserId, int Level);
