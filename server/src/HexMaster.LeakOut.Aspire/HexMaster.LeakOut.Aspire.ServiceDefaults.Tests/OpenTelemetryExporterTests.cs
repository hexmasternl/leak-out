using Azure.Monitor.OpenTelemetry.AspNetCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Options;

namespace HexMaster.LeakOut.Aspire.ServiceDefaults.Tests;

public sealed class OpenTelemetryExporterTests
{
    private const string ConnectionString =
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://localhost/";

    [Fact]
    public void ConfigureOpenTelemetry_WithApplicationInsightsConnectionString_RegistersAzureMonitorExporter()
    {
        var builder = Host.CreateEmptyApplicationBuilder(new HostApplicationBuilderSettings());
        builder.Configuration["APPLICATIONINSIGHTS_CONNECTION_STRING"] = ConnectionString;

        builder.ConfigureOpenTelemetry();

        Assert.True(HasAzureMonitorRegistration(builder.Services));
    }

    [Fact]
    public void ConfigureOpenTelemetry_WithoutApplicationInsightsConnectionString_DoesNotRegisterAzureMonitorExporter()
    {
        var builder = Host.CreateEmptyApplicationBuilder(new HostApplicationBuilderSettings());

        builder.ConfigureOpenTelemetry();

        Assert.False(HasAzureMonitorRegistration(builder.Services));
    }

    private static bool HasAzureMonitorRegistration(IServiceCollection services) =>
        services.Any(d => d.ServiceType == typeof(IConfigureOptions<AzureMonitorOptions>));
}
