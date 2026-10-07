using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Observability;
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
    IClock clock,
    PlayersMetrics metrics) : ICommandHandler<SignInPlayerCommand, SessionDto>
{
    public async Task<SessionDto> Handle(SignInPlayerCommand command, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(command);

        using var activity = PlayersMetrics.ActivitySource.StartActivity(PlayersMetrics.SignInActivityName);
        try
        {
            var googlePlayer = await googlePlayGamesClient.GetPlayerAsync(command.ServerAuthCode, cancellationToken);
            var now = clock.UtcNow;

            var player = await playerRepository.FindBySubjectIdAsync(googlePlayer.PlayerId, cancellationToken);
            var isNewPlayer = player is null;
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

            activity?.SetTag("player.id", player.Id);
            activity?.SetTag("player.new", isNewPlayer);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.SignInSucceeded(isNewPlayer);

            return new SessionDto(accessToken.Token, accessToken.ExpiresAt, player.Id, player.DisplayName);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.SignInFailed(ex switch
            {
                GooglePlayGamesAuthenticationException => PlayersMetrics.OutcomeRejected,
                GooglePlayGamesUnavailableException => PlayersMetrics.OutcomeUnavailable,
                _ => PlayersMetrics.OutcomeError,
            });
            throw;
        }
    }
}
