using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

/// <summary>The verdicts are covered by the conformance fixtures; these tests pin the details around them.</summary>
public sealed class LevelSolverTests
{
    [Fact]
    public void Solve_OpenGrid_ReturnsTheShortestRouteBetweenTheFacingCells()
    {
        var level = Load("unsolvable_sealed.json") with { Blocked = [] };

        var path = Assert.IsType<SolverResult.Solvable>(LevelSolver.Solve(level)).Path;

        Assert.Equal(new Position(1, 0), path[0]);
        Assert.Equal(new Position(4, 4), path[^1]);
        Assert.Equal(8, path.Count);
    }

    [Fact]
    public void Solve_ThroughPortals_HoldsBothPortalsInFlowOrder()
    {
        var level = Load("solvable_through_portals.json");

        var path = Assert.IsType<SolverResult.Solvable>(LevelSolver.Solve(level)).Path;

        var a = path.ToList().IndexOf(new Position(1, 2));
        Assert.True(a > 0);
        Assert.Equal(new Position(5, 1), path[a + 1]);
    }

    [Fact]
    public void Solve_Sealed_NamesTheFacingCells()
    {
        var result = Assert.IsType<SolverResult.Unsolvable>(LevelSolver.Solve(Load("unsolvable_sealed.json")));

        Assert.Equal(
            "The finish's facing cell (4, 4) cannot be reached from the start's facing cell (1, 0): blocked cells or the grid's edge seal it off",
            result.Reason);
    }

    [Fact]
    public void Solve_MandatoryAgainstTheEdge_NamesIt()
    {
        var result = Assert.IsType<SolverResult.Unsolvable>(LevelSolver.Solve(Load("unsolvable_mandatory_on_edge.json")));

        Assert.StartsWith("The mandatory RES at (0, 3) (mandatory[0]) can never be passed from left to right", result.Reason, StringComparison.Ordinal);
    }

    [Fact]
    public void Solve_UnreachableFacingCell_NamesTheEndpoint()
    {
        var level = Load("unsolvable_sealed.json") with { Blocked = [new Position(1, 0)] };

        var result = Assert.IsType<SolverResult.Unsolvable>(LevelSolver.Solve(level));

        Assert.StartsWith("The start's facing cell (1, 0) is blocked", result.Reason, StringComparison.Ordinal);
    }

    [Fact]
    public void Solve_MandatoryThatCannotBeReached_NamesTheWaypoint()
    {
        // A wall cuts the reservoir off from the start, though both of its neighbours are free.
        var level = Load("unsolvable_sealed.json") with
        {
            Mandatory = [new MandatorySection(new Position(4, 2), PipeSection.RES)],
            Blocked = [.. Enumerable.Range(0, 5).Select(y => new Position(2, y))],
        };

        var result = Assert.IsType<SolverResult.Unsolvable>(LevelSolver.Solve(level));

        Assert.StartsWith("No route from the start's facing cell (1, 0) runs through the mandatory RES at (4, 2)", result.Reason, StringComparison.Ordinal);
    }

    [Fact]
    public void Solve_FinishUnreachableAfterTheMandatory_SaysSo()
    {
        // The reservoir is reachable from the start, but the finish lies behind a wall.
        var level = Load("unsolvable_sealed.json") with
        {
            Mandatory = [new MandatorySection(new Position(1, 2), PipeSection.RES)],
        };

        var result = Assert.IsType<SolverResult.Unsolvable>(LevelSolver.Solve(level));

        Assert.Equal("The finish's facing cell (4, 4) cannot be reached after running through every mandatory section", result.Reason);
    }

    [Fact]
    public void Solve_RouteOverALeak_IsNotLeakFree()
    {
        var result = Assert.IsType<SolverResult.Solvable>(LevelSolver.Solve(Load("solvable_only_over_a_leak.json")));

        Assert.False(result.LeakFree);
    }

    [Fact]
    public void Level_CountsFreeAndScoringCells()
    {
        var level = Load("solvable_only_over_a_leak.json");

        // 24 cells, 6 blocked, start and finish: 16 free, one of them leaky.
        Assert.Equal(16, level.FreeCells);
        Assert.Equal(15, level.ScoringCells);
        Assert.True(level.IsLeaky(new Position(2, 0)));
    }

    [Theory]
    [InlineData(Side.Left, Side.Right)]
    [InlineData(Side.Right, Side.Left)]
    [InlineData(Side.Top, Side.Bottom)]
    [InlineData(Side.Bottom, Side.Top)]
    public void Side_Opposite(Side side, Side opposite) => Assert.Equal(opposite, side.Opposite());

    private static Level Load(string fileName) =>
        LevelJson.Parse(File.ReadAllText(Path.Combine(Fixtures.Directory, "levels", fileName)));
}
