namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

/// <summary>The stored daily pipe of a UTC date.</summary>
public sealed record DailyPipeEntity
{
    public DateOnly Date { get; set; }

    public string LevelJson { get; set; } = string.Empty;

    public long StackSeed { get; set; }

    public int MaxScore { get; set; }

    public DateTimeOffset PublishedAt { get; set; }
}
