namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

/// <summary>The stored daily pipe streak of a player.</summary>
public sealed record DailyStreakEntity
{
    public Guid UserId { get; set; }

    public int CurrentStreak { get; set; }

    public int BestStreak { get; set; }

    public DateOnly? LastWinDate { get; set; }
}
