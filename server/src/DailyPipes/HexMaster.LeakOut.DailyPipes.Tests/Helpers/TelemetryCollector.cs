using System.Collections.Concurrent;
using System.Diagnostics;
using System.Diagnostics.Metrics;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.DependencyInjection;

namespace HexMaster.LeakOut.DailyPipes.Tests.Helpers;

/// <summary>
/// Owns a <see cref="DailyPipesMetrics"/> instance and records the measurements and activities it and
/// the DailyPipes module produce.
/// </summary>
public sealed class TelemetryCollector : IDisposable
{
    private readonly ServiceProvider _services;
    private readonly MeterListener _meterListener = new();
    private readonly ActivityListener _activityListener;

    public TelemetryCollector()
    {
        _services = new ServiceCollection().AddMetrics().BuildServiceProvider();
        var meterFactory = _services.GetRequiredService<IMeterFactory>();
        Metrics = new DailyPipesMetrics(meterFactory);

        _meterListener.InstrumentPublished = (instrument, listener) =>
        {
            if (ReferenceEquals(instrument.Meter.Scope, meterFactory))
            {
                listener.EnableMeasurementEvents(instrument);
            }
        };
        _meterListener.SetMeasurementEventCallback<long>((instrument, value, tags, _) =>
            Measurements.Add(new Measurement(instrument.Name, value, tags.ToArray().ToDictionary(t => t.Key, t => t.Value))));
        _meterListener.SetMeasurementEventCallback<int>((instrument, value, tags, _) =>
            Measurements.Add(new Measurement(instrument.Name, value, tags.ToArray().ToDictionary(t => t.Key, t => t.Value))));
        _meterListener.SetMeasurementEventCallback<double>((instrument, value, tags, _) =>
            Measurements.Add(new Measurement(instrument.Name, value, tags.ToArray().ToDictionary(t => t.Key, t => t.Value))));
        _meterListener.Start();

        _activityListener = new ActivityListener
        {
            ShouldListenTo = source => source.Name == DailyPipesMetrics.Name,
            Sample = (ref _) => ActivitySamplingResult.AllDataAndRecorded,
            ActivityStopped = Activities.Add,
        };
        ActivitySource.AddActivityListener(_activityListener);
    }

    public DailyPipesMetrics Metrics { get; }

    public ConcurrentBag<Measurement> Measurements { get; } = [];

    public ConcurrentBag<Activity> Activities { get; } = [];

    /// <summary>The outcome tag of every measurement of <paramref name="instrument"/>.</summary>
    public IReadOnlyList<object?> Outcomes(string instrument) =>
        Measurements.Where(m => m.Instrument == instrument).Select(m => m.Tags["outcome"]).ToList();

    public void Dispose()
    {
        _activityListener.Dispose();
        _meterListener.Dispose();
        _services.Dispose();
    }

    public sealed record Measurement(string Instrument, double Value, IReadOnlyDictionary<string, object?> Tags);
}

/// <summary>
/// The activity listener of <see cref="TelemetryCollector"/> sees every activity of the module in the
/// process, so test classes that run instrumented code join this collection and never run in parallel.
/// </summary>
[CollectionDefinition(Name, DisableParallelization = true)]
public sealed class TelemetryCollection
{
    public const string Name = "Telemetry";
}
