using System.Text.Json;
using System.Text.Json.Serialization;
using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

/// <summary>
/// Runs every fixture level through the C# <see cref="LevelRules"/> and <see cref="LevelSolver"/> and
/// checks the verdict in <c>Fixtures/verdicts.json</c>. The app's core tests check the same file with
/// the Kotlin originals (<c>DailyPipeConformanceTest</c>), so the two implementations cannot drift apart.
/// </summary>
public sealed class LevelConformanceTests
{
    public static TheoryData<string> FixtureNames()
    {
        var data = new TheoryData<string>();
        foreach (var name in Fixtures.LevelFiles().Select(Path.GetFileName).Order(StringComparer.Ordinal))
        {
            data.Add(name!);
        }

        return data;
    }

    [Theory]
    [MemberData(nameof(FixtureNames))]
    public void Fixture_MatchesItsVerdict(string fileName)
    {
        var verdicts = Fixtures.ReadVerdicts();
        Assert.True(verdicts.TryGetValue(fileName, out var expected), $"{fileName} has no verdict in verdicts.json");

        Assert.Equal(expected, Fixtures.Judge(fileName));
    }

    [Fact]
    public void EveryVerdict_HasAFixture()
    {
        var files = Fixtures.LevelFiles().Select(Path.GetFileName).ToHashSet();

        Assert.All(Fixtures.ReadVerdicts().Keys, name => Assert.Contains(name, files));
    }

    [Fact]
    public void Fixtures_CoverEveryVerdictKind()
    {
        var verdicts = Fixtures.ReadVerdicts().Values.ToList();

        Assert.Contains(verdicts, v => !v.Valid);
        Assert.Contains(verdicts, v => v is { Valid: true, Solvable: false });
        Assert.Contains(verdicts, v => v is { Solvable: true, LeakFree: false });
        Assert.Contains(verdicts, v => v is { Solvable: true, LeakFree: true });
    }
}

/// <summary>The verdict on one fixture level, as stored in <c>verdicts.json</c>.</summary>
public sealed record LevelVerdict
{
    public bool Valid { get; init; }

    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public IReadOnlyList<string>? ErrorFields { get; init; }

    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public bool? Solvable { get; init; }

    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public bool? LeakFree { get; init; }

    /// <summary>The number of entries of the solver's path: the same search must find the same route.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? RouteLength { get; init; }

    public bool Equals(LevelVerdict? other) =>
        other is not null
        && Valid == other.Valid
        && (ErrorFields ?? []).SequenceEqual(other.ErrorFields ?? [])
        && Solvable == other.Solvable
        && LeakFree == other.LeakFree
        && RouteLength == other.RouteLength;

    public override int GetHashCode() => HashCode.Combine(Valid, Solvable, LeakFree, RouteLength);

    public override string ToString() => JsonSerializer.Serialize(this, Fixtures.JsonOptions);
}

/// <summary>The conformance fixtures shared with the app's core tests.</summary>
public static class Fixtures
{
    public static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true,
    };

    public static string Directory => Path.Combine(AppContext.BaseDirectory, "Fixtures");

    public static IEnumerable<string> LevelFiles() => System.IO.Directory.EnumerateFiles(Path.Combine(Directory, "levels"), "*.json");

    public static IReadOnlyDictionary<string, LevelVerdict> ReadVerdicts() =>
        JsonSerializer.Deserialize<Dictionary<string, LevelVerdict>>(File.ReadAllText(Path.Combine(Directory, "verdicts.json")), JsonOptions)!;

    /// <summary>What the C# rules and solver say about a fixture level.</summary>
    public static LevelVerdict Judge(string fileName)
    {
        var level = LevelJson.Parse(File.ReadAllText(Path.Combine(Directory, "levels", fileName)));
        var errors = LevelRules.Validate(level);
        if (errors.Count > 0)
        {
            return new LevelVerdict { Valid = false, ErrorFields = errors.Select(e => e.Field).ToList() };
        }

        return LevelSolver.Solve(level) switch
        {
            SolverResult.Solvable solvable => new LevelVerdict
            {
                Valid = true,
                Solvable = true,
                LeakFree = solvable.LeakFree,
                RouteLength = solvable.Path.Count,
            },
            _ => new LevelVerdict { Valid = true, Solvable = false },
        };
    }
}
