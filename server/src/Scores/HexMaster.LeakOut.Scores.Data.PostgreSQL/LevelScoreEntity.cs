namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

/// <summary>The stored best score of a player on a level.</summary>
public sealed record LevelScoreEntity
{
    public Guid Id { get; set; }

    public Guid UserId { get; set; }

    public int Level { get; set; }

    public int Score { get; set; }

    public DateTimeOffset AchievedAt { get; set; }
}
