using Bogus;
using HexMaster.LeakOut.Players.DomainModels;

namespace HexMaster.LeakOut.Players.Tests.Factories;

/// <summary>Creates plausible, already registered players.</summary>
public sealed class PlayerFaker : Faker<Player>
{
    public PlayerFaker(string? displayName = null)
    {
        CustomInstantiator(f =>
        {
            var createdAt = new DateTimeOffset(f.Date.Past(2, new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc)), TimeSpan.Zero);
            var lastSignedInAt = createdAt.AddDays(f.Random.Int(0, 300));
            return Player.Restore(Guid.CreateVersion7(createdAt), displayName ?? f.Internet.UserName(), createdAt, lastSignedInAt);
        });
    }
}
