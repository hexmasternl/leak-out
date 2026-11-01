using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.DomainModels;

namespace HexMaster.LeakOut.DailyPipes.Tests.DomainModels;

public sealed class DailyPipeTests
{
    private static readonly DateOnly Date = new(2026, 11, 1);
    private static readonly DateTimeOffset Now = new(2026, 11, 1, 0, 0, 5, TimeSpan.Zero);

    [Fact]
    public void Create_StoresThePipe()
    {
        var pipe = DailyPipe.Create(Date, """{"id":1}""", 42, 900, Now);

        Assert.Equal(Date, pipe.Date);
        Assert.Equal("""{"id":1}""", pipe.LevelJson);
        Assert.Equal(42, pipe.StackSeed);
        Assert.Equal(900, pipe.MaxScore);
        Assert.Equal(Now, pipe.PublishedAt);
    }

    [Theory]
    [InlineData("", 1, 100)]
    [InlineData("  ", 1, 100)]
    [InlineData("{}", -1, 100)]
    [InlineData("{}", 1, 0)]
    public void Create_Invalid_Throws(string levelJson, long seed, int maxScore) =>
        Assert.Throws<DomainException>(() => DailyPipe.Create(Date, levelJson, seed, maxScore, Now));
}
