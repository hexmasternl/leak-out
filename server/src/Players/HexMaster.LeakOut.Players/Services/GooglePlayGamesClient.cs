using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>
/// Exchanges a Play Games server auth code at Google's OAuth token endpoint and reads the
/// player it belongs to from the Play Games Services API. Google's tokens are discarded afterwards.
/// </summary>
public sealed partial class GooglePlayGamesClient(
    HttpClient httpClient,
    IOptions<GooglePlayGamesOptions> options,
    ILogger<GooglePlayGamesClient> logger) : IGooglePlayGamesClient
{
    public static readonly Uri TokenEndpoint = new("https://oauth2.googleapis.com/token");
    public static readonly Uri CurrentPlayerEndpoint = new("https://games.googleapis.com/games/v1/players/me");

    public async Task<GooglePlayGamesPlayer> GetPlayerAsync(string serverAuthCode, CancellationToken cancellationToken)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(serverAuthCode);

        var googleAccessToken = await ExchangeServerAuthCodeAsync(serverAuthCode, cancellationToken);
        return await GetCurrentPlayerAsync(googleAccessToken, cancellationToken);
    }

    private async Task<string> ExchangeServerAuthCodeAsync(string serverAuthCode, CancellationToken cancellationToken)
    {
        var settings = options.Value;
        using var request = new HttpRequestMessage(HttpMethod.Post, TokenEndpoint)
        {
            Content = new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["grant_type"] = "authorization_code",
                ["code"] = serverAuthCode,
                ["client_id"] = settings.ClientId,
                ["client_secret"] = settings.ClientSecret,
                ["redirect_uri"] = string.Empty,
            }),
        };

        using var response = await SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            var error = await TryReadAsync<TokenErrorResponse>(response, cancellationToken);
            if (response.StatusCode == HttpStatusCode.BadRequest)
            {
                LogServerAuthCodeRejected(error?.Error);
                throw new GooglePlayGamesAuthenticationException("Google rejected the server auth code.");
            }

            // 401 (invalid_client) means our own client id or secret is wrong: not the player's fault.
            LogGoogleFailed("token exchange", (int)response.StatusCode, error?.Error);
            throw new GooglePlayGamesUnavailableException($"Google's token endpoint answered {(int)response.StatusCode}.");
        }

        var token = await ReadAsync<TokenResponse>(response, cancellationToken);
        if (string.IsNullOrEmpty(token.AccessToken))
        {
            throw new GooglePlayGamesUnavailableException("Google's token endpoint returned no access token.");
        }

        return token.AccessToken;
    }

    private async Task<GooglePlayGamesPlayer> GetCurrentPlayerAsync(string googleAccessToken, CancellationToken cancellationToken)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, CurrentPlayerEndpoint);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", googleAccessToken);

        using var response = await SendAsync(request, cancellationToken);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
        {
            LogServerAuthCodeRejected("unauthorized for players/me");
            throw new GooglePlayGamesAuthenticationException("Google did not accept the access token for the player.");
        }

        if (!response.IsSuccessStatusCode)
        {
            LogGoogleFailed("players/me", (int)response.StatusCode, null);
            throw new GooglePlayGamesUnavailableException($"The Play Games Services API answered {(int)response.StatusCode}.");
        }

        var player = await ReadAsync<PlayerResponse>(response, cancellationToken);
        if (string.IsNullOrEmpty(player.PlayerId))
        {
            throw new GooglePlayGamesUnavailableException("The Play Games Services API returned no player id.");
        }

        return new GooglePlayGamesPlayer(player.PlayerId, player.DisplayName ?? string.Empty);
    }

    private async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        try
        {
            return await httpClient.SendAsync(request, cancellationToken);
        }
        catch (Exception ex) when (ex is not OperationCanceledException || !cancellationToken.IsCancellationRequested)
        {
            // Network failures and resilience time-outs after retries.
            LogGoogleUnreachable(ex, request.RequestUri?.Host);
            throw new GooglePlayGamesUnavailableException("Google could not be reached.", ex);
        }
    }

    private static async Task<T> ReadAsync<T>(HttpResponseMessage response, CancellationToken cancellationToken)
        where T : class
    {
        return await TryReadAsync<T>(response, cancellationToken)
            ?? throw new GooglePlayGamesUnavailableException("Google returned a response that could not be read.");
    }

    private static async Task<T?> TryReadAsync<T>(HttpResponseMessage response, CancellationToken cancellationToken)
        where T : class
    {
        try
        {
            return await response.Content.ReadFromJsonAsync<T>(cancellationToken);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            return null;
        }
    }

    [LoggerMessage(Level = LogLevel.Information, Message = "Google rejected a server auth code ({GoogleError})")]
    private partial void LogServerAuthCodeRejected(string? googleError);

    [LoggerMessage(Level = LogLevel.Error, Message = "Google {Operation} failed with status {StatusCode} ({GoogleError})")]
    private partial void LogGoogleFailed(string operation, int statusCode, string? googleError);

    [LoggerMessage(Level = LogLevel.Warning, Message = "Google host {Host} could not be reached")]
    private partial void LogGoogleUnreachable(Exception exception, string? host);

    private sealed record TokenResponse([property: JsonPropertyName("access_token")] string? AccessToken);

    private sealed record TokenErrorResponse([property: JsonPropertyName("error")] string? Error);

    private sealed record PlayerResponse(
        [property: JsonPropertyName("playerId")] string? PlayerId,
        [property: JsonPropertyName("displayName")] string? DisplayName);
}
