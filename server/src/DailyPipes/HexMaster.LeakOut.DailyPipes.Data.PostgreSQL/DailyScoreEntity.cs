namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

/// <summary>The stored best result of a player on the daily pipe of a date.</summary>
public sealed record DailyScoreEntity
{
    public Guid Id { get; set; }

    public DateOnly Date { get; set; }

    public Guid UserId { get; set; }

    public int Score { get; set; }

    public int Stars { get; set; }

    public DateTimeOffset AchievedAt { get; set; }
}
