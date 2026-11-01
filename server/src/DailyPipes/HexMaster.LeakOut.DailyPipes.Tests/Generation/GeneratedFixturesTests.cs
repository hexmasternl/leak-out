using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

/// <summary>
/// <c>Fixtures/generated</c> holds a daily pipe for every weekday, as the generator makes it for a
/// fixed seed. The app's core tests (<c>DailyPipeConformanceTest</c>) parse, validate and solve these
/// files, so a level the server publishes is one the app accepts. When the generator changes, this
/// test fails until the files are written again: run it with <c>LEAKOUT_WRITE_FIXTURES=1</c> and
/// then run the app's core tests.
/// </summary>
public sealed class GeneratedFixturesTests
{
    public static TheoryData<DateOnly> Dates()
    {
        var data = new TheoryData<DateOnly>();
        for (var day = 0; day < 7; day++)
        {
            data.Add(new DateOnly(2026, 11, 2).AddDays(day));
        }

        return data;
    }

    [Theory]
    [MemberData(nameof(Dates))]
    public void GeneratedFixture_IsWhatTheGeneratorMakes(DateOnly date)
    {
        var built = DailyLevelGeneratorTests.Build(date, seed: date.DayNumber).Built;
        Assert.NotNull(built);
        var fileName = $"daily_{date:yyyy-MM-dd}.json";

        if (Environment.GetEnvironmentVariable("LEAKOUT_WRITE_FIXTURES") == "1")
        {
            var sourceDir = Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "Fixtures", "generated");
            Directory.CreateDirectory(sourceDir);
            File.WriteAllText(Path.Combine(sourceDir, fileName), built.LevelJson + "\n");
            return;
        }

        var path = Path.Combine(Fixtures.Directory, "generated", fileName);
        Assert.True(File.Exists(path), $"{fileName} is missing; write the fixtures with LEAKOUT_WRITE_FIXTURES=1");
        Assert.Equal(built.LevelJson, File.ReadAllText(path).ReplaceLineEndings("\n").TrimEnd());
    }
}
