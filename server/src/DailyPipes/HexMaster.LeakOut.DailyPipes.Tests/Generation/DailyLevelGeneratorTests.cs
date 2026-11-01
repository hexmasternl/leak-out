using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

public sealed class DailyLevelGeneratorTests
{
    private static readonly DateOnly FirstDay = new(2026, 11, 2);

    [Fact]
    public void AYearOfSeeds_AlwaysYieldsAValidSolvableLevel()
    {
        var attempts = new List<int>();
        for (var day = 0; day < 365; day++)
        {
            var date = FirstDay.AddDays(day);
            var (built, used) = Build(date, seed: date.DayNumber);

            Assert.True(built is not null, $"no daily pipe for {date} within {GenerateDailyPipeCommandHandler.MaxAttempts} attempts");
            Assert.Empty(LevelRules.Validate(built.Level));
            Assert.IsType<SolverResult.Solvable>(LevelSolver.Solve(built.Level));
            attempts.Add(used);
        }

        // Far below the limit, so a run practically never fails.
        Assert.True(attempts.Max() < GenerateDailyPipeCommandHandler.MaxAttempts / 4, $"most attempts needed: {attempts.Max()}");
    }

    [Fact]
    public void Generate_StaysWithinThePhoneSizedLimitsAndOnDifferentEdges()
    {
        for (var day = 0; day < 70; day++)
        {
            var date = FirstDay.AddDays(day);
            var level = new DailyLevelGenerator(SeedsFrom(day)).Generate(date).Level;

            Assert.InRange(level.Grid.Columns, 7, 14);
            Assert.InRange(level.Grid.Rows, 5, 10);
            Assert.True(level.Blocked.Count <= level.Grid.Columns * level.Grid.Rows * 0.15, $"{level.Blocked.Count} blocked on {level.Grid}");
            Assert.NotEqual(Edge(level.Start, level.Grid), Edge(level.Finish, level.Grid));
            Assert.True(level.Portals.Count <= 1);
            Assert.Empty(level.Leaky);
        }
    }

    [Fact]
    public void Generate_NamesTheLevelAfterItsDate()
    {
        var candidate = new DailyLevelGenerator(SeedsFrom(1)).Generate(new DateOnly(2026, 11, 2));

        Assert.Equal(20261102, candidate.Level.Id);
        Assert.Equal("Daily Pipe 2026-11-02", candidate.Level.Name);
        Assert.InRange(candidate.StackSeed, 0, DailyLevelGenerator.MaxStackSeed - 1);
    }

    [Fact]
    public void Generate_SameSeed_SameCandidate()
    {
        var a = new DailyLevelGenerator(() => 42).Generate(FirstDay);
        var b = new DailyLevelGenerator(() => 42).Generate(FirstDay);

        Assert.Equal(LevelJson.Write(a.Level), LevelJson.Write(b.Level));
        Assert.Equal(a.StackSeed, b.StackSeed);
    }

    [Fact]
    public void TwoDays_GetDifferentPipes()
    {
        var first = Build(FirstDay, seed: 1).Built!;
        var second = Build(FirstDay.AddDays(1), seed: 2).Built!;

        Assert.NotEqual(first.LevelJson, second.LevelJson);
        Assert.NotEqual(first.StackSeed, second.StackSeed);
    }

    [Fact]
    public void DefaultGenerator_ProducesCandidates()
    {
        var candidate = new DailyLevelGenerator().Generate(FirstDay);

        Assert.NotNull(candidate.Level);
    }

    [Fact]
    public void Presets_GetHarderOverTheWeek()
    {
        var monday = DifficultyPreset.For(new DateOnly(2026, 11, 2));
        var sunday = DifficultyPreset.For(new DateOnly(2026, 11, 8));

        Assert.Equal(DifficultyPreset.ForWeekday(0), monday);
        Assert.Equal(DifficultyPreset.ForWeekday(6), sunday);
        Assert.True(sunday.CellsPerSecond > monday.CellsPerSecond);
        Assert.True(sunday.BaseDelayMs < monday.BaseDelayMs);
        Assert.True(sunday.MaxMandatory > monday.MaxMandatory);
        Assert.Equal(0, monday.PortalChance);
        Assert.Throws<ArgumentOutOfRangeException>(() => DifficultyPreset.ForWeekday(7));
    }

    /// <summary>Runs the handler's candidate loop with a reproducible seed sequence.</summary>
    internal static (BuiltDailyPipe? Built, int Attempts) Build(DateOnly date, int seed)
    {
        var generator = new DailyLevelGenerator(SeedsFrom(seed));
        for (var attempt = 1; attempt <= GenerateDailyPipeCommandHandler.MaxAttempts; attempt++)
        {
            if (DailyPipeBuilder.TryBuild(generator.Generate(date)) is { } built)
            {
                return (built, attempt);
            }
        }

        return (null, GenerateDailyPipeCommandHandler.MaxAttempts);
    }

    private static Func<int> SeedsFrom(int seed)
    {
        var next = seed * 1_000;
        return () => next++;
    }

    private static Side Edge(Endpoint endpoint, GridSize grid) => endpoint.Position switch
    {
        { X: 0 } when endpoint.OpenSide == Side.Right => Side.Left,
        var p when p.X == grid.Columns - 1 && endpoint.OpenSide == Side.Left => Side.Right,
        { Y: 0 } => Side.Top,
        _ => Side.Bottom,
    };
}
