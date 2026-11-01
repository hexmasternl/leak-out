using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

public sealed class DailyPipeBuilderTests
{
    [Fact]
    public void TryBuild_SolvableCandidate_SetsHeadStartAndStarsFromTheRoute()
    {
        var level = Load("solvable_through_portals.json");

        var built = DailyPipeBuilder.TryBuild(new DailyPipeCandidate(level, 9, DelayPerRouteCellMs: 500));

        Assert.NotNull(built);
        var path = Assert.IsType<SolverResult.Solvable>(LevelSolver.Solve(level)).Path;
        Assert.Equal(level.Flow.InitialDelayMs + (500 * path.Count), built.Level.Flow.InitialDelayMs);
        Assert.Equal(DailyPipeBuilder.StarsFor(DailyPipeBuilder.RoutePoints(level, path)), built.Level.Stars);
        Assert.Equal(9, built.StackSeed);
        Assert.Equal(DailyPipeBuilder.MaxScore(built.Level), built.MaxScore);
        Assert.Equal(LevelJson.Write(built.Level), built.LevelJson);
    }

    [Theory]
    [InlineData("unsolvable_sealed.json")]
    [InlineData("invalid_stars.json")]
    public void TryBuild_InvalidOrUnsolvable_ReturnsNull(string fixture) =>
        Assert.Null(DailyPipeBuilder.TryBuild(new DailyPipeCandidate(Load(fixture), 1, 500)));

    [Fact]
    public void TryBuild_FinishedLevelInvalid_ReturnsNull()
    {
        // A negative head start per cell makes the finished level invalid.
        var candidate = new DailyPipeCandidate(Load("solvable_through_portals.json"), 1, DelayPerRouteCellMs: long.MinValue / 100);

        Assert.Null(DailyPipeBuilder.TryBuild(candidate));
    }

    [Fact]
    public void RoutePoints_ScoresStraightsAndCorners()
    {
        // Start (0,0) facing right, finish (5,0) facing left on an open row: 4 straights.
        var level = Load("solvable_only_over_a_leak.json") with { Leaky = [] };

        Assert.Equal(40, DailyPipeBuilder.RoutePoints(level, [new(1, 0), new(2, 0), new(3, 0), new(4, 0)]));
    }

    [Fact]
    public void RoutePoints_CornersReservoirCrossAndLeak()
    {
        var level = Load("unsolvable_sealed.json") with
        {
            Blocked = [],
            Mandatory = [new MandatorySection(new Position(2, 1), PipeSection.X), new MandatorySection(new Position(1, 2), PipeSection.RES)],
            Leaky = [new Position(4, 3)],
        };
        Position[] path =
        [
            new(1, 0), // from the start (0,0) down to (1,1): a corner, 15
            new(1, 1), // straight down: 10
            new(1, 2), // reservoir: 25
            new(1, 3), // corner: 15
            new(2, 3), // straight: 10
            new(3, 3), // straight: 10
            new(4, 3), // leaky: -20
            new(4, 4), // corner into the finish (5,4): 15
        ];

        Assert.Equal(15 + 10 + 25 + 15 + 10 + 10 - 20 + 15, DailyPipeBuilder.RoutePoints(level, path));
        Assert.Equal(60, DailyPipeBuilder.RoutePoints(level, [new(2, 1), new(2, 1)]));
    }

    [Theory]
    [InlineData(200, 120, 180, 240)]
    [InlineData(135, 80, 120, 160)]
    [InlineData(5, 10, 20, 30)]
    [InlineData(0, 10, 20, 30)]
    public void StarsFor_RoundsToTensAndStaysStrictlyIncreasing(int points, int one, int two, int three) =>
        Assert.Equal(new StarBenchmarks(one, two, three), DailyPipeBuilder.StarsFor(points));

    [Fact]
    public void MaxScore_IsACrossDoublePassOnEveryScoringCell()
    {
        var level = Load("solvable_only_over_a_leak.json");

        Assert.Equal(15 * 60, DailyPipeBuilder.MaxScore(level));
    }

    private static Level Load(string fileName) =>
        LevelJson.Parse(File.ReadAllText(Path.Combine(Fixtures.Directory, "levels", fileName)));
}
