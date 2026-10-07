namespace HexMaster.LeakOut.Players.Tests.Helpers;

/// <summary>An <see cref="HttpMessageHandler"/> that answers from a delegate and records what it was sent.</summary>
public sealed class StubHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> respond) : HttpMessageHandler
{
    public List<RecordedRequest> Requests { get; } = [];

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        var body = request.Content is null ? null : await request.Content.ReadAsStringAsync(cancellationToken);
        Requests.Add(new RecordedRequest(request.Method, request.RequestUri, request.Headers.Authorization?.ToString(), body));
        return respond(request);
    }

    public sealed record RecordedRequest(HttpMethod Method, Uri? Uri, string? Authorization, string? Body);
}
