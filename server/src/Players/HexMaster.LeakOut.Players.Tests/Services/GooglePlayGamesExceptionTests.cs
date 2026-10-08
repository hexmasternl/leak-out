using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Services;

namespace HexMaster.LeakOut.Players.Tests.Services;

public sealed class GooglePlayGamesExceptionTests
{
    private readonly InvalidOperationException _inner = new("inner");

    [Fact]
    public void AuthenticationException_KeepsMessageAndInnerException()
    {
        var exception = new GooglePlayGamesAuthenticationException("rejected", _inner);

        Assert.Equal("rejected", exception.Message);
        Assert.Same(_inner, exception.InnerException);
    }

    [Fact]
    public void UnavailableException_KeepsMessage()
    {
        Assert.Equal("down", new GooglePlayGamesUnavailableException("down").Message);
    }

    [Fact]
    public void DomainException_KeepsMessageAndInnerException()
    {
        var exception = new DomainException("invalid", _inner);

        Assert.Equal("invalid", exception.Message);
        Assert.Same(_inner, exception.InnerException);
    }

    [Fact]
    public void SystemClock_ReturnsCurrentUtcTime()
    {
        var before = DateTimeOffset.UtcNow;

        var now = new SystemClock().UtcNow;

        Assert.InRange(now, before, DateTimeOffset.UtcNow);
        Assert.Equal(TimeSpan.Zero, now.Offset);
    }
}
