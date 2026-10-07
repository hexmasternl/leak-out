using System.Security.Claims;
using HexMaster.LeakOut.Players.Abstractions.Services;
using Microsoft.IdentityModel.JsonWebTokens;

namespace HexMaster.LeakOut.Api.Authentication;

/// <summary>
/// Resolves the access token's subject id to the internal player id at the API boundary, so
/// commands and queries only ever see the internal id.
/// </summary>
public static class PlayerIdentityExtensions
{
    private const string PlayerIdItemKey = "HexMaster.LeakOut.PlayerId";

    /// <summary>
    /// Resolves the calling player before the endpoint runs and answers 401 when the token belongs to
    /// no registered player. Read the id in the endpoint with <see cref="GetPlayerId"/>.
    /// </summary>
    public static TBuilder RequirePlayer<TBuilder>(this TBuilder builder)
        where TBuilder : IEndpointConventionBuilder
    {
        return builder.AddEndpointFilter(async (context, next) =>
        {
            var httpContext = context.HttpContext;
            var playerId = await httpContext.ResolveUserIdAsync(httpContext.RequestAborted);
            if (playerId is null)
            {
                return TypedResults.Unauthorized();
            }

            httpContext.Items[PlayerIdItemKey] = playerId.Value;
            return await next(context);
        });
    }

    /// <summary>Reads the <c>sub</c> claim and resolves it to the internal player id.</summary>
    /// <returns>The internal player id, or <see langword="null"/> when there is no subject or no such player.</returns>
    public static async Task<Guid?> ResolveUserIdAsync(this HttpContext httpContext, CancellationToken cancellationToken)
    {
        var subjectId = httpContext.User.FindFirstValue(JwtRegisteredClaimNames.Sub);
        if (string.IsNullOrWhiteSpace(subjectId))
        {
            return null;
        }

        var userResolver = httpContext.RequestServices.GetRequiredService<IUserResolver>();
        return await userResolver.ResolveAsync(subjectId, cancellationToken);
    }

    /// <summary>The internal player id resolved by <see cref="RequirePlayer{TBuilder}"/>.</summary>
    public static Guid GetPlayerId(this HttpContext httpContext) =>
        httpContext.Items[PlayerIdItemKey] is Guid playerId
            ? playerId
            : throw new InvalidOperationException("No player was resolved; add RequirePlayer() to the endpoint.");
}
