using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Services;

namespace HexMaster.LeakOut.Players.Features.SignInPlayer;

/// <summary>
/// Verifies the server auth code with Google, registers the player on their first sign-in or
/// refreshes their display name on a later one, and issues an access token.
/// </summary>
public sealed class SignInPlayerCommandHandler(
    IGooglePlayGamesClient googlePlayGamesClient,
    IPlayerRepository playerRepository,
    IAccessTokenIssuer accessTokenIssuer,
    IClock clock) : ICommandHandler<SignInPlayerCommand, SessionDto>
{
    public async Task<SessionDto> Handle(SignInPlayerCommand command, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(command);

        var googlePlayer = await googlePlayGamesClient.GetPlayerAsync(command.ServerAuthCode, cancellationToken);
        var now = clock.UtcNow;

        var player = await playerRepository.FindBySubjectIdAsync(googlePlayer.PlayerId, cancellationToken);
        if (player is null)
        {
            player = Player.Register(googlePlayer.DisplayName, now);
            await playerRepository.AddAsync(googlePlayer.PlayerId, player, cancellationToken);
        }
        else
        {
            player.RecordSignIn(googlePlayer.DisplayName, now);
            await playerRepository.UpdateAsync(player, cancellationToken);
        }

        var accessToken = accessTokenIssuer.Issue(googlePlayer.PlayerId);
        return new SessionDto(accessToken.Token, accessToken.ExpiresAt, player.Id, player.DisplayName);
    }
}
