using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Tests.Factories;

namespace HexMaster.LeakOut.Players.Tests.DomainModels;

public sealed class PlayerTests
{
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);

    [Fact]
    public void Register_SetsIdNameAndTimestamps()
    {
        var player = Player.Register("Ann", Now);

        Assert.NotEqual(Guid.Empty, player.Id);
        Assert.Equal("Ann", player.DisplayName);
        Assert.Equal(Now, player.CreatedAt);
        Assert.Equal(Now, player.LastSignedInAt);
    }

    [Fact]
    public void Register_GivesEveryPlayerANewId()
    {
        Assert.NotEqual(Player.Register("Ann", Now).Id, Player.Register("Ann", Now).Id);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void Register_WithoutDisplayName_StoresEmptyName(string? displayName)
    {
        Assert.Equal(string.Empty, Player.Register(displayName, Now).DisplayName);
    }

    [Fact]
    public void Register_TrimsDisplayName()
    {
        Assert.Equal("Ann", Player.Register("  Ann ", Now).DisplayName);
    }

    [Fact]
    public void RecordSignIn_RefreshesDisplayNameAndLastSignIn()
    {
        var player = new PlayerFaker("Ann").Generate();
        var createdAt = player.CreatedAt;
        var later = player.LastSignedInAt.AddDays(1);

        player.RecordSignIn("Annie", later);

        Assert.Equal("Annie", player.DisplayName);
        Assert.Equal(later, player.LastSignedInAt);
        Assert.Equal(createdAt, player.CreatedAt);
    }

    [Fact]
    public void RecordSignIn_BeforeRegistration_ThrowsDomainException()
    {
        var player = Player.Register("Ann", Now);

        Assert.Throws<DomainException>(() => player.RecordSignIn("Ann", Now.AddSeconds(-1)));
    }

    [Fact]
    public void Restore_RebuildsStoredState()
    {
        var id = Guid.CreateVersion7();

        var player = Player.Restore(id, "Ann", Now.AddDays(-1), Now);

        Assert.Equal(id, player.Id);
        Assert.Equal("Ann", player.DisplayName);
        Assert.Equal(Now.AddDays(-1), player.CreatedAt);
        Assert.Equal(Now, player.LastSignedInAt);
    }

    [Fact]
    public void Restore_WithEmptyId_ThrowsDomainException()
    {
        Assert.Throws<DomainException>(() => Player.Restore(Guid.Empty, "Ann", Now, Now));
    }

    [Fact]
    public void Restore_WithSignInBeforeRegistration_ThrowsDomainException()
    {
        Assert.Throws<DomainException>(() => Player.Restore(Guid.CreateVersion7(), "Ann", Now, Now.AddSeconds(-1)));
    }
}
