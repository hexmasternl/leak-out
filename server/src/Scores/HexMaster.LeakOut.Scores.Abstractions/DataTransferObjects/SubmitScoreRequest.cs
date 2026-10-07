namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>The body of a score submission: the level played and the score reached on it.</summary>
public sealed record SubmitScoreRequest(int Level, int Score);
