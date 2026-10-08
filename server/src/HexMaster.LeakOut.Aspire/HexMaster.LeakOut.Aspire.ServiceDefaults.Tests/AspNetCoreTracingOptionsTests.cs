using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Options;
using OpenTelemetry.Instrumentation.AspNetCore;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults.Tests;

public sealed class AspNetCoreTracingOptionsTests
{
    private const string ConnectionString =
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://localhost/";

    [Theory]
    [InlineData(false, "/alive", false)]
    [InlineData(false, "/health", false)]
    [InlineData(false, "/api/scores", true)]
    [InlineData(true, "/alive", false)]
    [InlineData(true, "/health", false)]
    [InlineData(true, "/api/scores", true)]
    public void ConfigureOpenTelemetry_FiltersProbeRequests(bool withApplicationInsights, string path, bool traced)
    {
        var options = ResolveOptions(withApplicationInsights);

        var context = new DefaultHttpContext();
        context.Request.Path = path;

        Assert.NotNull(options.Filter);
        Assert.Equal(traced, options.Filter(context));
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public void ConfigureOpenTelemetry_RecordsExceptionsOnRequestSpans(bool withApplicationInsights)
    {
        var options = ResolveOptions(withApplicationInsights);

        Assert.True(options.RecordException);
    }

    private static AspNetCoreTraceInstrumentationOptions ResolveOptions(bool withApplicationInsights)
    {
        var builder = Host.CreateEmptyApplicationBuilder(new HostApplicationBuilderSettings());
        if (withApplicationInsights)
        {
            builder.Configuration["APPLICATIONINSIGHTS_CONNECTION_STRING"] = ConnectionString;
        }

        builder.ConfigureOpenTelemetry();

        using var services = builder.Services.BuildServiceProvider();
        return services.GetRequiredService<IOptionsMonitor<AspNetCoreTraceInstrumentationOptions>>()
            .Get(Options.DefaultName);
    }
}
