namespace HexMaster.LeakOut.Api.Cors;

/// <summary>
/// The CORS policy of the endpoints the public website calls from the browser. The allowed origins
/// come from <c>Website:AllowedOrigins</c>; only GET requests are allowed and no credentials are sent.
/// </summary>
public static class WebsiteCorsRegistration
{
    public const string PolicyName = "Website";
    public const string AllowedOriginsKey = "Website:AllowedOrigins";

    public static IServiceCollection AddWebsiteCors(this IServiceCollection services, IConfiguration configuration)
    {
        var origins = configuration.GetSection(AllowedOriginsKey).Get<string[]>() ?? [];
        services.AddCors(options => options.AddPolicy(PolicyName, policy => policy
            .WithOrigins(origins)
            .WithMethods(HttpMethods.Get)
            .WithHeaders("Accept")));
        return services;
    }
}
