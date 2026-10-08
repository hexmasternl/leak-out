using System.Reflection;

namespace HexMaster.LeakOut.Api.Versioning;

/// <summary>
/// Adds the <see cref="HeaderName"/> header with the running build's version to every response, so a
/// caller can tell which build answered. The deploy workflow sets the informational version to
/// <c>&lt;semver&gt;+&lt;short commit sha&gt;</c>.
/// </summary>
public static class ApiVersionHeader
{
    public const string HeaderName = "X-API-Version";

    /// <summary>The informational version of the API assembly, without anything the SDK appends.</summary>
    public static string Version { get; } =
        typeof(ApiVersionHeader).Assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion
        ?? typeof(ApiVersionHeader).Assembly.GetName().Version?.ToString()
        ?? "unknown";

    public static IApplicationBuilder UseApiVersionHeader(this IApplicationBuilder app) =>
        app.Use((context, next) =>
        {
            // Set when the response starts rather than now: error handling clears the headers set so far.
            context.Response.OnStarting(() =>
            {
                context.Response.Headers[HeaderName] = Version;
                return Task.CompletedTask;
            });
            return next(context);
        });
}
