using Bogus;
using HexMaster.LeakOut.Scores.DomainModels;

namespace HexMaster.LeakOut.Scores.Tests.Factories;

/// <summary>Creates plausible stored best scores, with optional fixed parts.</summary>
public sealed class LevelScoreFaker : Faker<LevelScore>
{
    public LevelScoreFaker(Guid? userId = null, int? level = null, int? score = null)
    {
        CustomInstantiator(f => LevelScore.Create(
            userId ?? Guid.CreateVersion7(),
            level ?? f.Random.Int(1, 40),
            score ?? f.Random.Int(0, 10_000),
            new DateTimeOffset(f.Date.Past(1, new DateTime(2026, 10, 1, 0, 0, 0, DateTimeKind.Utc)), TimeSpan.Zero)));
    }
}
