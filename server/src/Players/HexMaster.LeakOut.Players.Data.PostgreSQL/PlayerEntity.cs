namespace HexMaster.LeakOut.Players.Data.PostgreSQL;

/// <summary>
/// The stored player. This record is the only place, besides the Players module's ports, where the
/// Google Play Games player id (<see cref="SubjectId"/>) is kept.
/// </summary>
public sealed record PlayerEntity
{
    public Guid Id { get; set; }

    public required string SubjectId { get; set; }

    public required string DisplayName { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset LastSignedInAt { get; set; }
}
