using System.ComponentModel.DataAnnotations;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>
/// The web OAuth client of the Google Cloud project linked to the game in the Play Console.
/// The game must request its server auth code with this client id as <c>serverClientId</c>.
/// </summary>
public sealed class GooglePlayGamesOptions
{
    public const string SectionName = "GooglePlayGames";

    [Required]
    public string ClientId { get; set; } = string.Empty;

    [Required]
    public string ClientSecret { get; set; } = string.Empty;
}
